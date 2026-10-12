// SPDX-License-Identifier: MIT
package com.apkcategorychecker.analyzer

import com.apkcategorychecker.apk.ApkInput
import com.apkcategorychecker.framework.ApkInspection
import com.apkcategorychecker.framework.Framework
import com.apkcategorychecker.framework.Frameworks
import java.nio.file.Path
import java.time.Instant
import kotlin.io.path.name

/** Analyzes an app (an APK, split APKs or a container), reading it without decoding it to disk. Thread-safe. */
class ApkAnalyzer(
    private val frameworks: List<Framework> = Frameworks.all(),
) {
    fun analyze(path: Path): ApkReport = analyze(ApkInput.of(path))

    fun analyze(input: ApkInput): ApkReport {
        val path = input.path
        val start = Instant.now()
        val startNanos = System.nanoTime()

        fun elapsedMs() = (System.nanoTime() - startNanos) / 1_000_000

        return try {
            input.open().use { analyze(input, ApkInspection(it), start, ::elapsedMs) }
        } catch (e: Throwable) {
            // One broken APK must not stop the analysis of a whole directory: that includes a huge
            // APK running out of memory, or deeply nested XML overflowing the stack. Other errors
            // (e.g. a missing class) are bugs and must surface.
            if (e !is Exception && e !is OutOfMemoryError && e !is StackOverflowError) throw e
            ApkReport(
                file = path.name,
                path = path.toString(),
                fileSize = runCatching { input.size() }.getOrNull(),
                packageName = null,
                mainFramework = null,
                baseFramework = null,
                analysisStart = start.toString(),
                analysisDurationMs = elapsedMs(),
                error = e.message ?: e.javaClass.simpleName,
            )
        }
    }

    private fun analyze(
        input: ApkInput,
        inspection: ApkInspection,
        start: Instant,
        elapsedMs: () -> Long,
    ): ApkReport {
        val manifest = inspection.apk.manifest
        val matched = frameworks.mapNotNull { framework -> framework.detect(inspection)?.let { framework to it } }.toMap()
        val main = matched.keys.lastOrNull()

        return ApkReport(
            file = input.path.name,
            path = input.path.toString(),
            splits = inspection.apk.splitNames,
            fileSize = input.size(),
            packageName = manifest.packageName,
            mainFramework = main?.name ?: ApkReport.NATIVE,
            baseFramework = main?.runsOn?.firstOrNull { it in matched }?.name,
            evidence = matched.mapKeys { it.key.name },
            htmlFiles = inspection.countFiles(".html"),
            jsFiles = inspection.countFiles(".js"),
            cssFiles = inspection.countFiles(".css"),
            jsFileNames =
                inspection.apk.entryNames
                    .filter { it.endsWith(".js", ignoreCase = true) }
                    .map { it.substringAfterLast('/') },
            debuggable = manifest.debuggable,
            permissions = manifest.permissions,
            minSdkVersion = manifest.minSdkVersion,
            targetSdkVersion = manifest.targetSdkVersion,
            maxSdkVersion = manifest.maxSdkVersion,
            analysisStart = start.toString(),
            analysisDurationMs = elapsedMs(),
        )
    }
}
