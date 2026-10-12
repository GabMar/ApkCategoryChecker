// SPDX-License-Identifier: MIT
package com.apkcategorychecker.corpus

import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipFile
import java.util.zip.ZipOutputStream
import kotlin.io.path.name
import kotlin.io.path.outputStream

/**
 * Splits a universal APK the way Google Play installs an App Bundle: `base.apk` with everything
 * but the native libraries, plus a `split_config.<abi>.apk` per ABI with that ABI's libraries.
 */
internal fun splitLikePlay(
    apk: Path,
    dir: Path,
): List<Path> =
    ZipFile(apk.toFile()).use { zip ->
        val entries = zip.entries().toList().filterNot { it.isDirectory }
        val abis = entries.mapNotNull { abiOf(it.name) }.distinct().sorted()
        val manifest = zip.getInputStream(zip.getEntry("AndroidManifest.xml")).use { it.readAllBytes() }

        val base = dir.resolve("base.apk")
        write(base, entries.filter { abiOf(it.name) == null }.map { it.name to zip.getInputStream(it).use { s -> s.readAllBytes() } })
        val splits =
            abis.map { abi ->
                val split = dir.resolve("split_config.${abi.replace('-', '_')}.apk")
                val libs = entries.filter { abiOf(it.name) == abi }.map { it.name to zip.getInputStream(it).use { s -> s.readAllBytes() } }
                write(split, listOf("AndroidManifest.xml" to splitManifest(manifest, "config.${abi.replace('-', '_')}")) + libs)
                split
            }
        listOf(base) + splits
    }

/** An XAPK (APKPure): the APKs plus a `manifest.json`. */
internal fun xapk(
    parts: List<Path>,
    file: Path,
): Path {
    write(file, parts.map { it.name to it.toFile().readBytes() } + ("manifest.json" to "{}".toByteArray()))
    return file
}

/** An APKS (bundletool): splits under `splits/`, plus a standalone APK that must be ignored. */
internal fun bundletoolApks(
    parts: List<Path>,
    standalone: Path,
    file: Path,
): Path {
    val splits = parts.map { "splits/" + it.name.replace("split_config.", "base-").replace("base.apk", "base-master.apk") }
    write(
        file,
        splits.zip(parts.map { it.toFile().readBytes() }) +
            ("standalones/standalone.apk" to standalone.toFile().readBytes()) +
            ("toc.pb" to ByteArray(0)),
    )
    return file
}

private fun abiOf(entry: String): String? = Regex("""lib/([^/]+)/.+""").matchEntire(entry)?.groupValues?.get(1)

/** The base manifest marked as a split: split APKs carry their own manifest with a `split` attribute. */
private fun splitManifest(
    manifest: ByteArray,
    split: String,
): ByteArray =
    AndroidManifestBlock.load(manifest.inputStream()).run {
        setSplit(split, true)
        refresh()
        bytes
    }

private fun write(
    file: Path,
    entries: List<Pair<String, ByteArray>>,
) {
    ZipOutputStream(file.outputStream()).use { zip ->
        entries.forEach { (name, bytes) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
    }
}
