package com.apkcategorychecker.apk

import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.file.Path
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.io.path.outputStream

/**
 * Builds a minimal synthetic APK: a binary manifest, a DEX defining [classes] (fully
 * qualified names) and the given extra files.
 */
internal fun syntheticApk(
    file: Path,
    files: Map<String, String> = emptyMap(),
    classes: List<String> = emptyList(),
    packageName: String = "com.example.app",
    activities: List<String> = emptyList(),
): Path {
    val manifest = AxmlWriter(utf8 = true).apply {
        namespace("android", BinaryXml.ANDROID_NS)
        start("manifest", Attr(null, "package", string = packageName))
        start("application")
        for (activity in activities) {
            start("activity", Attr(BinaryXml.ANDROID_NS, "name", string = activity))
            end("activity")
        }
        end("application")
        end("manifest")
    }.toByteArray()

    ZipOutputStream(file.outputStream()).use { zip ->
        fun add(name: String, bytes: ByteArray) {
            zip.putNextEntry(ZipEntry(name))
            zip.write(bytes)
            zip.closeEntry()
        }
        add("AndroidManifest.xml", manifest)
        add("classes.dex", dex(classes))
        files.forEach { (name, content) -> add(name, content.toByteArray()) }
    }
    return file
}

/** Just enough of a DEX file for [DexReader]: string ids, type ids and class defs. */
internal fun dex(classes: List<String>): ByteArray {
    val n = classes.size
    val stringIdsOff = 0x70
    val typeIdsOff = stringIdsOff + 4 * n
    val classDefsOff = typeIdsOff + 4 * n
    var stringDataOff = classDefsOff + 32 * n
    val descriptors = classes.map { "L" + it.replace('.', '/') + ";" }

    val buf = ByteBuffer.allocate(stringDataOff + descriptors.sumOf { it.length + 3 }).order(ByteOrder.LITTLE_ENDIAN)
    buf.put("dex\n035\u0000".toByteArray())
    buf.putInt(0x38, n).putInt(0x3C, stringIdsOff)
    buf.putInt(0x40, n).putInt(0x44, typeIdsOff)
    buf.putInt(0x60, n).putInt(0x64, classDefsOff)
    descriptors.forEachIndexed { i, descriptor ->
        buf.putInt(stringIdsOff + 4 * i, stringDataOff)
        buf.putInt(typeIdsOff + 4 * i, i)
        buf.putInt(classDefsOff + 32 * i, i)
        buf.position(stringDataOff)
        buf.put(descriptor.length.toByte()) // ULEB128, fine for names shorter than 128
        buf.put(descriptor.toByteArray()).put(0)
        stringDataOff = buf.position()
    }
    return buf.array()
}
