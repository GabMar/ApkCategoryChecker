// SPDX-License-Identifier: MIT
package com.apkcategorychecker.apk

import com.apkcategorychecker.analyzer.ApkAnalyzer
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Files
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.fileSize
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.writeText

class ApkInputTest {
    @TempDir
    lateinit var dir: Path

    /** A Flutter app split like Google Play installs it: the engine is only in the ABI split. */
    private fun flutterBase(file: Path) = syntheticApk(file, classes = listOf("io.flutter.embedding.engine.FlutterEngine"))

    private fun flutterAbiSplit(file: Path) =
        syntheticApk(file, mapOf("lib/arm64-v8a/libflutter.so" to ""), dex = null, split = "config.arm64_v8a")

    @Test
    fun `groups split apks with their base apk`() {
        val pulled = dir.resolve("pulled").createDirectories()
        val base = flutterBase(pulled.resolve("base.apk"))
        val split = flutterAbiSplit(pulled.resolve("split_config.arm64_v8a.apk"))
        val other = syntheticApk(pulled.resolve("other.apk"), packageName = "com.example.other")
        val orphan = syntheticApk(pulled.resolve("orphan.apk"), packageName = "com.example.gone", split = "config.xxhdpi")
        // Same package, other version: not a split of this base.
        val stale = syntheticApk(pulled.resolve("split_old.apk"), split = "config.en", versionCode = 2)

        assertEquals(
            listOf(ApkInput.Split(base, listOf(split)), ApkInput.Single(orphan), ApkInput.Single(other), ApkInput.Single(stale)),
            ApkInput.find(listOf(dir)),
        )
    }

    @Test
    fun `analyzes split apks as one app`() {
        val base = flutterBase(dir.resolve("base.apk"))
        val split = flutterAbiSplit(dir.resolve("split_config.arm64_v8a.apk"))

        val report = ApkAnalyzer().analyze(ApkInput.find(listOf(dir)).single())
        assertEquals("base.apk", report.file)
        assertEquals(listOf("split_config.arm64_v8a.apk"), report.splits)
        assertEquals("Flutter", report.mainFramework)
        assertEquals(
            listOf("lib/arm64-v8a/libflutter.so", "class io.flutter.embedding.engine.FlutterEngine"),
            report.evidence["Flutter"],
        )
        assertEquals(base.fileSize() + split.fileSize(), report.fileSize)
        assertEquals("com.example.app", report.packageName)
    }

    @Test
    fun `analyzes xapk containers and deletes the extracted apks`() {
        val xapk =
            container(
                dir.resolve("app.xapk"),
                mapOf(
                    "com.example.app.apk" to flutterBase(dir.resolve("b.apk")),
                    "config.arm64_v8a.apk" to flutterAbiSplit(dir.resolve("s.apk")),
                ),
                extraFiles = mapOf("manifest.json" to "{}", "icon.png" to ""),
            )
        val tempDirsBefore = extractionDirs()

        val report = ApkAnalyzer().analyze(xapk)
        assertNull(report.error)
        assertEquals("app.xapk", report.file)
        assertEquals(listOf("config.arm64_v8a.apk"), report.splits)
        assertEquals("Flutter", report.mainFramework)
        assertEquals(xapk.fileSize(), report.fileSize)
        assertEquals(tempDirsBefore, extractionDirs())
    }

    @Test
    fun `uses the splits of bundletool apks files and ignores standalone apks`() {
        val apks =
            container(
                dir.resolve("app.apks"),
                mapOf(
                    "splits/base-master.apk" to flutterBase(dir.resolve("b.apk")),
                    "splits/base-arm64_v8a.apk" to flutterAbiSplit(dir.resolve("s.apk")),
                    "standalones/standalone-arm64_v8a.apk" to syntheticApk(dir.resolve("st.apk"), packageName = "com.example.standalone"),
                ),
                extraFiles = mapOf("toc.pb" to ""),
            )

        val report = ApkAnalyzer().analyze(apks)
        assertEquals("com.example.app", report.packageName)
        assertEquals(listOf("splits/base-arm64_v8a.apk"), report.splits)
        assertEquals("Flutter", report.mainFramework)
    }

    @Test
    fun `reports containers without apks`() {
        val empty = container(dir.resolve("empty.apkm"), emptyMap(), extraFiles = mapOf("info.json" to "{}"))
        val broken = dir.resolve("broken.xapk").apply { writeText("not a zip") }
        val tempDirsBefore = extractionDirs()

        assertTrue(ApkAnalyzer().analyze(empty).error!!.startsWith("no APK inside"))
        assertFalse(ApkAnalyzer().analyze(broken).success)
        assertEquals(tempDirsBefore, extractionDirs())
    }

    @Test
    fun `finds containers in directories and as arguments`() {
        val nested = dir.resolve("nested").createDirectories()
        val xapk = container(nested.resolve("a.xapk"), mapOf("base.apk" to flutterBase(dir.resolve("b.apk"))))
        val apkm = container(dir.resolve("c.apkm"), mapOf("base.apk" to flutterBase(dir.resolve("d.apk"))))
        dir.resolve("notes.txt").writeText("not an app")

        val found = ApkInput.find(listOf(nested, apkm))
        assertEquals(listOf(ApkInput.Container(apkm), ApkInput.Container(xapk)), found)
    }

    private fun extractionDirs(): Set<String> =
        Path
            .of(System.getProperty("java.io.tmpdir"))
            .listDirectoryEntries("apk-category-checker-*")
            .filter { Files.isDirectory(it) }
            .map { it.name }
            .toSet()
}
