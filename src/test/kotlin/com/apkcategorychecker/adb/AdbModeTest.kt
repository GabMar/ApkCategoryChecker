// SPDX-License-Identifier: MIT
package com.apkcategorychecker.adb

import com.apkcategorychecker.analyzer.ApkReport
import com.apkcategorychecker.apk.syntheticApk
import com.apkcategorychecker.cli.run
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.condition.DisabledOnOs
import org.junit.jupiter.api.condition.OS
import org.junit.jupiter.api.io.TempDir
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions
import kotlin.io.path.copyTo
import kotlin.io.path.createDirectories
import kotlin.io.path.fileSize
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name
import kotlin.io.path.readText
import kotlin.io.path.writeText

/** The adb mode against a fake `adb` (a shell script) that answers like the real one. */
@DisabledOnOs(OS.WINDOWS)
class AdbModeTest {
    @TempDir
    lateinit var dir: Path

    private val device by lazy { FakeDevice(dir) }

    private class Result(
        val exitCode: Int,
        val reports: List<ApkReport>,
        val err: String,
    )

    private fun cli(vararg args: String): Result {
        val out = StringWriter()
        val err = StringWriter()
        val exitCode =
            run(arrayOf(*args, "--adb-path", device.adb.toString(), "-f", "jsonl"), PrintWriter(out, true), PrintWriter(err, true))
        val reports =
            out
                .toString()
                .lines()
                .filter { it.isNotBlank() }
                .map { Json.decodeFromString<ApkReport>(it) }
        return Result(exitCode, reports, err.toString())
    }

    private fun flutterApp(packageName: String) {
        val base =
            syntheticApk(
                dir.resolve("$packageName-base.apk"),
                classes = listOf("io.flutter.embedding.engine.FlutterJNI"),
                packageName = packageName,
            )
        val split =
            syntheticApk(
                dir.resolve("$packageName-split.apk"),
                mapOf("lib/arm64-v8a/libflutter.so" to ""),
                dex = null,
                packageName = packageName,
                split = "config.arm64_v8a",
            )
        device.install(packageName, mapOf("base.apk" to base, "split_config.arm64_v8a.apk" to split))
    }

    @Test
    fun `analyzes an installed app with its splits`() {
        flutterApp("com.example.flutter")
        val tempDirsBefore = extractionDirs()

        val result = cli("--adb", "com.example.flutter")
        assertEquals(0, result.exitCode, result.err)
        val report = result.reports.single()
        assertEquals("com.example.flutter", report.file)
        assertEquals("adb:device/com.example.flutter", report.path)
        assertEquals(listOf("split_config.arm64_v8a.apk"), report.splits)
        assertEquals("Flutter", report.mainFramework)
        assertEquals(listOf("lib/arm64-v8a/libflutter.so", "class io.flutter.embedding.engine.FlutterJNI"), report.evidence["Flutter"])
        assertEquals(device.installedSize("com.example.flutter"), report.fileSize)
        assertEquals(tempDirsBefore, extractionDirs())
    }

    @Test
    fun `analyzes all third-party apps of the chosen device`() {
        flutterApp("org.b.second")
        device.install("com.a.first", mapOf("base.apk" to syntheticApk(dir.resolve("first.apk"), packageName = "com.a.first")))
        device.serial = "emulator-5554"

        val result = cli("--adb-all", "-s", "emulator-5554")
        assertEquals(0, result.exitCode, result.err)
        assertEquals(listOf("com.a.first", "org.b.second"), result.reports.map { it.file })
        assertEquals(listOf("Native", "Flutter"), result.reports.map { it.mainFramework })
        assertTrue(device.calls().all { it.startsWith("-s emulator-5554 ") }, device.calls().toString())
        assertTrue("Analyzed 2 app(s), 0 failed." in result.err)
    }

    @Test
    fun `reports packages that are not installed`() {
        flutterApp("com.example.flutter")
        val result = cli("--adb", "com.example.flutter", "com.example.missing")
        assertEquals(0, result.exitCode)
        assertEquals("com.example.missing is not installed on the device", result.reports.single { it.file == "com.example.missing" }.error)
        assertTrue("Analyzed 2 app(s), 1 failed." in result.err)
    }

