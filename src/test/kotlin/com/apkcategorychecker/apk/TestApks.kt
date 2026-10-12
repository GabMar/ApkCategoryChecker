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

/**
 * Builds a minimal synthetic APK with ARSCLib: a binary manifest declaring [activities], a DEX
 * defining [classes] (fully qualified names) and the given extra [files]. [resources] maps
 * `type/name` to a file path, written to `resources.arsc` (e.g. `xml/config` -> `res/ZW.xml`).
 */
internal fun syntheticApk(
    file: Path,
    files: Map<String, String> = emptyMap(),
    classes: List<String> = emptyList(),
    packageName: String = "com.example.app",
    activities: List<String> = emptyList(),
    resources: Map<String, String> = emptyMap(),
    dex: ByteArray = dex(classes),
): Path {
    val manifest =
        AndroidManifestBlock().apply {
            setPackageName(packageName)
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
        add("classes.dex", dex)
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
