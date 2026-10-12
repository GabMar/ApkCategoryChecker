// SPDX-License-Identifier: MIT
package com.apkcategorychecker.framework

/** A fact about how a detected framework is used, e.g. the JavaScript engine of a React Native app. */
class Detail(
    val key: String,
    private val value: (ApkInspection) -> String?,
) {
    fun find(apk: ApkInspection): String? = value(apk)
}

/** A detail whose value is the first option whose signal is found, e.g. `"Hermes" to nativeLib("libhermes.so")`. */
fun detail(
    key: String,
    vararg options: Pair<String, Signal>,
) = Detail(key) { apk -> options.firstOrNull { (_, signal) -> signal.find(apk) != null }?.first }

class Framework(
    val name: String,
    private val signals: List<Signal>,
    /** True if all the signals must be found, false if any of them is enough. */
    private val requireAll: Boolean,
    /** Frameworks this one is built on, in order of preference: the detected one is reported as base framework. */
    val runsOn: List<Framework> = emptyList(),
    private val details: List<Detail> = emptyList(),
) {
    /** The evidence found in the APK, or null if the framework is not detected. */
    fun detect(apk: ApkInspection): List<String>? =
        if (requireAll) {
            // Stop at the first missing signal: the next ones may be expensive (e.g. the resource table).
            signals.map { it.find(apk) ?: return null }
        } else {
            signals.mapNotNull { it.find(apk) }.ifEmpty { null }
        }

    /** The details found, for a detected framework. */
    fun details(apk: ApkInspection): Map<String, String> =
        details.mapNotNull { detail -> detail.find(apk)?.let { detail.key to it } }.toMap()

    override fun toString() = name
}

fun anyOf(
    name: String,
    vararg signals: Signal,
    runsOn: List<Framework> = emptyList(),
    details: List<Detail> = emptyList(),
) = Framework(name, signals.toList(), requireAll = false, runsOn, details)

fun allOf(
    name: String,
    vararg signals: Signal,
    runsOn: List<Framework> = emptyList(),
    details: List<Detail> = emptyList(),
) = Framework(name, signals.toList(), requireAll = true, runsOn, details)

/**
 * The known frameworks, from the most generic to the most specific: when several match,
 * the analysis reports the last one as main framework (e.g. Ionic over the Capacitor it runs on).
 *
 * Signals combine native libraries and classes where possible: split APKs from App Bundles
 * keep the native libraries in a separate per-ABI APK, and R8 may rename classes that no
 * keep rule protects.
 */
object Frameworks {
    private const val TWA_DEFAULT_URL = "android.support.customtabs.trusted.DEFAULT_URL"

    val CORDOVA =
        allOf(
            "Apache Cordova",
            classNamed("CordovaActivity"),
            either(configXmlContains("org.apache.cordova"), file("assets/www/cordova.js")),
        )

    /** Bundles the Cordova framework for plugin compatibility, so it must come after [CORDOVA]. */
    val CAPACITOR = anyOf("Capacitor", classInPackage("com.getcapacitor"), file("assets/capacitor.config.json"))

    /** A website in a full-screen Chrome tab: PWAs packaged with Bubblewrap or PWABuilder. */
    val TRUSTED_WEB_ACTIVITY =
        anyOf(
            "Trusted Web Activity",
            manifestContains(TWA_DEFAULT_URL),
            manifestContains("com.google.androidbrowserhelper.trusted"),
            classInPackage("com.google.androidbrowserhelper.trusted"),
            details = listOf(Detail("url") { it.manifestMetaData(TWA_DEFAULT_URL) }),
        )

    val REACT_NATIVE =
        anyOf(
            "React Native",
            nativeLib("libreactnativejni.so"), // up to 0.75
            nativeLib("libreactnative.so"), // 0.76+
            file("assets/index.android.bundle"),
            classInPackage("com.facebook.react"),
            details =
                listOf(
                    detail(
                        "engine",
                        "Hermes" to either(hermesBytecode("assets/index.android.bundle"), nativeLib(Regex("""libhermes.*\.so"""))),
                        "JavaScriptCore" to either(nativeLib("libjsc.so"), nativeLib("libjscexecutor.so")),
                    ),
                    // libappmodules.so holds the app's own C++ TurboModules, only built with the New Architecture.
                    detail(
                        "architecture",
                        "New" to nativeLib("libappmodules.so"),
                        "Legacy" to either(nativeLib("libreactnativejni.so"), nativeLib("libreactnative.so")),
                    ),
                ),
        )

    /** Packages Python apps; its classes keep the `org.kivy.android` package for historical reasons. */
    val PYTHON_FOR_ANDROID =
        anyOf(
            "python-for-android",
            classInPackage("org.kivy.android"),
            manifestContains("PythonActivity"),
        )

