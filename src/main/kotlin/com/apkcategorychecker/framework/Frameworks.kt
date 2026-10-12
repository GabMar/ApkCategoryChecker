// SPDX-License-Identifier: MIT
package com.apkcategorychecker.framework

class Framework(
    val name: String,
    private val signals: List<Signal>,
    /** True if all the signals must be found, false if any of them is enough. */
    private val requireAll: Boolean,
    /** Frameworks this one is built on, in order of preference: the detected one is reported as base framework. */
    val runsOn: List<Framework> = emptyList(),
) {
    /** The evidence found in the APK, or null if the framework is not detected. */
    fun detect(apk: ApkInspection): List<String>? {
        val found = signals.map { it.find(apk) }
        return if (requireAll) {
            found.takeIf { results -> results.all { it != null } }?.filterNotNull()
        } else {
            found.filterNotNull().ifEmpty { null }
        }
    }

    override fun toString() = name
}

fun anyOf(name: String, vararg signals: Signal, runsOn: List<Framework> = emptyList()) =
    Framework(name, signals.toList(), requireAll = false, runsOn)

fun allOf(name: String, vararg signals: Signal, runsOn: List<Framework> = emptyList()) =
    Framework(name, signals.toList(), requireAll = true, runsOn)

/**
 * The known frameworks, from the most generic to the most specific: when several match,
 * the analysis reports the last one as main framework (e.g. Ionic over the Capacitor it runs on).
 *
 * Signals combine native libraries and classes where possible: split APKs from App Bundles
 * keep the native libraries in a separate per-ABI APK, and R8 may rename classes that no
 * keep rule protects.
 */
object Frameworks {

    val CORDOVA = allOf(
        "Apache Cordova",
        classNamed("CordovaActivity"),
        either(configXmlContains("org.apache.cordova"), file("assets/www/cordova.js")),
    )

    /** Bundles the Cordova framework for plugin compatibility, so it must come after [CORDOVA]. */
    val CAPACITOR = anyOf("Capacitor", classInPackage("com.getcapacitor"), file("assets/capacitor.config.json"))

    val REACT_NATIVE = anyOf(
        "React Native",
        nativeLib("libreactnativejni.so"), // up to 0.75
        nativeLib("libreactnative.so"), // 0.76+
        file("assets/index.android.bundle"),
        classInPackage("com.facebook.react"),
    )

    /** Packages Python apps; its classes keep the `org.kivy.android` package for historical reasons. */
    val PYTHON_FOR_ANDROID = anyOf(
        "python-for-android",
        classInPackage("org.kivy.android"),
        manifestContains("PythonActivity"),
    )

    /** Xamarin.Android and its successor. */
    val DOTNET = anyOf(".NET for Android", nativeLib("libmonodroid.so"), classInPackage("mono.android"))

    /** Any app with at least [threshold] web resource files; disabled when [threshold] is 0. */
    fun undefinedWebApp(threshold: Int) = anyOf("Undefined_Web_App", Signal { apk ->
        val count = apk.countFiles(".html") + apk.countFiles(".js") + apk.countFiles(".css")
        if (threshold > 0 && count >= threshold) "$count web resource files (threshold $threshold)" else null
    })

    fun all(webAppThreshold: Int = 0): List<Framework> = listOf(
        undefinedWebApp(webAppThreshold),

        // Runtimes and engines
        CORDOVA,
        CAPACITOR,
        REACT_NATIVE,
        DOTNET,
        PYTHON_FOR_ANDROID,
        allOf("Titanium", classNamed("TitaniumModule"), classNamed("TiActivity")),
        anyOf("NativeScript", nativeLib("libNativeScript.so"), classInPackage("com.tns")),
        anyOf("Flutter", nativeLib("libflutter.so"), classInPackage("io.flutter")),
        anyOf(
            "Qt",
            nativeLib(Regex("""libQt[56]Core.*\.so""")),
            classInPackage("org.qtproject.qt"), // Qt 6
            classInPackage("org.qtproject.qt5"),
            runsOn = listOf(PYTHON_FOR_ANDROID), // PySide/PyQt apps
        ),
        anyOf("Unity", nativeLib("libunity.so"), classInPackage("com.unity3d.player")),
        anyOf(
            "Unreal Engine",
            nativeLib("libUE4.so"),
            nativeLib("libUnreal.so"),
            classInPackage("com.epicgames.ue4"),
            classInPackage("com.epicgames.unreal"),
        ),
        anyOf("Godot", nativeLib("libgodot_android.so"), classInPackage("org.godotengine.godot")),
        anyOf(
            "Cocos",
            nativeLib("libcocos2dcpp.so"), // Cocos2d-x
            nativeLib("libcocos.so"), // Cocos Creator 3
            classInPackage("org.cocos2dx.lib"),
            classInPackage("com.cocos.lib"),
        ),
        anyOf(
            "Compose Multiplatform",
            classInPackage("org.jetbrains.compose.resources"),
            fileUnder("assets/composeResources/"),
            fileUnder("composeResources/"),
        ),

        // Frameworks built on the runtimes above
        allOf(
            "Kivy",
            classInPackage("org.kivy.android"),
            either(nativeLib("libSDL2.so"), nativeLib("libSDL3.so")), // Kivy's bootstraps; Qt apps use the qt one
            runsOn = listOf(PYTHON_FOR_ANDROID),
        ),
        allOf(
            "Sencha",
            webAssetContains("Ext.create", ".js"),
            webAssetContains("Ext.application", ".js"),
            runsOn = listOf(CAPACITOR, CORDOVA),
        ),
        anyOf("Ionic", webAssetContains("ion-app", ".js"), runsOn = listOf(CAPACITOR, CORDOVA)),
        anyOf("Expo", classInPackage("expo.modules"), runsOn = listOf(REACT_NATIVE)),
        anyOf(
            "Xamarin.Forms",
            classNamed("FormsAppCompatActivity"),
            classNamed("FormsApplicationActivity"),
            runsOn = listOf(DOTNET),
        ),
        anyOf(
            ".NET MAUI",
            classNamed("MauiAppCompatActivity"),
            classInPackage("com.microsoft.maui"),
            runsOn = listOf(DOTNET),
        ),
    )
}
