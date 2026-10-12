package com.apkcategorychecker.analyzer

import com.apkcategorychecker.apk.syntheticApk
import com.apkcategorychecker.framework.Frameworks
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments
import org.junit.jupiter.params.provider.MethodSource
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
    ): ApkReport {
        val apk = syntheticApk(dir.resolve("test.apk"), files, classes, activities = activities)
        return ApkAnalyzer(Frameworks.all(webAppThreshold)).analyze(apk)
    }

    private fun ApkReport.evidenceText() =
        evidence.entries.joinToString(" | ") { (framework, found) -> "$framework: ${found.joinToString(", ")}" }

    private val cordovaConfig = mapOf("res/xml/config.xml" to """<widget><param value="org.apache.cordova.App"/></widget>""")
    private val cordovaClasses = listOf("org.apache.cordova.CordovaActivity")

    @Test
    fun `an app without known frameworks is native`() {
        val result = analyze(classes = listOf("com.example.app.MainActivity"))
        assertEquals("Native", result.mainFramework)
        assertNull(result.baseFramework)
        assertEquals(emptyMap<String, List<String>>(), result.evidence)
        assertEquals("com.example.app", result.packageName)
        assertTrue(result.success)
    }

    @ParameterizedTest(name = "{0}")
    @MethodSource("singleSignalApps")
    fun `detects frameworks from native libraries, classes and files`(
        expected: String,
        files: Map<String, String>,
        classes: List<String>,
        evidence: String,
    ) {
        val result = analyze(files, classes)
        assertEquals(expected, result.mainFramework)
        assertEquals("$expected: $evidence", result.evidenceText())
    }

    @Test
    fun `cordova requires CordovaActivity and either config xml or cordova js`() {
        assertEquals("Native", analyze(cordovaConfig).mainFramework)
        assertEquals("Native", analyze(classes = cordovaClasses).mainFramework)

        val withConfig = analyze(cordovaConfig, cordovaClasses)
        assertEquals("Apache Cordova", withConfig.mainFramework)
        assertEquals(
            "Apache Cordova: class org.apache.cordova.CordovaActivity, res/xml/config.xml contains \"org.apache.cordova\"",
            withConfig.evidenceText(),
        )
        // Resource shrinking can rename res/xml/config.xml.
        val withCordovaJs = analyze(mapOf("assets/www/cordova.js" to ""), cordovaClasses)
        assertEquals("Apache Cordova", withCordovaJs.mainFramework)
    }

    @Test
    fun `capacitor wins over the cordova runtime it bundles`() {
        val result = analyze(
            cordovaConfig + ("assets/capacitor.config.json" to "{}"),
            cordovaClasses + "com.getcapacitor.BridgeActivity",
        )
        assertEquals("Capacitor", result.mainFramework)
        assertNull(result.baseFramework)
    }

    @Test
    fun `sencha requires both Ext create and Ext application`() {
        val create = "assets/www/a.js" to "Ext.create('Ext.Panel')"
        val application = "assets/www/b.js" to "Ext.application({})"
        assertEquals("Native", analyze(mapOf(create)).mainFramework)
        assertEquals("Native", analyze(mapOf(application)).mainFramework)
        assertEquals("Sencha", analyze(mapOf(create, application)).mainFramework)
    }

    @Test
    fun `web frameworks report the runtime they run on as base framework`() {
        val sencha = mapOf("assets/www/app.js" to "Ext.create(); Ext.application();")
        assertNull(analyze(sencha).baseFramework)

        val onCordova = analyze(sencha + cordovaConfig, cordovaClasses)
        assertEquals("Sencha", onCordova.mainFramework)
        assertEquals("Apache Cordova", onCordova.baseFramework)

        val ionic = analyze(
            mapOf("assets/public/main.js" to "customElements.define('ion-app', IonApp)"),
            listOf("com.getcapacitor.BridgeActivity"),
        )
        assertEquals("Ionic", ionic.mainFramework)
        assertEquals("Capacitor", ionic.baseFramework)
        assertEquals(
            "Capacitor: class com.getcapacitor.BridgeActivity | Ionic: assets/public/main.js contains \"ion-app\"",
            ionic.evidenceText(),
        )
    }

    @Test
    fun `expo runs on react native`() {
        val result = analyze(
            mapOf("lib/arm64-v8a/libreactnative.so" to ""),
            listOf("com.facebook.react.ReactActivity", "expo.modules.ExpoModulesPackage"),
        )
        assertEquals("Expo", result.mainFramework)
        assertEquals("React Native", result.baseFramework)
    }

    @Test
    fun `dotnet ui frameworks run on dotnet for android`() {
        val runtime = mapOf("lib/arm64-v8a/libmonodroid.so" to "")
        val maui = analyze(runtime, listOf("crc6452ffdc5b34af3a0f.MauiAppCompatActivity"))
        assertEquals(".NET MAUI", maui.mainFramework)
        assertEquals(".NET for Android", maui.baseFramework)

        val forms = analyze(runtime, listOf("crc643f46942d9dd1fff9.FormsAppCompatActivity"))
        assertEquals("Xamarin.Forms", forms.mainFramework)
        assertEquals(".NET for Android", forms.baseFramework)

        assertEquals(".NET for Android", analyze(runtime).mainFramework)
    }

    @Test
    fun `titanium requires both TitaniumModule and TiActivity`() {
        val module = "ti.modules.titanium.TitaniumModule"
        val activity = "org.appcelerator.titanium.TiActivity"
        assertEquals("Native", analyze(classes = listOf(module)).mainFramework)
        assertEquals("Titanium", analyze(classes = listOf(module, activity)).mainFramework)
    }

    @Test
    fun `kivy is detected from the python activity`() {
        assertEquals("Kivy", analyze(activities = listOf("org.kivy.android.PythonActivity")).mainFramework)
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
        assertEquals(1, result.htmlFiles)
        assertEquals(1, result.jsFiles)
        assertEquals(1, result.cssFiles)
    }

    @Test
    fun `undefined web app above the threshold`() {
        val files = mapOf("assets/a.html" to "", "assets/b.js" to "", "assets/c.css" to "")
        assertEquals("Native", analyze(files, webAppThreshold = 0).mainFramework)
        assertEquals("Native", analyze(files, webAppThreshold = 4).mainFramework)
        val result = analyze(files, webAppThreshold = 3)
        assertEquals("Undefined_Web_App", result.mainFramework)
        assertEquals("Undefined_Web_App: 3 web resource files (threshold 3)", result.evidenceText())
    }

    @Test
    fun `a broken apk yields minimal results instead of failing`() {
        val broken = dir.resolve("broken.apk").apply { writeText("not a zip") }
        val result = ApkAnalyzer().analyze(broken)
        assertNull(result.mainFramework)
        assertEquals("broken.apk", result.file)
        assertEquals(9L, result.fileSize)
        assertFalse(result.success)
    }

    companion object {
        private fun app(name: String, file: String, evidence: String = file) =
            Arguments.of(name, mapOf(file to ""), emptyList<String>(), evidence)

        private fun appWithClass(name: String, className: String) =
            Arguments.of(name, emptyMap<String, String>(), listOf(className), "class $className")

        @JvmStatic
        fun singleSignalApps() = listOf(
            app("Flutter", "lib/arm64-v8a/libflutter.so"),
            appWithClass("Flutter", "io.flutter.embedding.engine.FlutterEngine"),
            app("React Native", "lib/armeabi-v7a/libreactnativejni.so"),
            app("React Native", "assets/index.android.bundle"),
            appWithClass("React Native", "com.facebook.react.ReactActivity"),
            app(".NET for Android", "lib/x86_64/libmonodroid.so"),
            appWithClass(".NET for Android", "mono.android.Runtime"),
            app("Unity", "lib/arm64-v8a/libunity.so"),
            appWithClass("Unity", "com.unity3d.player.UnityPlayer"),
            app("Unreal Engine", "lib/arm64-v8a/libUnreal.so"),
            appWithClass("Unreal Engine", "com.epicgames.ue4.GameActivity"),
            app("Godot", "lib/arm64-v8a/libgodot_android.so"),
            app("Cocos", "lib/armeabi-v7a/libcocos2dcpp.so"),
            appWithClass("Cocos", "com.cocos.lib.CocosActivity"),
            app("Capacitor", "assets/capacitor.config.json"),
            app("NativeScript", "lib/arm64-v8a/libNativeScript.so"),
            appWithClass("NativeScript", "com.tns.Runtime"),
            app("Qt", "lib/arm64-v8a/libQt6Core_arm64-v8a.so"),
            appWithClass("Qt", "org.qtproject.qt5.android.bindings.QtActivity"),
            app("Compose Multiplatform", "assets/composeResources/app.generated.resources/values/strings.commonMain.cvr"),
        )
    }
}
