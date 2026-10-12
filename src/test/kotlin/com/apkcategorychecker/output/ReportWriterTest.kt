package com.apkcategorychecker.output

import com.apkcategorychecker.analyzer.ApkReport
import kotlinx.serialization.json.Json
import org.apache.commons.csv.CSVFormat
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.StringWriter

class ReportWriterTest {

    private val report = ApkReport(
        file = "a, \"quoted\".apk",
        path = "/tmp/a, \"quoted\".apk",
        fileSize = 42,
        packageName = "com.example",
        mainFramework = "Ionic",
        baseFramework = "Capacitor",
        evidence = mapOf("Capacitor" to listOf("class com.getcapacitor.Bridge"), "Ionic" to listOf("a", "b")),
        permissions = listOf("android.permission.INTERNET", "android.permission.CAMERA"),
        analysisStart = "2026-10-12T10:00:00Z",
        analysisDurationMs = 5,
    )
    private val failed = report.copy(file = "b.apk", mainFramework = null, baseFramework = null, error = "not a zip")

    private fun write(format: OutputFormat, vararg reports: ApkReport): String {
        val out = StringWriter()
        format.writer(out).use { writer -> reports.forEach(writer::write) }
        return out.toString()
    }

    @Test
    fun `csv is quoted per RFC 4180 and lists are joined`() {
        val csv = write(OutputFormat.CSV, report, failed)
        val rows = CSVFormat.RFC4180.builder().setHeader().get().parse(csv.reader()).records
        assertEquals(2, rows.size)
        assertEquals(report.file, rows[0]["file"])
        assertEquals("android.permission.INTERNET; android.permission.CAMERA", rows[0]["permissions"])
        assertEquals("Capacitor: class com.getcapacitor.Bridge | Ionic: a, b", rows[0]["evidence"])
        assertEquals("true", rows[0]["success"])
        assertEquals("", rows[0]["error"])
        assertEquals("false", rows[1]["success"])
        assertEquals("not a zip", rows[1]["error"])
        assertEquals("", rows[1]["main_framework"])
    }

    @Test
    fun `json is an array that round-trips`() {
        assertEquals(listOf(report, failed), Json.decodeFromString<List<ApkReport>>(write(OutputFormat.JSON, report, failed)))
        assertEquals("[]\n", write(OutputFormat.JSON))
    }

    @Test
    fun `json lines has one object per line`() {
        val lines = write(OutputFormat.JSONL, report, failed).lines().filter { it.isNotEmpty() }
        assertEquals(listOf(report, failed), lines.map { Json.decodeFromString<ApkReport>(it) })
    }

    @Test
    fun `json includes default and null fields`() {
        val line = write(OutputFormat.JSONL, report)
        for (field in listOf("\"htmlFiles\":0", "\"jsFileNames\":[]", "\"maxSdkVersion\":null", "\"error\":null")) {
            assertTrue(field in line, field)
        }
    }

    @Test
    fun `format from file extension`() {
        assertEquals(OutputFormat.JSON, OutputFormat.fromExtension("out/results.JSON"))
        assertEquals(OutputFormat.JSONL, OutputFormat.fromExtension("results.jsonl"))
        assertEquals(OutputFormat.CSV, OutputFormat.fromExtension("results.csv"))
        assertEquals(null, OutputFormat.fromExtension("results.txt"))
    }
}
