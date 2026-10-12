// SPDX-License-Identifier: MIT
package com.apkcategorychecker.output

import com.apkcategorychecker.analyzer.ApkReport
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.json.Json
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.Closeable
import java.io.Writer

/** Writes reports one at a time, so that results are streamed while the analysis goes on. */
interface ReportWriter : Closeable {
    fun write(report: ApkReport)
}

enum class OutputFormat(
    val extension: String,
) {
    CSV("csv"),
    JSON("json"),
    JSONL("jsonl"),
    ;

    fun writer(out: Writer): ReportWriter =
        when (this) {
            CSV -> CsvReportWriter(out)
            JSON -> JsonReportWriter(out)
            JSONL -> JsonLinesReportWriter(out)
        }

    companion object {
        fun fromExtension(fileName: String): OutputFormat? =
            entries.firstOrNull { fileName.endsWith(".${it.extension}", ignoreCase = true) }
    }
}

/** RFC 4180 CSV with a header row; list values are separated by "; ". */
class CsvReportWriter(
    out: Writer,
) : ReportWriter {
    private val printer =
        CSVPrinter(
            out,
            CSVFormat.RFC4180
                .builder()
                .setHeader(*HEADER)
                .get(),
        )

    override fun write(report: ApkReport) =
        with(report) {
            printer.printRecord(
                file,
                path,
                splits.joinToString(LIST_SEPARATOR),
                fileSize,
                packageName,
                mainFramework,
                baseFramework,
                evidence.entries.joinToString(" | ") { (framework, found) -> "$framework: ${found.joinToString(", ")}" },
                details.entries.joinToString(" | ") { (framework, facts) ->
                    "$framework: ${facts.entries.joinToString(", ") { (key, value) -> "$key=$value" }}"
                },
                htmlFiles,
                jsFiles,
                cssFiles,
                jsFileNames.joinToString(LIST_SEPARATOR),
                debuggable,
                permissions.joinToString(LIST_SEPARATOR),
                minSdkVersion,
                targetSdkVersion,
                maxSdkVersion,
                analysisStart,
                analysisDurationMs,
                success,
                error,
            )
        }

    override fun close() = printer.close()

    private companion object {
        const val LIST_SEPARATOR = "; "
        val HEADER =
            arrayOf(
                "file",
                "path",
                "splits",
                "file_size",
                "package",
                "main_framework",
                "base_framework",
                "evidence",
                "details",
                "html_files",
                "js_files",
                "css_files",
                "js_file_names",
                "debuggable",
                "permissions",
                "min_sdk_version",
                "target_sdk_version",
                "max_sdk_version",
                "analysis_start",
                "analysis_duration_ms",
                "success",
                "error",
            )
    }
}

/** Every report has all the fields, defaults and nulls included. */
private val compactJson = Json { encodeDefaults = true }

@OptIn(ExperimentalSerializationApi::class)
private val prettyJson =
    Json(compactJson) {
        prettyPrint = true
        prettyPrintIndent = "  "
    }

/** A JSON array of reports. */
class JsonReportWriter(
    private val out: Writer,
) : ReportWriter {
    private var first = true

    override fun write(report: ApkReport) {
        out.write(if (first) "[\n" else ",\n")
        out.write(prettyJson.encodeToString(report).prependIndent("  "))
        first = false
    }

    override fun close() {
        out.write(if (first) "[]\n" else "\n]\n")
        out.close()
    }
}

/** One JSON object per line (https://jsonlines.org), convenient for large batches. */
class JsonLinesReportWriter(
    private val out: Writer,
) : ReportWriter {
    override fun write(report: ApkReport) {
        out.write(compactJson.encodeToString(report))
        out.write("\n")
    }

    override fun close() = out.close()
}
