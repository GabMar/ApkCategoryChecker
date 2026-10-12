// SPDX-License-Identifier: MIT
package com.apkcategorychecker.cli

import com.apkcategorychecker.analyzer.ApkAnalyzer
import com.apkcategorychecker.analyzer.BatchAnalyzer
import com.apkcategorychecker.apk.ApkInput
import com.apkcategorychecker.framework.Frameworks
import com.apkcategorychecker.output.OutputFormat
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.FilterWriter
import java.io.PrintWriter
import java.io.Writer
import java.nio.file.Path
import java.util.concurrent.Callable
import kotlin.io.path.bufferedWriter
import kotlin.io.path.exists
import kotlin.system.exitProcess

@Command(
    name = "apk-category-checker",
    mixinStandardHelpOptions = true,
    versionProvider = ManifestVersionProvider::class,
    description = ["Detects the framework used to build Android APKs (Flutter, React Native, Cordova, Unity, ...)."],
    sortOptions = false,
    usageHelpAutoWidth = true,
)
class ApkCategoryChecker : Callable<Int> {
    @Parameters(
        arity = "1..*",
        paramLabel = "PATH",
        description = [
            "APK files, APK containers (.xapk, .apks, .apkm) or directories searched recursively for them.",
            "Split APKs of the same app in one directory are analyzed together.",
        ],
    )
    lateinit var paths: List<Path>

    @Option(names = ["-o", "--output"], paramLabel = "FILE", description = ["Write the results to FILE instead of standard output."])
    var output: Path? = null

    @Option(
        names = ["-f", "--format"],
        paramLabel = "FORMAT",
        description = ["Output format: \${COMPLETION-CANDIDATES}. Default: from the output file extension, otherwise csv."],
    )
    var format: OutputFormat? = null

    @Option(
        names = ["-j", "--threads"],
        paramLabel = "N",
        description = ["APKs analyzed in parallel. Default: number of CPUs (\${DEFAULT-VALUE})."],
    )
    var threads: Int = Runtime.getRuntime().availableProcessors()

    @Option(
        names = ["--web-app-threshold"],
        paramLabel = "N",
        description = ["Report apps with at least N .html/.js/.css files and no other match as Undefined_Web_App. Default: disabled."],
    )
    var webAppThreshold: Int = 0

    @CommandLine.Spec
    lateinit var spec: CommandLine.Model.CommandSpec

    override fun call(): Int {
        val err = spec.commandLine().err
        paths.filterNot { it.exists() }.takeIf { it.isNotEmpty() }?.let { missing ->
            throw CommandLine.ParameterException(spec.commandLine(), "Path not found: ${missing.joinToString()}")
        }
        if (threads < 1) throw CommandLine.ParameterException(spec.commandLine(), "--threads must be at least 1")

        val apps = ApkInput.find(paths)
        val outputFormat = format ?: output?.let { OutputFormat.fromExtension(it.fileName.toString()) } ?: OutputFormat.CSV
        val out = output?.bufferedWriter() ?: NonClosingWriter(spec.commandLine().out)

        var failures = 0
        outputFormat.writer(out).use { writer ->
            BatchAnalyzer(ApkAnalyzer(Frameworks.all(webAppThreshold)), threads).analyze(apps) { report ->
                writer.write(report)
                if (!report.success) {
                    failures++
                    err.println("Could not analyze ${report.path}: ${report.error}")
                }
            }
        }
        err.println("Analyzed ${apps.size} app(s), $failures failed.")
        return 0
    }
}

/** Lets the report writer close its output without closing standard output. */
private class NonClosingWriter(
    out: Writer,
) : FilterWriter(out) {
    override fun close() = flush()
}

class ManifestVersionProvider : CommandLine.IVersionProvider {
    override fun getVersion(): Array<String> =
        arrayOf("apk-category-checker " + (javaClass.`package`.implementationVersion ?: "development build"))
}

fun run(
    args: Array<String>,
    out: PrintWriter? = null,
    err: PrintWriter? = null,
): Int {
    val commandLine = CommandLine(ApkCategoryChecker()).setCaseInsensitiveEnumValuesAllowed(true)
    out?.let(commandLine::setOut)
    err?.let(commandLine::setErr)
    return commandLine.execute(*args)
}

fun main(args: Array<String>): Unit = exitProcess(run(args))
