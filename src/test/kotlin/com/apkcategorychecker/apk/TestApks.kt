// SPDX-License-Identifier: MIT
package com.apkcategorychecker.apk

import com.reandroid.arsc.chunk.TableBlock
import com.reandroid.arsc.chunk.xml.AndroidManifestBlock
import com.reandroid.arsc.value.ValueType
import com.reandroid.dex.key.TypeKey
import com.reandroid.dex.model.DexFile
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream
import kotlin.io.path.readBytes

/**
 * Builds a minimal synthetic APK with ARSCLib: a binary manifest declaring [activities] and
 * [metaData], a DEX defining [classes] (fully qualified names) and the given extra [files] and
 * [binaryFiles]. [resources] maps `type/name` to a value written to `resources.arsc` (e.g.
 * `xml/config` -> `res/ZW.xml`); a meta-data value like `@string/url` references one of them.
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
    metaData: Map<String, String> = emptyMap(),
    binaryFiles: Map<String, ByteArray> = emptyMap(),
): Path {
    val table = if (resources.isEmpty()) null else resourceTable(packageName, resources)
    val manifest =
        AndroidManifestBlock().apply {
            setPackageName(packageName)
            setVersionCode(versionCode)
            if (split != null) setSplit(split, true)
            val application = getOrCreateApplicationElement()
            activities.forEach { getOrCreateActivity(it, false) }
            metaData.forEach { (name, value) ->
                val element = application.newElement("meta-data")
                element.getOrCreateAndroidAttribute("name", ANDROID_NAME).setValueAsString(name)
                val attribute = element.getOrCreateAndroidAttribute("value", ANDROID_VALUE)
                if (value.startsWith("@")) {
                    attribute.setValueType(ValueType.REFERENCE)
                    attribute.setData(table!!.second.getValue(value.removePrefix("@")))
                } else {
                    attribute.setValueAsString(value)
                }
            }
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
        if (table != null) add("resources.arsc", table.first)
        files.forEach { (name, content) -> add(name, content.toByteArray()) }
        binaryFiles.forEach { (name, content) -> add(name, content) }
    }
    return file
}

private const val ANDROID_NAME = 0x01010003
private const val ANDROID_VALUE = 0x01010024

internal fun dex(classes: List<String>): ByteArray {
    val dex = DexFile.createDefault()
    val layout = dex.iterator().next()
    classes.forEach { layout.getOrCreateClass(TypeKey.create("L" + it.replace('.', '/') + ";")) }
    dex.refresh()
    return dex.bytes
}

/** The bytes of `resources.arsc`, and the id of each resource (`type/name`). */
private fun resourceTable(
    packageName: String,
    resources: Map<String, String>,
): Pair<ByteArray, Map<String, Int>> {
    val table = TableBlock()
    val pkg = table.newPackage(0x7f, packageName)
    val entries =
        resources.mapValues { (resource, value) ->
            val (type, name) = resource.split('/')
            pkg.getOrCreate("", type, name).apply { setValueAsString(value) }
        }
    table.refresh()
    return table.bytes to entries.mapValues { it.value.resourceId }
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
