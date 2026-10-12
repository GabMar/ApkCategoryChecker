// SPDX-License-Identifier: MIT
package com.apkcategorychecker.framework

import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import java.nio.file.Path
import java.util.regex.PatternSyntaxException
import kotlin.io.path.readText

class RulesException(
    message: String,
) : IllegalArgumentException(message)

/**
 * The framework rules: which frameworks exist, how they are recognized, what they run on and
 * which details they report. Read from JSON: see `rules/frameworks.json` for the format.
 */
class Rules(
    /** From the most generic to the most specific. */
    val frameworks: List<Framework>,
    /** Details of apps without a cross-platform framework. */
    val nativeDetails: List<Detail>,
) {
    companion object {
        private const val BUILT_IN = "/rules/frameworks.json"

        /** The rules shipped with the tool, as JSON text. */
        val builtInText: String by lazy { Rules::class.java.getResource(BUILT_IN)!!.readText() }

        val builtIn: Rules by lazy { parse(builtInText, "built-in rules") }

        fun load(path: Path): Rules = parse(path.readText(), path.toString())

        fun parse(
            json: String,
            source: String,
        ): Rules {
            val root =
                try {
                    Json.parseToJsonElement(json)
                } catch (e: SerializationException) {
                    throw RulesException("$source: invalid JSON: ${e.message}")
                }
            return RulesParser(source).parse(root)
        }
    }
}

