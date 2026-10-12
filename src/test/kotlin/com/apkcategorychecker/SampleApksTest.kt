// SPDX-License-Identifier: MIT
package com.apkcategorychecker

import com.apkcategorychecker.analyzer.ApkReport
import com.apkcategorychecker.cli.run
import kotlinx.serialization.json.Json
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.io.path.readText
import kotlin.io.path.reader

/**
 * End-to-end tests: run the command line on the sample APKs and pin the results.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SampleApksTest {
    private lateinit var csvRows: List<CSVRecord>
    private lateinit var reports: Map<String, ApkReport>

    @BeforeAll
    fun analyzeSampleApks(
        @TempDir outDir: Path,
    ) {
        val apks = File(javaClass.getResource("/apks")!!.toURI()).path
        val csv = outDir.resolve("results.csv")
        val json = outDir.resolve("results.json")
        val err = StringWriter()

        assertEquals(0, run(arrayOf(apks, "-o", csv.toString()), err = PrintWriter(err)))
        assertEquals(0, run(arrayOf(apks, "--output", json.toString(), "--threads", "2"), err = PrintWriter(err)))

        csvRows =
            csv.reader().use {
                CSVFormat.RFC4180
                    .builder()
                    .setHeader()
                    .get()
                    .parse(it)
                    .records
            }
        reports = Json.decodeFromString<List<ApkReport>>(json.readText()).associateBy { it.file }
    }

    @Test
    fun `csv and json have one entry per APK, in path order`() {
        assertEquals(listOf("HelloWorld.apk", "SenToCo.apk", "TodoList.apk"), csvRows.map { it["file"] })
        assertEquals(listOf("HelloWorld.apk", "SenToCo.apk", "TodoList.apk"), reports.keys.toList())
    }

    @Test
    fun `cordova app`() {
        val report = reports.getValue("HelloWorld.apk")
        assertEquals(
            expected(
                report,
                packageName = "com.example.hello",
                mainFramework = "Apache Cordova",
                baseFramework = null,
                evidence = mapOf("Apache Cordova" to CORDOVA_EVIDENCE),
                htmlFiles = 1,
                jsFiles = 3,
                cssFiles = 1,
                jsFileNames = listOf("index.js", "cordova.js", "cordova_plugins.js"),
                debuggable = "true",
                permissions = listOf("android.permission.INTERNET"),
                minSdkVersion = "10",
                targetSdkVersion = "19",
                fileSize = 1915225,
            ),
            report,
        )
        assertCsvMatches(report)
    }

    @Test
    fun `sencha app on top of cordova`() {
        val report = reports.getValue("SenToCo.apk")
        assertEquals(
            expected(
                report,
                packageName = "com.mydomain.sentoco",
                mainFramework = "Sencha",
                baseFramework = "Apache Cordova",
                evidence =
                    mapOf(
                        "Apache Cordova" to CORDOVA_EVIDENCE,
                        "Sencha" to
                            listOf(
                                "assets/www/app.js contains \"Ext.create\"",
                                "assets/www/app.js contains \"Ext.application\"",
                            ),
                    ),
                htmlFiles = 1,
                jsFiles = 4,
                cssFiles = 2,
                jsFileNames = listOf("index.js", "app.js", "cordova.js", "cordova_plugins.js"),
                debuggable = "true",
                permissions = listOf("android.permission.INTERNET"),
                minSdkVersion = "10",
                targetSdkVersion = "17",
                fileSize = 716499,
            ),
            report,
        )
        assertCsvMatches(report)
    }

    @Test
    fun `titanium app`() {
        val report = reports.getValue("TodoList.apk")
        assertEquals(
            expected(
                report,
                packageName = "com.appcelerator.sample.todo",
                mainFramework = "Titanium",
                baseFramework = null,
                evidence =
                    mapOf(
                        "Titanium" to listOf("class ti.modules.titanium.TitaniumModule", "class org.appcelerator.titanium.TiActivity"),
                    ),
                htmlFiles = 0,
                jsFiles = 10,
                cssFiles = 0,
                jsFileNames =
                    listOf(
                        "AddWindow.js",
                        "AppTabGroup.js",
                        "ListWindow.js",
                        "app.js",
                        "db.js",
                        "binding.js",
                        "binding.min.js",
                        "json2.js",
                        "polling.js",
                        "polling.min.js",
                    ),
                debuggable = "false",
                permissions =
                    listOf(
                        "INTERNET",
                        "ACCESS_WIFI_STATE",
                        "ACCESS_NETWORK_STATE",
                        "WRITE_EXTERNAL_STORAGE",
                        "ACCESS_COARSE_LOCATION",
                        "ACCESS_FINE_LOCATION",
                        "ACCESS_MOCK_LOCATION",
                    ).map { "android.permission.$it" },
                minSdkVersion = "10",
                targetSdkVersion = "19",
                fileSize = 8202108,
            ),
            report,
        )
        assertCsvMatches(report)
    }

    /** The expected report, taking the fields that change at every run (path, timing) from [actual]. */
    private fun expected(
        actual: ApkReport,
        packageName: String,
        mainFramework: String,
        baseFramework: String?,
        evidence: Map<String, List<String>>,
        htmlFiles: Int,
        jsFiles: Int,
        cssFiles: Int,
        jsFileNames: List<String>,
        debuggable: String,
        permissions: List<String>,
        minSdkVersion: String,
        targetSdkVersion: String,
        fileSize: Long,
    ) = ApkReport(
        file = actual.file,
        path = actual.path,
        fileSize = fileSize,
        packageName = packageName,
        mainFramework = mainFramework,
        baseFramework = baseFramework,
        evidence = evidence,
        htmlFiles = htmlFiles,
        jsFiles = jsFiles,
        cssFiles = cssFiles,
        jsFileNames = jsFileNames,
        debuggable = debuggable,
        permissions = permissions,
        minSdkVersion = minSdkVersion,
        targetSdkVersion = targetSdkVersion,
        maxSdkVersion = null,
        analysisStart = actual.analysisStart,
        analysisDurationMs = actual.analysisDurationMs,
    )

    /** The CSV row carries the same values as the JSON report. */
    private fun assertCsvMatches(report: ApkReport) {
        val row = csvRows.single { it["file"] == report.file }
        assertEquals(report.packageName, row["package"])
        assertEquals(report.mainFramework, row["main_framework"])
        assertEquals(report.baseFramework.orEmpty(), row["base_framework"])
        assertEquals(report.jsFiles.toString(), row["js_files"])
        assertEquals(report.jsFileNames.joinToString("; "), row["js_file_names"])
        assertEquals(report.permissions.joinToString("; "), row["permissions"])
        assertEquals(report.minSdkVersion, row["min_sdk_version"])
        assertEquals("", row["max_sdk_version"])
        assertEquals(report.fileSize.toString(), row["file_size"])
        assertEquals("true", row["success"])
        assertEquals(
            report.evidence.entries.joinToString(" | ") { (framework, found) -> "$framework: ${found.joinToString(", ")}" },
            row["evidence"],
        )
    }

    private companion object {
        val CORDOVA_EVIDENCE =
            listOf(
                "class org.apache.cordova.CordovaActivity",
                "res/xml/config.xml contains \"org.apache.cordova\"",
            )
    }
}