    /** Xamarin.Android and its successor. */
    val DOTNET =
        anyOf(
            ".NET for Android",
            nativeLib("libmonodroid.so"),
            classInPackage("mono.android"),
            details = listOf(detail("runtime", "CoreCLR" to nativeLib("libcoreclr.so"), "Mono" to nativeLib("libmonosgen-2.0.so"))),
        )

    /** Details of apps without a cross-platform framework. */
    val NATIVE_DETAILS =
        listOf(
            // Compose may also come with a library, and the UI may still mix in Views: this means Compose is included.
            detail(
                "ui",
                "Jetpack Compose" to either(file("META-INF/androidx.compose.ui_ui.version"), classInPackage("androidx.compose.ui")),
            ),
        )

    /** Any app with at least [threshold] web resource files; disabled when [threshold] is 0. */
    fun undefinedWebApp(threshold: Int) =
        anyOf(
            "Undefined_Web_App",
            Signal { apk ->
                val count = apk.countFiles(".html") + apk.countFiles(".js") + apk.countFiles(".css")
                if (threshold > 0 && count >= threshold) "$count web resource files (threshold $threshold)" else null
            },
        )

    fun all(webAppThreshold: Int = 0): List<Framework> =
        listOf(
            undefinedWebApp(webAppThreshold),
            // Runtimes and engines
            CORDOVA,
            CAPACITOR,
            TRUSTED_WEB_ACTIVITY,
            REACT_NATIVE,
            DOTNET,
            PYTHON_FOR_ANDROID,
            allOf("Titanium", classNamed("TitaniumModule"), classNamed("TiActivity")),
            anyOf("NativeScript", nativeLib("libNativeScript.so"), classInPackage("com.tns")),
            anyOf("Tauri", classInPackage("app.tauri"), file("assets/tauri.conf.json")),
            anyOf("Lynx", nativeLib("liblynx.so"), classInPackage("com.lynx.tasm"), file("assets/lynx_core.js")),
            anyOf("Kony", nativeLib(Regex("""libkony.*\.so""")), classInPackage("com.konylabs")),
            anyOf(
                "Flutter",
                nativeLib("libflutter.so"),
                classInPackage("io.flutter"),
                // Release builds ship the Dart code compiled ahead of time, debug builds a kernel for the JIT.
                details =
                    listOf(
                        detail(
                            "build",
                            "release" to nativeLib("libapp.so"),
                            "debug" to file("assets/flutter_assets/kernel_blob.bin"),
                        ),
                    ),
            ),
            anyOf(
                "Qt",
                nativeLib(Regex("""libQt[56]Core.*\.so""")),
                classInPackage("org.qtproject.qt"), // Qt 6
                classInPackage("org.qtproject.qt5"),
                runsOn = listOf(PYTHON_FOR_ANDROID), // PySide/PyQt apps
            ),
            anyOf(
                "Unity",
                nativeLib("libunity.so"),
                classInPackage("com.unity3d.player"),
                details =
                    listOf(
                        detail(
                            "scripting backend",
                            "IL2CPP" to nativeLib("libil2cpp.so"),
                            "Mono" to
                                either(nativeLib("libmonobdwgc-2.0.so"), nativeLib("libmono.so"), fileUnder("assets/bin/Data/Managed/")),
                        ),
                    ),
            ),
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
            anyOf("libGDX", nativeLib("libgdx.so"), classInPackage("com.badlogic.gdx")),
            anyOf("Defold", nativeLib("libdmengine.so"), classInPackage("com.dynamo.android")),
            anyOf("Solar2D", nativeLib("libcorona.so"), classInPackage("com.ansca.corona")),
            anyOf("GameMaker", nativeLib("libyoyo.so"), classInPackage("com.yoyogames.runner"), file("assets/game.droid")),
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
            anyOf(
                "Expo",
                nativeLib("libexpo-modules-core.so"),
                file("assets/app.config"),
                classInPackage("expo.modules"),
                runsOn = listOf(REACT_NATIVE),
            ),
            anyOf(
                "Xamarin.Forms",
                classNamed("FormsAppCompatActivity"),
                classNamed("FormsApplicationActivity"),
                runsOn = listOf(DOTNET),
            ),
            anyOf("Avalonia", classNamed("AvaloniaMainActivity"), classNamed("AvaloniaView"), runsOn = listOf(DOTNET)),
            anyOf(
                ".NET MAUI",
                classNamed("MauiAppCompatActivity"),
                classInPackage("com.microsoft.maui"),
                runsOn = listOf(DOTNET),
            ),
        )
}
