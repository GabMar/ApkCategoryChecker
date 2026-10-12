package com.apkcategorychecker.analyzer

import com.apkcategorychecker.apk.syntheticApk
import com.apkcategorychecker.framework.Frameworks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.writeText

class ApkAnalyzerTest {

    @TempDir
    lateinit var dir: Path

    private fun analyze(
        files: Map<String, String> = emptyMap(),
        classes: List<String> = emptyList(),
        activities: List<String> = emptyList(),
        webAppThreshold: Int = 0,
    ): AnalyzerResult {
        val apk = syntheticApk(dir.resolve("test.apk"), files, classes, activities = activities)
        return ApkAnalyzer(Frameworks.all(webAppThreshold)).analyze(apk).result
    }

    private val cordovaConfig = mapOf("res/xml/config.xml" to """<widget><feature name="App"><param value="org.apache.cordova.App"/></feature></widget>""")
    private val cordovaClasses = listOf("org.apache.cordova.CordovaActivity")

    @Test
    fun `an app without known frameworks is native`() {
        val result = analyze()
        assertEquals("Native", result._APKMainFramework)
        assertEquals("", result._APKBaseFramework)
        assertEquals("com.example.app", result._Package)
        assertEquals(0, result._decodeSuccess)
    }

    @Test
    fun `cordova requires both config xml and CordovaActivity`() {
        assertEquals("Apache Cordova", analyze(cordovaConfig, cordovaClasses)._APKMainFramework)
        assertEquals("Native", analyze(cordovaConfig)._APKMainFramework)
        assertEquals("Native", analyze(classes = cordovaClasses)._APKMainFramework)
    }

    @Test
    fun `sencha requires both Ext create and Ext application`() {
        val create = "assets/www/a.js" to "Ext.create('Ext.Panel')"
        val application = "assets/www/b.js" to "Ext.application({})"
        assertEquals("Native", analyze(mapOf(create))._APKMainFramework)
        assertEquals("Native", analyze(mapOf(application))._APKMainFramework)
        assertEquals("Sencha", analyze(mapOf(create, application))._APKMainFramework)
    }

    @Test
    fun `base framework is cordova only when cordova is actually detected`() {
        val sencha = mapOf("assets/www/app.js" to "Ext.create(); Ext.application();")

        val standalone = analyze(sencha)
        assertEquals("Sencha", standalone._APKMainFramework)
        assertEquals("", standalone._APKBaseFramework)

        val onCordova = analyze(sencha + cordovaConfig, cordovaClasses)
        assertEquals("Sencha", onCordova._APKMainFramework)
        assertEquals("Apache Cordova", onCordova._APKBaseFramework)
    }

    @Test
    fun `phonegap is reported on top of cordova`() {
        val config = mapOf("res/xml/config.xml" to """<widget xmlns:gap="http://phonegap.com/ns/1.0"><param value="org.apache.cordova.App"/></widget>""")
        val result = analyze(config, cordovaClasses)
        assertEquals("PhoneGap", result._APKMainFramework)
        assertEquals("Apache Cordova", result._APKBaseFramework)
    }

    @Test
    fun `titanium requires both TitaniumModule and TiActivity`() {
        val module = "ti.modules.titanium.TitaniumModule"
        val activity = "org.appcelerator.titanium.TiActivity"
        assertEquals("Native", analyze(classes = listOf(module))._APKMainFramework)
        assertEquals("Titanium", analyze(classes = listOf(module, activity))._APKMainFramework)
    }

    @Test
    fun `mobl requires MoblGap and a mobl source file`() {
        val moblGap = listOf("org.mobl.MoblGap")
        assertEquals("Native", analyze(classes = moblGap)._APKMainFramework)
        assertEquals("Mobl", analyze(mapOf("assets/www/app.mobl" to "screen root()"), moblGap)._APKMainFramework)
    }

    @Test
    fun `other detectors`() {
        assertEquals("RhoMobile", analyze(mapOf("assets/apps/rho.dat" to ""))._APKMainFramework)
        assertEquals("Kivy", analyze(activities = listOf("org.renpy.android.PythonActivity"))._APKMainFramework)
        assertEquals(
            "Enyo",
            analyze(mapOf("assets/www/enyo.js" to "enyo.machine = {}; enyo.kind({})"))._APKMainFramework,
        )
    }

    @Test
    fun `counts web resources by extension`() {
        val result = analyze(
            mapOf(
                "assets/www/index.html" to "",
                "assets/www/app.js" to "",
                "assets/www/app.json" to "{}",
                "assets/www/style.css" to "",
                "assets/www/data.jsonp" to "",
            )
        )
        assertEquals(1, result._html)
        assertEquals(1, result._js)
        assertEquals(1, result._css)
    }

    @Test
    fun `undefined web app above the threshold`() {
        val files = mapOf("assets/a.html" to "", "assets/b.js" to "", "assets/c.css" to "")
        assertEquals("Native", analyze(files, webAppThreshold = 0)._APKMainFramework)
        assertEquals("Native", analyze(files, webAppThreshold = 4)._APKMainFramework)
        assertEquals("Undefined_Web_App", analyze(files, webAppThreshold = 3)._APKMainFramework)
    }

    @Test
    fun `a broken apk yields minimal results instead of failing`() {
        val broken = dir.resolve("broken.apk").apply { writeText("not a zip") }
        val result = ApkAnalyzer().analyze(broken).result
        assertEquals("UNDEFINED", result._APKMainFramework)
        assertEquals("broken.apk", result._APKName)
        assertEquals("9", result._fileSize)
        assertEquals(1, result._decodeSuccess)
    }
}
