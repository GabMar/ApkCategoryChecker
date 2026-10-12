// SPDX-License-Identifier: MIT
package com.apkcategorychecker.adb

import java.io.IOException
import java.nio.file.Path
import java.util.concurrent.TimeUnit
import kotlin.io.path.exists

class AdbException(
    message: String,
) : IOException(message)

/**
 * Runs the `adb` command line tool to list the apps installed on a device and pull their APKs.
 * Thread-safe: every call runs its own `adb` process.
 */
class Adb(
    private val executable: String = defaultExecutable(),
    /** The device to talk to (`adb -s`), or null for the only one connected. */
    val serial: String? = null,
) {
    /** Fails with a readable message if adb is missing or the device is not available. */
    fun checkDevice() {
        val state = run("get-state").trim()
        if (state != "device") throw AdbException("${serial ?: "the device"} is $state, not ready")
    }

    /** Third-party (user-installed) packages, sorted. */
    fun thirdPartyPackages(): List<String> = run("shell", "pm", "list", "packages", "-3").packageLines().sorted()

    /** Paths on the device of the APKs of [packageName]: the base APK and its splits. */
    fun apkPaths(packageName: String): List<String> {
        val output =
            try {
                run("shell", "pm", "path", packageName)
            } catch (e: AdbException) {
                // pm exits with an error when the package is unknown.
                throw AdbException("$packageName is not installed on the device")
            }
        return output.packageLines().ifEmpty { throw AdbException("$packageName is not installed on the device") }
    }

    fun pull(
        remote: String,
        local: Path,
    ) {
        run("pull", remote, local.toString(), timeoutMinutes = PULL_TIMEOUT_MINUTES)
        if (!local.exists()) throw AdbException("adb pull $remote produced no file")
    }

    private fun run(
        vararg args: String,
        timeoutMinutes: Long = COMMAND_TIMEOUT_MINUTES,
    ): String {
        val command = listOf(executable) + (serial?.let { listOf("-s", it) } ?: emptyList()) + args
        val process =
            try {
                ProcessBuilder(command).redirectErrorStream(false).start()
            } catch (e: IOException) {
                throw AdbException("cannot run $executable: install Android platform-tools or pass --adb-path (${e.message})")
            }
        // Read stderr on another thread, so that a full pipe can't block the process.
        val stderr = StringBuilder()
        val errorReader = Thread { stderr.append(process.errorStream.bufferedReader().readText()) }.apply { start() }
        val stdout = process.inputStream.bufferedReader().readText()
        if (!process.waitFor(timeoutMinutes, TimeUnit.MINUTES)) {
            process.destroyForcibly()
            throw AdbException("adb ${args.joinToString(" ")} timed out")
        }
        errorReader.join()
        if (process.exitValue() != 0) {
            val message = stderr.toString().trim().ifEmpty { stdout.trim() }
            throw AdbException("adb ${args.joinToString(" ")} failed${if (message.isEmpty()) "" else ": $message"}")
        }
        return stdout
    }

    /** `package:com.example\r\n` lines (pm separates them with \r\n on older Androids). */
    private fun String.packageLines(): List<String> =
        lines().map(String::trim).filter { it.startsWith("package:") }.map { it.removePrefix("package:") }

    companion object {
        private const val COMMAND_TIMEOUT_MINUTES = 2L
        private const val PULL_TIMEOUT_MINUTES = 15L

        /** `$ADB`, else adb from the Android SDK (`$ANDROID_HOME`, `$ANDROID_SDK_ROOT`), else adb on the PATH. */
        fun defaultExecutable(): String {
            System.getenv("ADB")?.takeIf { it.isNotBlank() }?.let { return it }
            for (sdk in listOf("ANDROID_HOME", "ANDROID_SDK_ROOT")) {
                val adb = System.getenv(sdk)?.let { Path.of(it, "platform-tools", "adb") }
                if (adb != null && adb.exists()) return adb.toString()
            }
            return "adb"
        }
    }
}
