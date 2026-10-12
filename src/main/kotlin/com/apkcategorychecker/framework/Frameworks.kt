// SPDX-License-Identifier: GPL-3.0-or-later
package com.apkcategorychecker.framework

import com.apkcategorychecker.apk.ApkFile
import com.apkcategorychecker.apk.ApkFormatException
import com.apkcategorychecker.apk.BinaryXml

class Framework(val name: String, private val test: (ApkInspection) -> Boolean) {
    fun matches(apk: ApkInspection): Boolean = test(apk)
    override fun toString() = name
}

/** Caches the parts of an APK that several detectors look at. */
class ApkInspection(val apk: ApkFile) {

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

    /** Text content of the web resources and other text files under `assets/`. */
    private val assetTexts: Map<String, String> by lazy {
        apk.entryNames
            .filter { it.startsWith("assets/") && it.substringAfterLast('.').lowercase() in TEXT_EXTENSIONS }
            .associateWith { apk.readText(it).orEmpty() }
    }

    fun anyAssetContains(text: String, under: String = "assets/", extension: String? = null): Boolean =
        assetTexts.any { (name, content) ->
            name.startsWith(under) && (extension == null || name.endsWith(extension, ignoreCase = true)) && text in content
        }

    fun countFiles(extension: String): Int = apk.entryNames.count { it.endsWith(extension, ignoreCase = true) }

    private companion object {
        const val CONFIG_XML = "res/xml/config.xml"
        val TEXT_EXTENSIONS = setOf("js", "html", "htm", "css", "json", "xml", "txt")
    }
}

/**
 * The known frameworks, from the most generic to the most specific: when several match,
 * the analysis reports the last one (e.g. Sencha wins over the Cordova it runs on).
 */
object Frameworks {

    val CORDOVA = Framework("Apache Cordova") {
        "org.apache.cordova" in it.configXml && it.apk.hasClassNamed("CordovaActivity")
    }

    /** Any app with at least [threshold] web resource files; disabled when [threshold] is 0. */
    fun undefinedWebApp(threshold: Int) = Framework("Undefined_Web_App") {
        threshold > 0 && it.countFiles(".html") + it.countFiles(".js") + it.countFiles(".css") >= threshold
    }

    fun all(webAppThreshold: Int = 0): List<Framework> = listOf(
        undefinedWebApp(webAppThreshold),
        CORDOVA,
        Framework("PhoneGap") {
            "http://phonegap.com/ns/1.0" in it.configXml && it.apk.hasClassNamed("CordovaActivity")
        },
        Framework("Titanium") {
            it.apk.hasClassNamed("TitaniumModule") && it.apk.hasClassNamed("TiActivity")
        },
        Framework("Enyo") {
            it.anyAssetContains("enyo.machine", under = WWW, extension = ".js") &&
                it.anyAssetContains("enyo.kind", under = WWW, extension = ".js")
        },
        Framework("IBM Worklight") {
            "com.worklight.androidgap" in it.configXml && it.apk.hasClassNamed("WLDroidGap")
        },
        Framework("IUI") { it.apk.hasClassNamed("IUI") },
        Framework("Mobl") {
            it.apk.hasClassNamed("MoblGap") && it.apk.entryNames.any { name -> name.endsWith(".mobl") }
        },
        Framework("Next") {
            it.anyAssetContains("nextwebapp", under = WWW) && it.apk.hasClassNamed("NextWebApp")
        },
        Framework("Sencha") {
            it.anyAssetContains("Ext.create", under = WWW, extension = ".js") &&
                it.anyAssetContains("Ext.application", under = WWW, extension = ".js")
        },
        Framework("Kivy") { "PythonActivity" in it.manifestXml },
        Framework("MoSync") { "MoSyncService" in it.manifestXml },
        Framework("QuickConnect") {
            it.anyAssetContains("qc.handleError") && it.anyAssetContains("function QCNativeFooter")
        },
        Framework("RhoMobile") { it.apk.entryNames.any { name -> name.substringAfterLast('/') == "rho.dat" } },
    )

    private const val WWW = "assets/www/"
}
