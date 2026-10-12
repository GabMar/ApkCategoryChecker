// SPDX-License-Identifier: MIT
package com.apkcategorychecker.apk

import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.dex.key.TypeKey
import com.reandroid.dex.model.DexFile
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes

/**
 * Builds a minimal synthetic APK with ARSCLib: a binary manifest declaring [activities], a DEX
 * defining [classes] (fully qualified names) and the given extra [files]. [resources] maps
 * `type/name` to a file path, written to `resources.arsc` (e.g. `xml/config` -> `res/ZW.xml`).
 * With [split], the APK is a split of the [packageName] app (e.g. `config.arm64_v8a`).
 */
internal fun syntheticApk(
    file: Path,
    files: Map<String, String> = emptyMap(),
    classes: List<String> = emptyList(),
    packageName: String = "com.example.app",
    activities: List<String> = emptyList(),
    resources: Map<String, String> = emptyMap(),
    dex: ByteArray? = dex(classes),
    split: String? = null,
    versionCode: Int = 1,
): Path {
    val manifest =
        AndroidManifestBlock().apply {
            setPackageName(packageName)
            setVersionCode(versionCode)
            if (split != null) setSplit(split, true)
            getOrCreateApplicationElement()
            activities.forEach { getOrCreateActivity(it, false) }
            refresh()
        }

    ZipOutputStream(file.outputStream()).use { zip ->
        fun add(
            name: String,
            bytes: ByteArray,
        ) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
        add("AndroidManifest.xml", manifest.bytes)
        if (dex != null) add("classes.dex", dex)
        if (resources.isNotEmpty()) add("resources.arsc", resourceTable(packageName, resources))
        files.forEach { (name, content) -> add(name, content.toByteArray()) }
    }
    return file
}

internal fun dex(classes: List<String>): ByteArray {
    val dex = DexFile.createDefault()
    val layout = dex.iterator().next()
    classes.forEach { layout.getOrCreateClass(TypeKey.create("L" + it.replace('.', '/') + ";")) }
    dex.refresh()
    return dex.bytes
}

private fun resourceTable(
    packageName: String,
    resources: Map<String, String>,
): ByteArray {
    val table = TableBlock()
    val pkg = table.newPackage(0x7f, packageName)
    resources.forEach { (resource, path) ->
        val (type, name) = resource.split('/')
        pkg.getOrCreate("", type, name).setValueAsString(path)
    }
    table.refresh()
    return table.bytes
}

/** A zip of APKs, like an XAPK, APKM or APKS file. */
internal fun container(
    file: Path,
    apks: Map<String, Path>,
    extraFiles: Map<String, String> = emptyMap(),
): Path {
    ZipOutputStream(file.outputStream()).use { zip ->
        apks.forEach { (name, apk) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(apk.readBytes())
            zip.closeEntry()
        }
        extraFiles.forEach { (name, content) ->
            zip.putNextEntry(ZipEntry(name))
            zip.write(content.toByteArray())
            zip.closeEntry()
        }
    }
    return file
}
