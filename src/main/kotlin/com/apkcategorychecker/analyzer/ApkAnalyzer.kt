// SPDX-License-Identifier: GPL-3.0-or-later
package com.apkcategorychecker.analyzer

import com.apkcategorychecker.apk.ApkFile
import com.apkcategorychecker.framework.ApkInspection
import com.apkcategorychecker.framework.Framework
import com.apkcategorychecker.framework.Frameworks
import java.nio.file.Path
import kotlin.io.path.fileSize
import kotlin.io.path.name

class ApkAnalysis(val result: AnalyzerResult, val jsFiles: JsElement)

/** Analyzes a single APK, reading it directly without decoding it to disk. */
class ApkAnalyzer(private val frameworks: List<Framework> = Frameworks.all()) {

    fun analyze(path: Path): ApkAnalysis {
        val startTime = System.currentTimeMillis()
        return try {
            ApkFile(path).use { analyze(path, ApkInspection(it), startTime) }
        } catch (e: Exception) {
            // One broken APK must not stop the analysis of a whole directory.
            System.err.println("Could not analyze $path (${e.message}), writing minimal results.")
            failed(path, startTime)
        }
    }

    private fun analyze(path: Path, inspection: ApkInspection, startTime: Long): ApkAnalysis {
        val manifest = inspection.apk.manifest
        val matched = frameworks.mapNotNull { framework -> framework.detect(inspection)?.let { framework to it } }.toMap()
        val main = matched.keys.lastOrNull()
        val base = main?.runsOn?.firstOrNull { it in matched }
        val packageName = manifest.packageName.orEmpty()

        val result = AnalyzerResult().apply {
            set_Package(packageName)
            set_APKPath(path.toString())
            set_APKName(path.name)
            set_APKMainFramework(main?.name ?: NATIVE)
            set_APKBaseFramework(base?.name.orEmpty())
            set_html(inspection.countFiles(".html"))
            set_js(inspection.countFiles(".js"))
            set_css(inspection.countFiles(".css"))
            set_debuggable(manifest.debuggable.orEmpty())
            set_permissions(manifest.permissions.joinToString(", "))
            set_minSdkVersion(manifest.minSdkVersion ?: UNDEFINED)
            set_maxSdkVersion(manifest.maxSdkVersion ?: UNDEFINED)
            set_targetSdkVersion(manifest.targetSdkVersion ?: UNDEFINED)
            set_fileSize(path.fileSize().toString())
            set_startAnalysis(startTime)
            set_durationAnalysis(System.currentTimeMillis() - startTime)
            set_decodeSuccess(0)
            set_evidence(matched.entries.joinToString(" | ") { (framework, evidence) -> "${framework.name}: ${evidence.joinToString(", ")}" })
        }
        val jsFiles = inspection.apk.entryNames
            .filter { it.endsWith(".js", ignoreCase = true) }
            .joinToString(", ") { it.substringAfterLast('/') }
        return ApkAnalysis(result, jsElement(packageName, jsFiles))
    }

    private fun failed(path: Path, startTime: Long): ApkAnalysis {
        val result = AnalyzerResult().apply {
            set_Package(UNDEFINED)
            set_APKPath(path.toString())
            set_APKName(path.name)
            set_APKMainFramework(UNDEFINED)
            set_APKBaseFramework("")
            set_debuggable(UNDEFINED)
            set_permissions(UNDEFINED)
            set_minSdkVersion(UNDEFINED)
            set_maxSdkVersion(UNDEFINED)
            set_targetSdkVersion(UNDEFINED)
            set_fileSize(runCatching { path.fileSize().toString() }.getOrDefault(UNDEFINED))
            set_startAnalysis(startTime)
            set_durationAnalysis(System.currentTimeMillis() - startTime)
            set_decodeSuccess(1)
        }
        return ApkAnalysis(result, jsElement(UNDEFINED, UNDEFINED))
    }

    private fun jsElement(appId: String, jsFiles: String) = JsElement().apply {
        set_id(appId)
        set_JsFiles(jsFiles)
    }

    private companion object {
        const val UNDEFINED = "UNDEFINED"
        const val NATIVE = "Native"
    }
}
