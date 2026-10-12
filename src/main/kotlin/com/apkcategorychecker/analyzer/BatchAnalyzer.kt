// SPDX-License-Identifier: MIT
package com.apkcategorychecker.analyzer

import com.apkcategorychecker.apk.ApkInput
import java.util.concurrent.Executors

/** Analyzes many apps in parallel, delivering the reports in the given order. */
class BatchAnalyzer(
    private val analyzer: ApkAnalyzer,
    private val threads: Int,
) {
    init {
        require(threads > 0) { "threads must be positive" }
    }

    fun analyze(
        inputs: List<ApkInput>,
        onReport: (ApkReport) -> Unit,
    ) {
        Executors.newFixedThreadPool(threads).use { executor ->
            // Only `threads` apps are held in memory at a time; reports are small and wait in order.
            val futures = inputs.map { input -> executor.submit<ApkReport> { analyzer.analyze(input) } }
            futures.forEach { onReport(it.get()) }
        }
    }
}
