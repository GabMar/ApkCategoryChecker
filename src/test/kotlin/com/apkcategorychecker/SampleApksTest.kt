package com.apkcategorychecker

import com.apkcategorychecker.analyzer.PathAnalyzer
import com.apkcategorychecker.cli.CommandLineInterface
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVRecord
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.TestInstance
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readLines
import kotlin.io.path.reader

/**
 * Characterization tests: run the whole pipeline on the sample APKs and pin the
 * results, so the refactoring in the next phases can't silently change them.
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class SampleApksTest {

    private lateinit var results: Map<String, CSVRecord>
    private lateinit var jsFiles: Map<String, String>
    private lateinit var resultLines: List<String>

    @BeforeAll
    fun analyzeSampleApks(@TempDir outDir: Path) {
        val apks = File(javaClass.getResource("/apks")!!.toURI())
        val decodedDir = outDir.resolve("decoded")

        CommandLineInterface.getInstance().Initialize(
            arrayOf("-p", apks.path, "-csv", outDir.toString(), "-k", decodedDir.toString())
        )
        PathAnalyzer().Analyze()

        val resultsCsv = outDir.listDirectoryEntries("Results_*.csv").single()
        resultLines = resultsCsv.readLines()
        results = parse(resultsCsv).associateBy { it["APK_File_Name"] }
        jsFiles = parse(outDir.listDirectoryEntries("Results-JS FILES_*.csv").single())
            .associate { it["appID"] to it["jsFiles"] }
    }

    @Test
    fun `results file has a header and one line per APK`() {
        assertEquals(4, resultLines.size)
        assert(resultLines.first().startsWith("App_ID#APK_File_Name#"))
        assertEquals(setOf("HelloWorld.apk", "SenToCo.apk", "TodoList.apk"), results.keys)
    }

    @Test
    fun `cordova app`() {
        assertResult(
            results.getValue("HelloWorld.apk"),
            pkg = "com.example.hello",
            mainFramework = "Apache Cordova",
            baseFramework = "",
            html = 1, js = 3, css = 1,
            debuggable = "true",
            permissions = "android.permission.INTERNET",
            minSdk = "10", targetSdk = "19",
            fileSize = 1915225,
        )
        assertEquals("cordova.js, index.js, cordova_plugins.js", jsFiles["com.example.hello"])
    }

    @Test
    fun `sencha app on top of cordova`() {
        assertResult(
            results.getValue("SenToCo.apk"),
            pkg = "com.mydomain.sentoco",
            mainFramework = "Sencha",
            baseFramework = "Apache Cordova",
            html = 1, js = 5, css = 2,
            debuggable = "true",
            permissions = "android.permission.INTERNET",
            minSdk = "10", targetSdk = "17",
            fileSize = 716499,
        )
        assertEquals(
            "cordova.js, index.js, app.json, app.js, cordova_plugins.js",
            jsFiles["com.mydomain.sentoco"],
        )
    }

    @Test
    fun `titanium app`() {
        assertResult(
            results.getValue("TodoList.apk"),
            pkg = "com.appcelerator.sample.todo",
            mainFramework = "Titanium",
            baseFramework = "",
            html = 0, js = 13, css = 0,
            debuggable = "false",
            permissions = listOf(
                "INTERNET", "ACCESS_WIFI_STATE", "ACCESS_NETWORK_STATE", "WRITE_EXTERNAL_STORAGE",
                "ACCESS_COARSE_LOCATION", "ACCESS_FINE_LOCATION", "ACCESS_MOCK_LOCATION",
            ).joinToString(", ") { "android.permission.$it" },
            minSdk = "10", targetSdk = "19",
            fileSize = 8202108,
        )
        assertEquals(
            listOf(
                "json2.js", "polling.js", "binding.min.js", "binding.js", "polling.min.js",
                "_app_props_.json", "db.js", "ListWindow.js", "AppTabGroup.js", "AddWindow.js",
                "app.js", "index.json", "app.json",
            ).sorted(),
            jsFiles.getValue("com.appcelerator.sample.todo").split(", ").sorted(),
        )
    }

    private fun assertResult(
        row: CSVRecord,
        pkg: String,
        mainFramework: String,
        baseFramework: String,
        html: Int,
        js: Int,
        css: Int,
        debuggable: String,
        permissions: String,
        minSdk: String,
        targetSdk: String,
        fileSize: Long,
    ) {
        assertEquals(pkg, row["APK_Package"])
        assertEquals(mainFramework, row["Main_Framework"])
        assertEquals(baseFramework, row["Base_Framework"])
        assertEquals(html.toString(), row["HTML"])
        assertEquals(js.toString(), row["JS"])
        assertEquals(css.toString(), row["CSS"])
        assertEquals(debuggable, row["Android_Debuggable"])
        assertEquals(permissions, row["Android_Permissions"])
        assertEquals(minSdk, row["Android_MinSdkVersion"])
        assertEquals("UNDEFINED", row["Android_MaxSdkVersion"])
        assertEquals(targetSdk, row["Android_TargetSdkVersion"])
        assertEquals(fileSize.toString(), row["File_Size(Bytes)"])
        assertEquals("0", row["Decode_Success"])
    }

    private fun parse(csv: Path): List<CSVRecord> =
        csv.reader().use { CSVFormat.DEFAULT.withDelimiter('#').withHeader().parse(it).records }
}
