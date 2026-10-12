// SPDX-License-Identifier: MIT
package com.apkcategorychecker.sdk

import com.apkcategorychecker.framework.ApkInspection
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.util.regex.PatternSyntaxException

/** An SDK or library found in an app, with what was found (at most [SdkRules.MAX_EVIDENCE] items). */
@Serializable
data class SdkFinding(
    val name: String,
    val team: String? = null,
    val evidence: List<String>,
)

/**
 * Recognizes SDKs and libraries from native libraries, manifest components, intent actions and
 * DEX packages, with the rules of LibChecker-Rules (https://github.com/LibChecker/LibChecker-Rules,
 * Apache-2.0), regenerated with `./gradlew updateSdkRules`.
 *
 * As in LibChecker, exact names are looked up first, then regular expressions in priority order;
 * a regular expression must match the whole name.
 */
class SdkRules private constructor(
    private val libraries: Map<String, Library>,
    rules: List<Rule>,
) {
    @Serializable
    private data class Data(
        val libraries: Map<String, Library>,
        val rules: List<Rule>,
    )

    @Serializable
    private data class Library(
        val label: String,
        val team: String? = null,
        val link: String? = null,
    )

    @Serializable
    private data class Rule(
        val type: String,
        val name: String,
        val regex: Boolean,
        val priority: Int,
        val id: Int,
        val library: String,
    )

    private val exact: Map<Pair<String, String>, Rule> = rules.associateBy { it.type to it.name }

    private val patterns: Map<String, List<Pair<Regex, Rule>>> =
        rules
            .filter { it.regex }
            .sortedWith(compareBy({ it.priority }, { it.id }))
            .mapNotNull { rule ->
                // Patterns written for Android's regex engine that the JVM rejects are skipped.
                try {
                    Regex(rule.name) to rule
                } catch (e: PatternSyntaxException) {
                    null
                }
            }.groupBy({ it.second.type })

    /** Number of rules whose pattern could not be compiled. */
    val skippedPatterns: Int = rules.count { it.regex } - patterns.values.sumOf { it.size }

    private fun match(
        type: String,
        name: String,
    ): Rule? = exact[type to name] ?: patterns[type]?.firstOrNull { (regex, _) -> regex.matches(name) }?.second

    fun detect(apk: ApkInspection): List<SdkFinding> {
        val found = linkedMapOf<String, MutableList<String>>() // library -> evidence

        fun check(
            type: String,
            name: String,
            evidence: String,
        ) {
            match(type, name)?.let { found.getOrPut(it.library) { mutableListOf() } += evidence }
        }

        apk.apk.entryNames
            .filter { it.startsWith("lib/") && it.endsWith(".so") }
            .sorted()
            .distinctBy { it.substringAfterLast('/') }
            .forEach { check(NATIVE_LIB, it.substringAfterLast('/'), it) }

        val manifest = apk.apk.manifest
        val packageName = manifest.packageName.orEmpty()
        for (component in COMPONENTS) {
            manifest.root.findAll(component).mapNotNull { it.attributes["android:name"] }.distinct().forEach { name ->
                val className = qualify(name, packageName)
                check(component, className, "$component $className")
            }
        }
        manifest.root
            .findAll("action")
            .mapNotNull { it.attributes["android:name"] }
            .distinct()
            .forEach { check(ACTION, it, "action $it") }

        if (exact.keys.any { it.first == DEX } || DEX in patterns) {
            val classes = apk.apk.classNames
            val packages = classes.flatMapTo(HashSet()) { packagesOf(it) }
            exact.keys.filter { it.first == DEX && it.second in packages }.forEach { (_, name) -> check(DEX, name, "package $name") }
            patterns[DEX]?.let { dexPatterns ->
                classes.sorted().forEach { className ->
                    val rule = dexPatterns.firstOrNull { (regex, _) -> regex.matches(className) }?.second ?: return@forEach
                    found.getOrPut(rule.library) { mutableListOf() } += "class $className"
                }
            }
        }

        // Several libraries may share a label (e.g. an SDK and its plugins): report them once.
        return found.entries
            .groupBy { libraries[it.key]?.label ?: it.key }
            .map { (label, entries) ->
                SdkFinding(
                    name = label,
                    team = entries.firstNotNullOfOrNull { libraries[it.key]?.team },
                    evidence =
                        entries
                            .flatMap { it.value }
                            .distinct()
                            .sorted()
                            .take(MAX_EVIDENCE),
                )
            }.sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { it.name })
    }

    companion object {
        const val MAX_EVIDENCE = 5
        private const val NATIVE_LIB = "nativeLib"
        private const val ACTION = "action"
        private const val DEX = "dex"
        private val COMPONENTS = listOf("activity", "service", "receiver", "provider")

        private val JSON = Json { ignoreUnknownKeys = true }

        val builtIn: SdkRules by lazy { parse(SdkRules::class.java.getResource("/sdks/libchecker-rules.json")!!.readText()) }

        fun parse(json: String): SdkRules {
            val data = JSON.decodeFromString(Data.serializer(), json)
            return SdkRules(data.libraries, data.rules)
        }

        /** Manifest class names may be relative to the package: `.Main` or (old style) `Main`. */
        private fun qualify(
            name: String,
            packageName: String,
        ): String =
            when {
                name.startsWith(".") -> packageName + name
                '.' !in name && packageName.isNotEmpty() -> "$packageName.$name"
                else -> name
            }

        /** `a.b.C` -> `a`, `a.b` */
        private fun packagesOf(className: String): List<String> {
            val parts = className.split('.').dropLast(1)
            return parts.indices.map { parts.subList(0, it + 1).joinToString(".") }
        }
    }
}
