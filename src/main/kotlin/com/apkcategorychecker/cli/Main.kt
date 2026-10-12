// SPDX-License-Identifier: MIT
package com.apkcategorychecker.cli

import com.apkcategorychecker.adb.Adb
import com.apkcategorychecker.adb.AdbException
import com.apkcategorychecker.analyzer.ApkAnalyzer
import com.apkcategorychecker.analyzer.BatchAnalyzer
import com.apkcategorychecker.apk.ApkInput
import com.apkcategorychecker.framework.Rules
import com.apkcategorychecker.framework.RulesException
import com.apkcategorychecker.output.OutputFormat
import com.apkcategorychecker.sdk.SdkRules
import picocli.CommandLine
import picocli.CommandLine.Command
import picocli.CommandLine.Option
import picocli.CommandLine.Parameters
import java.io.FilterWriter
import java.io.IOException
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
        arity = "0..*",
        paramLabel = "PATH",
        description = [
            "APK files, APK containers (.xapk, .apks, .apkm) or directories searched recursively for them.",
            "Split APKs of the same app in one directory are analyzed together.",
            "With --adb: package names of apps installed on the device.",
        ],
    )
    var paths: List<String> = emptyList()

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

    @Option(
        names = ["--rules"],
        paramLabel = "FILE",
        description = ["Use the framework rules in FILE instead of the built-in ones (see --print-rules)."],
    )
    var rules: Path? = null

    @Option(names = ["--adb"], description = ["Analyze the apps installed on a device: PATH arguments are package names."])
    var adb: Boolean = false

    @Option(names = ["--adb-all"], description = ["Analyze all the third-party apps installed on a device."])
    var adbAll: Boolean = false

    @Option(names = ["-s", "--serial"], paramLabel = "SERIAL", description = ["The device to use with --adb, if several are connected."])
    var serial: String? = null

    @Option(
        names = ["--adb-path"],
        paramLabel = "FILE",
        description = ["The adb executable. Default: \$ADB, the Android SDK's, or adb on the PATH."],
    )
    var adbPath: String? = null

    @Option(names = ["--no-sdks"], description = ["Skip the detection of SDKs and libraries (faster)."])
    var noSdks: Boolean = false

    @Option(names = ["--print-rules"], description = ["Print the built-in framework rules, a starting point for --rules, and exit."])
    var printRules: Boolean = false

    @CommandLine.Spec
    lateinit var spec: CommandLine.Model.CommandSpec

    private fun fail(message: String): Nothing = throw CommandLine.ParameterException(spec.commandLine(), message)

    private fun fileApps(): List<ApkInput> {
        if (serial != null || adbPath != null) fail("--serial and --adb-path need --adb or --adb-all")
        if (paths.isEmpty()) fail("Missing required parameter: 'PATH'")
        val files = paths.map { Path.of(it) }
        files.filterNot { it.exists() }.takeIf { it.isNotEmpty() }?.let { fail("Path not found: ${it.joinToString()}") }
        return ApkInput.find(files)
    }

    private fun deviceApps(): List<ApkInput> {
        if (adbAll && paths.isNotEmpty()) fail("--adb-all analyzes all the apps: it takes no package names")
        if (!adbAll && paths.isEmpty()) fail("Missing package names to analyze with --adb (or use --adb-all)")
        val device = Adb(adbPath ?: Adb.defaultExecutable(), serial)
        return try {
            device.checkDevice()
            val packages = if (adbAll) device.thirdPartyPackages() else paths.distinct()
            packages.map { ApkInput.Device(it, device) }
        } catch (e: AdbException) {
            fail(e.message!!)
        }
    }

    override fun call(): Int {
        val err = spec.commandLine().err
        if (printRules) {
            spec.commandLine().out.print(Rules.builtInText)
            spec.commandLine().out.flush()
            return 0
        }
        if (threads < 1) throw CommandLine.ParameterException(spec.commandLine(), "--threads must be at least 1")
        val rules =
            try {
                rules?.let(Rules::load) ?: Rules.builtIn
            } catch (e: RulesException) {
                throw CommandLine.ParameterException(spec.commandLine(), e.message)
            } catch (e: IOException) {
                throw CommandLine.ParameterException(spec.commandLine(), "Cannot read rules: ${e.message}")
            }

        val apps = if (adb || adbAll) deviceApps() else fileApps()
        val outputFormat = format ?: output?.let { OutputFormat.fromExtension(it.fileName.toString()) } ?: OutputFormat.CSV
        val out = output?.bufferedWriter() ?: NonClosingWriter(spec.commandLine().out)

        var failures = 0
        outputFormat.writer(out).use { writer ->
            BatchAnalyzer(ApkAnalyzer(rules, webAppThreshold, if (noSdks) null else SdkRules.builtIn), threads).analyze(apps) { report ->
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
