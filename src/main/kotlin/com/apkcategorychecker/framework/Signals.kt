// SPDX-License-Identifier: MIT
package com.apkcategorychecker.framework

/** Something that can be found in an APK: returns a human-readable evidence, or null if absent. */
fun interface Signal {
    fun find(apk: ApkInspection): String?
}

/** A native library in any ABI directory, e.g. `lib/arm64-v8a/libflutter.so`. */
fun nativeLib(fileName: String) =
    Signal { apk ->
        apk.apk.entryNames
            .filter { it.startsWith("lib/") && it.substringAfterLast('/') == fileName }
            .minOrNull()
    }

fun nativeLib(fileName: Regex) =
    Signal { apk ->
        apk.apk.entryNames
            .filter { it.startsWith("lib/") && fileName.matches(it.substringAfterLast('/')) }
            .minOrNull()
    }

/** Any class in the package or its subpackages. */
fun classInPackage(packageName: String) = Signal { apk -> apk.firstClassInPackage(packageName)?.let { "class $it" } }

/** A class with this simple name in any package (class names are kept by JNI or reflection keep rules). */
fun classNamed(simpleName: String) = Signal { apk -> apk.firstClassNamed(simpleName)?.let { "class $it" } }

fun file(path: String) = Signal { apk -> path.takeIf { it in apk.entries } }

/** Any file under the directory, e.g. `assets/composeResources/`. */
fun fileUnder(dir: String) =
    Signal { apk ->
        apk.apk.entryNames
            .filter { it.startsWith(dir) }
            .minOrNull()
    }

fun configXmlContains(text: String) =
    Signal { apk ->
        if (text in apk.configXml) "${apk.configXmlPath} contains \"$text\"" else null
    }

fun manifestContains(text: String) =
    Signal { apk ->
        if (text in apk.manifestXml) "AndroidManifest.xml contains \"$text\"" else null
    }

/** A web asset of a Cordova (`assets/www/`) or Capacitor (`assets/public/`) app containing [text]. */
fun webAssetContains(
    text: String,
    extension: String? = null,
) = Signal { apk ->
    apk.findAssetContaining(text, WEB_ROOTS, extension)?.let { "$it contains \"$text\"" }
}

/** The first of [signals] that is found. */
fun either(vararg signals: Signal) = Signal { apk -> signals.firstNotNullOfOrNull { it.find(apk) } }

private val WEB_ROOTS = listOf("assets/www/", "assets/public/")
