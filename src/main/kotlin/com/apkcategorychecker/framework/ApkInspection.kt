// SPDX-License-Identifier: GPL-3.0-or-later
package com.apkcategorychecker.framework

import com.apkcategorychecker.apk.ApkFile
import com.apkcategorychecker.apk.ApkFormatException
import com.apkcategorychecker.apk.BinaryXml

/** Caches the parts of an APK that several detectors look at. */
class ApkInspection(val apk: ApkFile) {

    val entries: Set<String> by lazy { apk.entryNames.toHashSet() }

    /** Cordova's `res/xml/config.xml` as text, or "" if absent. */
    val configXml: String by lazy {
        val bytes = apk.read(CONFIG_XML) ?: return@lazy ""
        try {
            BinaryXml.parse(bytes).toXml()
        } catch (e: ApkFormatException) {
            bytes.toString(Charsets.UTF_8) // not compiled: some build tools ship it as plain XML
        }
    }

    val manifestXml: String by lazy { apk.manifest.root.toXml() }

    /** Sorted, so that the evidence reported for a package is stable. */
    private val sortedClassNames: List<String> by lazy { apk.classNames.sorted() }

    fun firstClassInPackage(packageName: String): String? =
        sortedClassNames.firstOrNull { it.startsWith("$packageName.") }

    fun firstClassNamed(simpleName: String): String? =
        sortedClassNames.firstOrNull { it.substringAfterLast('.') == simpleName }

    /** Text content of the web resources and other text files under `assets/`. */
    private val assetTexts: Map<String, String> by lazy {
        apk.entryNames
            .filter { it.startsWith("assets/") && it.substringAfterLast('.').lowercase() in TEXT_EXTENSIONS }
            .sorted()
            .associateWith { apk.readText(it).orEmpty() }
    }

    /** The first text asset under one of [dirs] (and with [extension], if given) that contains [text]. */
    fun findAssetContaining(text: String, dirs: List<String>, extension: String? = null): String? =
        assetTexts.entries.firstOrNull { (name, content) ->
            dirs.any(name::startsWith) &&
                (extension == null || name.endsWith(extension, ignoreCase = true)) &&
                text in content
        }?.key

    fun countFiles(extension: String): Int = apk.entryNames.count { it.endsWith(extension, ignoreCase = true) }

    private companion object {
        const val CONFIG_XML = "res/xml/config.xml"
        val TEXT_EXTENSIONS = setOf("js", "mjs", "html", "htm", "css", "json", "xml", "txt")
    }
}