/** Turns the JSON rules into frameworks, with errors that point to the faulty element. */
private class RulesParser(
    private val source: String,
) {
    fun parse(root: JsonElement): Rules {
        val obj = root.obj("")
        obj.only("", "schemaVersion", "frameworks", "nativeDetails", "comment")
        val version = obj["schemaVersion"]?.let { (it as? JsonPrimitive)?.content }
        if (version != "1") fail("schemaVersion", "unsupported schema version $version, expected 1")

        val frameworks = obj["frameworks"].array("frameworks").mapIndexed { i, it -> framework(it, "frameworks[$i]") }
        if (frameworks.isEmpty()) fail("frameworks", "no frameworks")
        val names = frameworks.map { it.name }
        names
            .groupBy { it }
            .filterValues { it.size > 1 }
            .keys
            .firstOrNull()
            ?.let { fail("frameworks", "duplicate framework \"$it\"") }
        frameworks.forEachIndexed { i, framework ->
            framework.runsOn.filterNot { it in names }.forEach { fail("frameworks[$i].runsOn", "unknown framework \"$it\"") }
        }
        val nativeDetails = obj["nativeDetails"]?.array("nativeDetails")?.mapIndexed { i, it -> detail(it, "nativeDetails[$i]") }
        return Rules(frameworks, nativeDetails.orEmpty())
    }

    private fun framework(
        element: JsonElement,
        path: String,
    ): Framework {
        val obj = element.obj(path)
        obj.only(path, "name", "match", "signals", "runsOn", "details", "comment")
        val name = obj.string("name", path)
        val requireAll =
            when (val match = obj["match"]?.let { it.string("$path.match") } ?: "any") {
                "any" -> false
                "all" -> true
                else -> fail("$path.match", "expected \"any\" or \"all\", found \"$match\"")
            }
        val signals = obj["signals"].array("$path.signals").mapIndexed { i, it -> signal(it, "$path.signals[$i]") }
        if (signals.isEmpty()) fail("$path.signals", "no signals")
        val runsOn = obj["runsOn"]?.array("$path.runsOn")?.mapIndexed { i, it -> it.string("$path.runsOn[$i]") }.orEmpty()
        val details = obj["details"]?.array("$path.details")?.mapIndexed { i, it -> detail(it, "$path.details[$i]") }.orEmpty()
        return Framework(name, signals, requireAll, runsOn, details)
    }

    private fun signal(
        element: JsonElement,
        path: String,
    ): Signal {
        val obj = element.obj(path)
        val kinds = obj.keys - setOf("comment", "extension")
        val kind =
            kinds.singleOrNull() ?: fail(path, "expected exactly one of ${SIGNALS.joinToString()}, found ${kinds.ifEmpty { "none" }}")
        if (kind !in SIGNALS) fail(path, "unknown signal \"$kind\", expected one of ${SIGNALS.joinToString()}")
        if ("extension" in obj && kind != "webAssetContains") fail(path, "\"extension\" only applies to webAssetContains")
        if (kind == "anyOf") {
            val signals = obj["anyOf"].array("$path.anyOf").mapIndexed { i, it -> signal(it, "$path.anyOf[$i]") }
            if (signals.isEmpty()) fail("$path.anyOf", "no signals")
            return either(*signals.toTypedArray())
        }
        val value = obj.string(kind, path)
        if (value.isEmpty()) fail("$path.$kind", "empty value")
        return when (kind) {
            "nativeLib" -> nativeLib(value)
            "nativeLibRegex" -> nativeLib(regex(value, "$path.$kind"))
            "classPackage" -> classInPackage(value)
            "className" -> classNamed(value)
            "file" -> file(value)
            "fileUnder" -> fileUnder(value)
            "manifestContains" -> manifestContains(value)
            "configXmlContains" -> configXmlContains(value)
            "webAssetContains" -> webAssetContains(value, obj["extension"]?.string("$path.extension"))
            else -> hermesBytecode(value)
        }
    }

    private fun detail(
        element: JsonElement,
        path: String,
    ): Detail {
        val obj = element.obj(path)
        obj.only(path, "key", "options", "manifestMetaData", "extract", "comment")
        val key = obj.string("key", path)
        val kinds = obj.keys.intersect(setOf("options", "manifestMetaData", "extract"))
        if (kinds.size != 1) fail(path, "expected exactly one of options, manifestMetaData, extract")
        obj["manifestMetaData"]?.let { name ->
            val metaData = name.string("$path.manifestMetaData")
            return Detail(key) { it.manifestMetaData(metaData) }
        }
        obj["extract"]?.let { return Detail(key, extraction(it, "$path.extract")) }
        val options =
            obj["options"].array("$path.options").mapIndexed { i, option ->
                val optionPath = "$path.options[$i]"
                val o = option.obj(optionPath)
                o.only(optionPath, "value", "signal", "comment")
                o.string("value", optionPath) to signal(o["signal"] ?: fail(optionPath, "missing \"signal\""), "$optionPath.signal")
            }
        if (options.isEmpty()) fail("$path.options", "no options")
        return detail(key, *options.toTypedArray())
    }

    /**
     * A value read from a file of the APK: the first group of `pattern` in its content, or, without
     * `pattern`, the first group of `nativeLibRegex` in the library's file name.
     */
    private fun extraction(
        element: JsonElement,
        path: String,
    ): (ApkInspection) -> String? {
        val obj = element.obj(path)
        obj.only(path, "nativeLib", "nativeLibRegex", "file", "pattern", "comment")
        val sources = obj.keys.intersect(setOf("nativeLib", "nativeLibRegex", "file"))
        val kind = sources.singleOrNull() ?: fail(path, "expected exactly one of nativeLib, nativeLibRegex, file")
        val value = obj.string(kind, path)
        val pattern = obj["pattern"]?.let { regexWithGroup(it.string("$path.pattern"), "$path.pattern") }
        val nameRegex = if (kind == "nativeLibRegex") regex(value, "$path.nativeLibRegex") else null
        if (pattern == null) {
            if (nameRegex == null) fail(path, "missing \"pattern\"")
            regexWithGroup(value, "$path.nativeLibRegex")
        }
        return { apk ->
            val entry =
                when (kind) {
                    "file" -> value.takeIf { it in apk.entries }
                    else -> apk.firstNativeLib { name -> nameRegex?.matches(name) ?: (name == value) }
                }
            when {
                entry == null -> null

                pattern == null -> nameRegex!!.matchEntire(entry.substringAfterLast('/'))?.groupValues?.get(1)

                // Binary files: ISO-8859-1 maps every byte to a char, so ASCII text is found as is.
                else -> apk.apk.read(entry)?.let { pattern.find(String(it, Charsets.ISO_8859_1))?.groupValues?.get(1) }
            }
        }
    }

    private fun regexWithGroup(
        pattern: String,
        path: String,
    ): Regex {
        val regex = regex(pattern, path)
        if (regex.toPattern().matcher("").groupCount() < 1) fail(path, "the regular expression needs a group (...) for the value")
        return regex
    }

    private fun regex(
        pattern: String,
        path: String,
    ): Regex =
        try {
            Regex(pattern)
        } catch (e: PatternSyntaxException) {
            fail(path, "invalid regular expression: ${e.description}")
        }

    private fun JsonElement?.obj(path: String): JsonObject = this as? JsonObject ?: fail(path, "expected an object")

    private fun JsonElement?.array(path: String): JsonArray = this as? JsonArray ?: fail(path, "expected an array")

    private fun JsonElement?.string(path: String): String =
        (this as? JsonPrimitive)?.takeIf { it.isString }?.content ?: fail(path, "expected a string")

    private fun JsonObject.string(
        key: String,
        path: String,
    ): String = (this[key] ?: fail(path, "missing \"$key\"")).string(if (path.isEmpty()) key else "$path.$key")

    private fun JsonObject.only(
        path: String,
        vararg allowed: String,
    ) {
        (keys - allowed.toSet()).firstOrNull()?.let {
            fail(path, "unknown field \"$it\", expected one of ${allowed.joinToString()}")
        }
    }

    private fun fail(
        path: String,
        message: String,
    ): Nothing = throw RulesException(if (path.isEmpty()) "$source: $message" else "$source: $path: $message")

    private companion object {
        val SIGNALS =
            listOf(
                "nativeLib",
                "nativeLibRegex",
                "classPackage",
                "className",
                "file",
                "fileUnder",
                "manifestContains",
                "configXmlContains",
                "webAssetContains",
                "hermesBytecode",
                "anyOf",
            )
    }
}
