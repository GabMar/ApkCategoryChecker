package com.apkcategorychecker.analyzer

import com.apkcategorychecker.apk.syntheticApk
import com.apkcategorychecker.framework.Frameworks
import org.junit.jupiter.api.Assertions.assertEquals
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
    ): AnalyzerResult {
        val apk = syntheticApk(dir.resolve("test.apk"), files, classes, activities = activities)
        return ApkAnalyzer(Frameworks.all(webAppThreshold)).analyze(apk).result
    }

    private val cordovaConfig = mapOf("res/xml/config.xml" to """<widget><param value="org.apache.cordova.App"/></widget>""")
    private val cordovaClasses = listOf("org.apache.cordova.CordovaActivity")

    @Test
    fun `an app without known frameworks is native`() {
        val result = analyze(classes = listOf("com.example.app.MainActivity"))
        assertEquals("Native", result._APKMainFramework)
        assertEquals("", result._APKBaseFramework)
        assertEquals("", result._evidence)
        assertEquals("com.example.app", result._Package)
        assertEquals(0, result._decodeSuccess)
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
        assertEquals(expected, result._APKMainFramework)
        assertEquals("$expected: $evidence", result._evidence)
    }

    @Test
    fun `cordova requires CordovaActivity and either config xml or cordova js`() {
        assertEquals("Native", analyze(cordovaConfig)._APKMainFramework)
        assertEquals("Native", analyze(classes = cordovaClasses)._APKMainFramework)

        val withConfig = analyze(cordovaConfig, cordovaClasses)
        assertEquals("Apache Cordova", withConfig._APKMainFramework)
        assertEquals(
            "Apache Cordova: class org.apache.cordova.CordovaActivity, res/xml/config.xml contains \"org.apache.cordova\"",
            withConfig._evidence,
        )
        // Resource shrinking can rename res/xml/config.xml.
        val withCordovaJs = analyze(mapOf("assets/www/cordova.js" to ""), cordovaClasses)
        assertEquals("Apache Cordova", withCordovaJs._APKMainFramework)
    }

    @Test
    fun `capacitor wins over the cordova runtime it bundles`() {
        val result = analyze(
            cordovaConfig + ("assets/capacitor.config.json" to "{}"),
            cordovaClasses + "com.getcapacitor.BridgeActivity",
        )
        assertEquals("Capacitor", result._APKMainFramework)
        assertEquals("", result._APKBaseFramework)
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
    fun `web frameworks report the runtime they run on as base framework`() {
        val sencha = mapOf("assets/www/app.js" to "Ext.create(); Ext.application();")
        assertEquals("", analyze(sencha)._APKBaseFramework)

        val onCordova = analyze(sencha + cordovaConfig, cordovaClasses)
        assertEquals("Sencha", onCordova._APKMainFramework)
        assertEquals("Apache Cordova", onCordova._APKBaseFramework)

        val ionic = analyze(
            mapOf("assets/public/main.js" to "customElements.define('ion-app', IonApp)"),
            listOf("com.getcapacitor.BridgeActivity"),
        )
        assertEquals("Ionic", ionic._APKMainFramework)
        assertEquals("Capacitor", ionic._APKBaseFramework)
        assertEquals(
            "Capacitor: class com.getcapacitor.BridgeActivity | Ionic: assets/public/main.js contains \"ion-app\"",
            ionic._evidence,
        )
    }

    @Test
    fun `expo runs on react native`() {
        val result = analyze(
            mapOf("lib/arm64-v8a/libreactnative.so" to ""),
            listOf("com.facebook.react.ReactActivity", "expo.modules.ExpoModulesPackage"),
        )
        assertEquals("Expo", result._APKMainFramework)
        assertEquals("React Native", result._APKBaseFramework)
    }

    @Test
    fun `dotnet ui frameworks run on dotnet for android`() {
        val runtime = mapOf("lib/arm64-v8a/libmonodroid.so" to "")
        val maui = analyze(runtime, listOf("crc6452ffdc5b34af3a0f.MauiAppCompatActivity"))
        assertEquals(".NET MAUI", maui._APKMainFramework)
        assertEquals(".NET for Android", maui._APKBaseFramework)

        val forms = analyze(runtime, listOf("crc643f46942d9dd1fff9.FormsAppCompatActivity"))
        assertEquals("Xamarin.Forms", forms._APKMainFramework)
        assertEquals(".NET for Android", forms._APKBaseFramework)

        assertEquals(".NET for Android", analyze(runtime)._APKMainFramework)
    }

    @Test
    fun `titanium requires both TitaniumModule and TiActivity`() {
        val module = "ti.modules.titanium.TitaniumModule"
        val activity = "org.appcelerator.titanium.TiActivity"
        assertEquals("Native", analyze(classes = listOf(module))._APKMainFramework)
        assertEquals("Titanium", analyze(classes = listOf(module, activity))._APKMainFramework)
    }

    @Test
    fun `kivy is detected from the python activity`() {
        assertEquals("Kivy", analyze(activities = listOf("org.kivy.android.PythonActivity"))._APKMainFramework)
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
        val result = analyze(files, webAppThreshold = 3)
        assertEquals("Undefined_Web_App", result._APKMainFramework)
        assertEquals("Undefined_Web_App: 3 web resource files (threshold 3)", result._evidence)
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
