// SPDX-License-Identifier: MIT
package com.apkcategorychecker.analyzer

import kotlinx.serialization.Serializable

/** The result of the analysis of one APK. Fields are null when unknown or not declared. */
@Serializable
data class ApkReport(
    val file: String,
    val path: String,
    /** File names of the split APKs analyzed together with [path], the base APK or container. */
    val splits: List<String> = emptyList(),
    /** Total size of the app's APK files in bytes. */
    val fileSize: Long?,
    val packageName: String?,
    /** The most specific framework detected, or [NATIVE]. */
    val mainFramework: String?,
    /** The runtime [mainFramework] is built on, if detected (e.g. Capacitor for Ionic). */
    val baseFramework: String?,
    /** For each detected framework, what was found in the APK. */
    val evidence: Map<String, List<String>> = emptyMap(),
    /**
     * Facts about the detected frameworks, e.g. `{"React Native": {"engine": "Hermes"}}`; for apps
     * without a cross-platform framework, under [NATIVE] (e.g. whether Jetpack Compose is included).
     */
    val details: Map<String, Map<String, String>> = emptyMap(),
    val htmlFiles: Int = 0,
    val jsFiles: Int = 0,
    val cssFiles: Int = 0,
    /** File names of the JavaScript files in the APK. */
    val jsFileNames: List<String> = emptyList(),
    /** Raw value of `android:debuggable`. */
    val debuggable: String? = null,
    val permissions: List<String> = emptyList(),
    val minSdkVersion: String? = null,
    val targetSdkVersion: String? = null,
    val maxSdkVersion: String? = null,
    /** Analysis start, as an ISO-8601 instant. */
    val analysisStart: String,
    val analysisDurationMs: Long,
    /** Why the APK could not be analyzed, or null on success. */
    val error: String? = null,
) {
    val success: Boolean get() = error == null

    companion object {
        const val NATIVE = "Native"
    }
}
