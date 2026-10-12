// SPDX-License-Identifier: MIT
package com.apkcategorychecker.analyzer

import java.nio.file.Path
import java.util.concurrent.Executors
import kotlin.io.path.extension
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.walk

/** Analyzes many APKs in parallel, delivering the reports in the order of [findApks]. */
class BatchAnalyzer(
    private val analyzer: ApkAnalyzer,
    private val threads: Int,
) {
    init {
        require(threads > 0) { "threads must be positive" }
    }

    fun analyze(
        apks: List<Path>,
        onReport: (ApkReport) -> Unit,
    ) {
        Executors.newFixedThreadPool(threads).use { executor ->
            // Only `threads` APKs are held in memory at a time; reports are small and wait in order.
            val futures = apks.map { apk -> executor.submit<ApkReport> { analyzer.analyze(apk) } }
            futures.forEach { onReport(it.get()) }
        }
    }

    companion object {
        /** The given APK files, plus the `.apk` files found recursively in the given directories, sorted. */
        fun findApks(paths: List<Path>): List<Path> =
            paths
                .flatMap { path ->
                    if (path.isDirectory()) {
                        path
                            .walk()
                            .filter { it.isRegularFile() && it.extension.equals("apk", ignoreCase = true) }
                            .sorted()
                            .toList()
                    } else {
                        listOf(path)
                    }
                }.distinct()
    }
}
