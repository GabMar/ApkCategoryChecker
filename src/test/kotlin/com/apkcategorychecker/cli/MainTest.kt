// SPDX-License-Identifier: MIT
package com.apkcategorychecker.cli

import com.apkcategorychecker.apk.syntheticApk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.readText
import kotlin.io.path.writeText

class MainTest {
    @TempDir
    lateinit var dir: Path

    private class Result(
        val exitCode: Int,
        val out: String,
        val err: String,
    )

    private fun cli(vararg args: String): Result {
        val out = StringWriter()
        val err = StringWriter()
        val exitCode = run(arrayOf(*args), PrintWriter(out, true), PrintWriter(err, true))
        return Result(exitCode, out.toString(), err.toString())
    }

    private fun flutterApk(name: String = "app.apk") = syntheticApk(dir.resolve(name), mapOf("lib/arm64-v8a/libflutter.so" to ""))

    @Test
    fun `writes csv to standard output by default`() {
        val result = cli(flutterApk().toString())
        assertEquals(0, result.exitCode)
        val lines = result.out.lines().filter { it.isNotEmpty() }
        assertEquals(2, lines.size)
        assertTrue(lines[0].startsWith("file,path,splits,file_size,package,main_framework,"))
        assertTrue(",Flutter," in lines[1])
        assertTrue("Analyzed 1 app(s), 0 failed." in result.err)
    }

    @Test
    fun `picks the format from the option or the output file extension`() {
        val apk = flutterApk().toString()
        assertTrue(cli(apk, "--format", "jsonl").out.startsWith("{\"file\":\"app.apk\""))
        assertTrue(cli(apk, "-f", "JSON").out.startsWith("[\n  {\n"))

        val output = dir.resolve("out/results.json")
        output.parent.createDirectories()
        assertEquals(0, cli(apk, "-o", output.toString()).exitCode)
        assertTrue(output.readText().trimStart().startsWith("["))
    }

    @Test
    fun `analyzes directories recursively and skips other files`() {
        flutterApk("b.apk")
        dir.resolve("nested").createDirectories()
        flutterApk("nested/a.APK")
        dir.resolve("notes.txt").writeText("not an apk")

        val lines = cli(dir.toString(), "-f", "jsonl").out.lines().filter { it.isNotEmpty() }
        assertEquals(2, lines.size)
        assertTrue("\"file\":\"b.apk\"" in lines[0])
        assertTrue("\"file\":\"a.APK\"" in lines[1])
    }

    @Test
    fun `reports apks that cannot be analyzed without failing`() {
        val broken = dir.resolve("broken.apk").apply { writeText("not a zip") }
        val result = cli(broken.toString(), "-f", "jsonl")
        assertEquals(0, result.exitCode)
        assertTrue("\"error\":" in result.out)
        assertTrue("Could not analyze" in result.err)
        assertTrue("Analyzed 1 app(s), 1 failed." in result.err)
    }

    @Test
    fun `rejects invalid arguments`() {
        assertEquals(2, cli().exitCode)
        assertEquals(2, cli(dir.resolve("missing.apk").toString()).exitCode)
        assertEquals(2, cli(dir.toString(), "--threads", "0").exitCode)
        assertEquals(2, cli(dir.toString(), "--format", "xml").exitCode)
    }

    @Test
    fun `finds sdks unless told not to`() {
        val apk = flutterApk().toString()
        assertTrue("\"sdks\":[{\"name\":\"Flutter\"" in cli(apk, "-f", "jsonl").out)
        assertTrue("\"sdks\":[]" in cli(apk, "-f", "jsonl", "--no-sdks").out)
    }

    @Test
    fun `prints help and version`() {
        val help = cli("--help")
        assertEquals(0, help.exitCode)
        assertTrue("--web-app-threshold" in help.out)
        assertTrue(cli("--version").out.startsWith("apk-category-checker "))
    }
}
