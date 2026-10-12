// SPDX-License-Identifier: MIT
package com.apkcategorychecker.framework

import com.apkcategorychecker.apk.ApkContents
import com.apkcategorychecker.apk.ApkFormatException
import com.apkcategorychecker.apk.BinaryXml

/** Caches the parts of an APK that several detectors look at. */
class ApkInspection(
    val apk: ApkContents,
) {
    val entries: Set<String> by lazy { apk.entryNames.toHashSet() }

    /** Where Cordova's `config.xml` is: looked up in the resource table, as release builds may rename it. */
    val configXmlPath: String by lazy { apk.resourceFile("xml", "config") ?: CONFIG_XML }

    /** Cordova's `config.xml` as text, or "" if absent. */
    val configXml: String by lazy {
        val bytes = apk.read(configXmlPath) ?: return@lazy ""
        try {
            BinaryXml.parse(bytes).toXml()
        } catch (e: ApkFormatException) {
            bytes.toString(Charsets.UTF_8) // not compiled: some build tools ship it as plain XML
        }
    }

    val manifestXml: String by lazy { apk.manifest.root.toXml() }

    /** The value of a `<meta-data>` of the manifest, with resource references (`@0x7f...`) resolved. */
    fun manifestMetaData(name: String): String? {
        val value =
            apk.manifest.root
                .findAll("meta-data")
                .firstOrNull { it.attributes["android:name"] == name }
                ?.attributes
                ?.get("android:value") ?: return null
        val reference =
            value
                .removePrefix("@0x")
                .takeIf { it != value }
                ?.toLongOrNull(16)
                ?.toInt() ?: return value
        return apk.resourceValue(reference) ?: value
    }

    /** Sorted, so that the evidence reported for a package is stable. */
    private val sortedClassNames: List<String> by lazy { apk.classNames.sorted() }

    fun firstClassInPackage(packageName: String): String? = sortedClassNames.firstOrNull { it.startsWith("$packageName.") }

    fun firstClassNamed(simpleName: String): String? = sortedClassNames.firstOrNull { it.substringAfterLast('.') == simpleName }

    /** Text content of the web resources and other text files under `assets/`. */
    private val assetTexts: Map<String, String> by lazy {
        apk.entryNames
            .filter { it.startsWith("assets/") && it.substringAfterLast('.').lowercase() in TEXT_EXTENSIONS }
            .sorted()
            .associateWith { apk.readText(it).orEmpty() }
    }

    /** The first text asset under one of [dirs] (and with [extension], if given) that contains [text]. */
    fun findAssetContaining(
        text: String,
        dirs: List<String>,
        extension: String? = null,
    ): String? =
        assetTexts.entries
            .firstOrNull { (name, content) ->
                dirs.any(name::startsWith) &&
                    (extension == null || name.endsWith(extension, ignoreCase = true)) &&
                    text in content
            }?.key

    /** The first native library (e.g. `lib/arm64-v8a/libflutter.so`) whose file name matches, in any ABI. */
    fun firstNativeLib(nameMatches: (String) -> Boolean): String? =
        apk.entryNames.filter { it.startsWith("lib/") && nameMatches(it.substringAfterLast('/')) }.minOrNull()

    fun countFiles(extension: String): Int = apk.entryNames.count { it.endsWith(extension, ignoreCase = true) }

    private companion object {
        const val CONFIG_XML = "res/xml/config.xml"
        val TEXT_EXTENSIONS = setOf("js", "mjs", "html", "htm", "css", "json", "xml", "txt")
    }
}