    @Test
    fun `rejects unusable devices and arguments`() {
        flutterApp("com.example.flutter")
        device.state = "offline"
        assertTrue("offline" in cli("--adb", "com.example.flutter").also { assertEquals(2, it.exitCode) }.err)
        device.state = "device"

        device.serial = "emulator-5554"
        assertTrue("not found" in cli("--adb-all", "-s", "other").also { assertEquals(2, it.exitCode) }.err)

        assertEquals(2, cli("--adb").exitCode) // no package names
        assertEquals(2, cli("--adb-all", "com.example.flutter").exitCode)

        val err = StringWriter()
        val missingAdb =
            run(arrayOf("--adb-all", "--adb-path", dir.resolve("nope").toString()), PrintWriter(StringWriter()), PrintWriter(err, true))
        assertEquals(2, missingAdb)
        assertTrue("cannot run" in err.toString(), err.toString())
        assertEquals(2, run(arrayOf("app.apk", "-s", "emulator-5554"), PrintWriter(StringWriter()), PrintWriter(StringWriter())))
    }

    private fun extractionDirs(): Set<String> =
        Path
            .of(System.getProperty("java.io.tmpdir"))
            .listDirectoryEntries("apk-category-checker-*")
            .map { it.name }
            .toSet()

    /** A fake device: installed packages with their APKs, and an `adb` script that serves them. */
    private class FakeDevice(
        dir: Path,
    ) {
        private val root = dir.resolve("device").createDirectories()
        private val log = dir.resolve("adb.log")

        var state: String
            get() = root.resolve("state").readText()
            set(value) = root.resolve("state").writeText(value)

        var serial: String? = null
            set(value) {
                field = value
                root.resolve("serial").writeText(value!!)
            }

        val adb: Path =
            dir.resolve("adb").also { script ->
                script.writeText(
                    """
                    #!/bin/sh
                    ROOT='$root'
                    echo "$*" >> '$log'
                    if [ "$1" = "-s" ]; then
                      if [ -f "${'$'}ROOT/serial" ] && [ "$2" != "$(cat "${'$'}ROOT/serial")" ]; then echo "error: device '$2' not found" >&2; exit 1; fi
                      shift 2
                    fi
                    case "$1" in
                      get-state)
                        state=$(cat "${'$'}ROOT/state")
                        if [ "${'$'}state" != device ]; then echo "error: device ${'$'}state" >&2; exit 1; fi
                        echo device ;;
                      shell)
                        if [ "$2 $3" = "pm path" ]; then
                          [ -f "${'$'}ROOT/packages/$4" ] || exit 1
                          sed 's/^/package:/' "${'$'}ROOT/packages/$4"
                        elif [ "$2 $3 $4 $5" = "pm list packages -3" ]; then
                          for p in "${'$'}ROOT"/packages/*; do [ -e "${'$'}p" ] && printf 'package:%s\r\n' "$(basename "${'$'}p")"; done
                        fi ;;
                      pull)
                        cp "${'$'}ROOT/fs$2" "$3" || { echo "adb: error: failed to stat remote object '$2'" >&2; exit 1; }
                        echo "$2: 1 file pulled, 0 skipped." ;;
                      *) echo "unknown command $1" >&2; exit 1 ;;
                    esac
                    """.trimIndent() + "\n",
                )
                Files.setPosixFilePermissions(script, PosixFilePermissions.fromString("rwxr-xr-x"))
                root.resolve("packages").createDirectories()
                state = "device"
            }

        /** Installs the APKs (name -> local file) of a package under /data/app, as Android does. */
        fun install(
            packageName: String,
            apks: Map<String, Path>,
        ) {
            val remoteDir = "/data/app/~~a1b2==/$packageName-c3d4==/"
            val remote =
                apks.map { (name, file) ->
                    (remoteDir + name).also { file.copyTo(root.resolve("fs$it").also { f -> f.parent.createDirectories() }) }
                }
            root.resolve("packages/$packageName").writeText(remote.joinToString("\n") + "\n")
        }

        fun installedSize(packageName: String): Long =
            root
                .resolve("packages/$packageName")
                .readText()
                .lines()
                .filter { it.isNotBlank() }
                .sumOf { root.resolve("fs$it").fileSize() }

        fun calls(): List<String> = log.readText().lines().filter { it.isNotBlank() }
    }
}
