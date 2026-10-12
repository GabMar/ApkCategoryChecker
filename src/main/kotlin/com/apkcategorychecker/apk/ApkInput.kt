// SPDX-License-Identifier: MIT
package com.apkcategorychecker.apk

import java.nio.file.Files
import java.nio.file.Path
import java.util.zip.ZipException
import java.util.zip.ZipFile
import kotlin.io.path.deleteRecursively
import kotlin.io.path.extension
import kotlin.io.path.fileSize
import kotlin.io.path.isDirectory
import kotlin.io.path.isRegularFile
import kotlin.io.path.name
import kotlin.io.path.walk

/** An app to analyze: a single APK, the split APKs of an App Bundle, or a container of APKs. */
sealed interface ApkInput {
    /** The file the app is reported under: the APK, the base APK or the container. */
    val path: Path

    /** Total size of the app's files in bytes. */
    fun size(): Long

    fun open(): ApkContents

    data class Single(
        override val path: Path,
    ) : ApkInput {
        override fun size() = path.fileSize()

        override fun open() = ApkFile(path)
    }

    /** A base APK and its splits, as pulled from a device (`base.apk`, `split_config.arm64_v8a.apk`, ...). */
    data class Split(
        override val path: Path,
        val splits: List<Path>,
    ) : ApkInput {
        override fun size() = path.fileSize() + splits.sumOf { it.fileSize() }

        override fun open(): ApkContents {
            val parts = mutableListOf<Pair<String, ApkFile>>()
            try {
                val base = ApkFile(path)
                parts += path.name to base
                splits.forEach { parts += it.name to ApkFile(it) }
                return SplitApk(base, parts.drop(1))
            } catch (e: Exception) {
                parts.forEach { runCatching { it.second.close() } }
                throw e
            }
        }
    }

    /**
     * A zip of APKs: XAPK (APKPure), APKM (APKMirror) or APKS (bundletool). The APKs are
     * extracted to a temporary directory, deleted when the returned contents are closed.
     */
    data class Container(
        override val path: Path,
    ) : ApkInput {
        override fun size() = path.fileSize()

        @OptIn(kotlin.io.path.ExperimentalPathApi::class)
        override fun open(): ApkContents {
            val tempDir = Files.createTempDirectory("apk-category-checker-")
            val opened = mutableListOf<Pair<String, ApkFile>>()
            try {
                extractApks(tempDir).forEach { (name, file) -> opened += name to ApkFile(file) }
                if (opened.isEmpty()) throw ApkFormatException("no APK inside $path")
                val (bases, splits) = opened.partition { it.second.manifest.split == null }
                // Prefer the conventional base names when a container also holds standalone APKs.
                val base =
                    bases.firstOrNull { it.first.substringAfterLast('/') in BASE_NAMES }
                        ?: bases.firstOrNull()
                        ?: throw ApkFormatException("no base APK inside $path")
                (bases - base).forEach { it.second.close() }
                return SplitApk(base.second, splits) { tempDir.deleteRecursively() }
            } catch (e: Exception) {
                opened.forEach { runCatching { it.second.close() } }
                tempDir.deleteRecursively()
                throw e
            }
        }

        /** Extracts the APKs to [dir], named by index: entry names are never used as paths (zip slip). */
        private fun extractApks(dir: Path): List<Pair<String, Path>> =
            try {
                ZipFile(path.toFile()).use { zip ->
                    val apks = zip.entries().toList().filter { !it.isDirectory && it.name.endsWith(".apk", ignoreCase = true) }
                    // bundletool's .apks holds both splits and standalone APKs for old devices.
                    val chosen = apks.filter { it.name.startsWith("splits/") }.ifEmpty { apks }
                    chosen.mapIndexed { i, entry ->
                        val file = dir.resolve("$i.apk")
                        zip.getInputStream(entry).use { Files.copy(it, file) }
                        entry.name to file
                    }
                }
            } catch (e: ZipException) {
                throw ApkFormatException("not a valid zip archive: $path", e)
            }

        private companion object {
            val BASE_NAMES = setOf("base.apk", "base-master.apk")
        }
    }

    companion object {
        val CONTAINER_EXTENSIONS = setOf("xapk", "apks", "apkm")

        fun of(path: Path): ApkInput = if (path.extension.lowercase() in CONTAINER_EXTENSIONS) Container(path) else Single(path)

        /**
         * The apps in [paths]: files as given, plus the APKs and containers found recursively in
         * directories. Split APKs of the same app (same package and version code) in the same
         * directory are grouped with their base APK. Sorted by path.
         */
        @OptIn(kotlin.io.path.ExperimentalPathApi::class)
        fun find(paths: List<Path>): List<ApkInput> {
            val files =
                paths
                    .flatMap { path ->
                        if (path.isDirectory()) {
                            path.walk().filter { it.isRegularFile() && it.extension.lowercase() in EXTENSIONS }.toList()
                        } else {
                            listOf(path)
                        }
                    }.distinct()
            val (containers, apks) = files.partition { it.extension.lowercase() in CONTAINER_EXTENSIONS }
            val grouped = apks.groupBy { it.toAbsolutePath().parent }.values.flatMap(::groupSplits)
            return (grouped + containers.map(::Container)).sortedBy { it.path }
        }

        private val EXTENSIONS = CONTAINER_EXTENSIONS + "apk"

        private data class SplitInfo(
            val packageName: String?,
            val versionCode: String?,
            val split: String?,
        )

        private fun groupSplits(apks: List<Path>): List<ApkInput> {
            if (apks.size < 2) return apks.map(::Single)
            // Unreadable APKs get no info: they are analyzed (and reported as failures) on their own.
            val infos =
                apks.associateWith { apk ->
                    runCatching { ApkFile(apk).use { SplitInfo(it.manifest.packageName, it.manifest.versionCode, it.manifest.split) } }
                        .getOrNull()
                }
            val grouped = mutableSetOf<Path>()
            val inputs = mutableListOf<ApkInput>()
            for ((apk, info) in infos) {
                if (info == null || info.split != null) continue
                val splits =
                    infos
                        .filter { (other, i) ->
                            other !in grouped && i?.split != null && i.packageName == info.packageName && i.versionCode == info.versionCode
                        }.keys
                        .sorted()
                grouped.addAll(splits)
                grouped.add(apk)
                inputs += if (splits.isEmpty()) Single(apk) else Split(apk, splits)
            }
            return inputs + apks.filter { it !in grouped }.map(::Single)
        }
    }
}
