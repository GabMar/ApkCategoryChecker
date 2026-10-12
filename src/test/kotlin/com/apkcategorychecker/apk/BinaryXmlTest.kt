package com.apkcategorychecker.apk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource
import java.io.ByteArrayOutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class BinaryXmlTest {

    @ParameterizedTest(name = "utf8 string pool: {0}")
    @ValueSource(booleans = [true, false])
    fun `decodes elements, namespaces and typed attributes`(utf8: Boolean) {
        val longLabel = "x".repeat(300)
        val xml = AxmlWriter(utf8).apply {
            namespace("android", BinaryXml.ANDROID_NS)
            start("manifest", Attr(null, "package", string = "com.example.città"))
            start(
                "uses-sdk",
                Attr(BinaryXml.ANDROID_NS, "minSdkVersion", type = 0x10, data = 21),
                Attr(BinaryXml.ANDROID_NS, "targetSdkVersion", type = 0x10, data = 34),
            )
            end("uses-sdk")
            start(
                "application",
                Attr(BinaryXml.ANDROID_NS, "debuggable", type = 0x12, data = -1),
                Attr(BinaryXml.ANDROID_NS, "label", string = longLabel),
                Attr(BinaryXml.ANDROID_NS, "icon", type = 0x01, data = 0x7f020000),
            )
            end("application")
            end("manifest")
        }.toByteArray()

        val manifest = AndroidManifest(BinaryXml.parse(xml))
        assertEquals("com.example.città", manifest.packageName)
        assertEquals("21", manifest.minSdkVersion)
        assertEquals("34", manifest.targetSdkVersion)
        assertEquals("true", manifest.debuggable)
        val application = manifest.root.findFirst("application")!!
        assertEquals(longLabel, application.attributes["android:label"])
        assertEquals("@0x7f020000", application.attributes["android:icon"])
        assertEquals(mapOf("android" to BinaryXml.ANDROID_NS), manifest.root.namespaces)
    }

    @Test
    fun `resolves framework attributes by resource id when the name is obfuscated`() {
        val xml = AxmlWriter(utf8 = true, resourceAttributes = listOf("" to 0x0101020c)).apply {
            namespace("android", BinaryXml.ANDROID_NS)
            start("manifest")
            // An empty name string, as left by some obfuscators: only the resource id is meaningful.
            start("uses-sdk", Attr(BinaryXml.ANDROID_NS, "", resourceId = 0x0101020c, type = 0x10, data = 23))
            end("uses-sdk")
            end("manifest")
        }.toByteArray()

        assertEquals("23", AndroidManifest(BinaryXml.parse(xml)).minSdkVersion)
    }

    @Test
    fun `rejects data that is not binary xml`() {
        assertThrows<ApkFormatException> { BinaryXml.parse("<manifest/>".toByteArray()) }
        val truncated = AxmlWriter(utf8 = true).apply { start("a"); end("a") }.toByteArray().copyOf(40)
        assertThrows<ApkFormatException> { BinaryXml.parse(truncated) }
    }

    private class Attr(
        val ns: String?,
        val name: String,
        val string: String? = null,
        val type: Int = 0x03,
        val data: Int = 0,
        val resourceId: Int? = null,
    )

    /** Just enough of an AXML encoder to produce test inputs. */
    private class AxmlWriter(private val utf8: Boolean, resourceAttributes: List<Pair<String, Int>> = emptyList()) {
        // Attribute names with a resource id come first in the pool, aligned with the resource map.
        private val strings = resourceAttributes.map { it.first }.toMutableList()
        private val resourceIds = resourceAttributes.map { it.second }.toMutableList()
        private val chunks = mutableListOf<ByteArray>()

        fun namespace(prefix: String, uri: String) {
            chunks += chunk(0x0100, 16, ints(1, -1, index(prefix), index(uri)))
        }

        fun start(name: String, vararg attrs: Attr) {
            val body = ByteArrayOutputStream()
            body.write(ints(1, -1, -1, index(name)))
            body.write(shorts(20, 20, attrs.size, 0, 0, 0))
            for (a in attrs) {
                val raw = a.string?.let { index(it) } ?: -1
                val data = a.string?.let { index(it) } ?: a.data
                body.write(ints(a.ns?.let { index(it) } ?: -1, attrIndex(a), raw))
                body.write(shorts(8))
                body.write(byteArrayOf(0, a.type.toByte()))
                body.write(ints(data))
            }
            chunks += chunk(0x0102, 16, body.toByteArray())
        }

        fun end(name: String) {
            chunks += chunk(0x0103, 16, ints(1, -1, -1, index(name)))
        }

        fun toByteArray(): ByteArray {
            val body = ByteArrayOutputStream()
            body.write(stringPool())
            if (resourceIds.isNotEmpty()) body.write(chunk(0x0180, 8, ints(*resourceIds.toIntArray())))
            chunks.forEach(body::write)
            return chunk(0x0003, 8, body.toByteArray())
        }

        private fun attrIndex(a: Attr): Int =
            if (a.resourceId == null) index(a.name)
            else resourceIds.indexOf(a.resourceId).also { check(it >= 0) { "unregistered resource id" } }

        private fun index(s: String): Int {
            val existing = strings.withIndex().drop(resourceIds.size).firstOrNull { it.value == s }
            return existing?.index ?: strings.size.also { strings += s }
        }

        private fun stringPool(): ByteArray {
            val data = ByteArrayOutputStream()
            val offsets = strings.map { s ->
                data.size().also { data.write(encode(s)) }
            }
            while (data.size() % 4 != 0) data.write(0)
            val header = 28
            val payload = ByteArrayOutputStream()
            payload.write(ints(strings.size, 0, if (utf8) 0x100 else 0, header + 4 * strings.size, 0))
            payload.write(ints(*offsets.toIntArray()))
            payload.write(data.toByteArray())
            return chunk(0x0001, header, payload.toByteArray())
        }

        private fun encode(s: String): ByteArray {
            val out = ByteArrayOutputStream()
            if (utf8) {
                val bytes = s.toByteArray(Charsets.UTF_8)
                out.write(len8(s.length)); out.write(len8(bytes.size)); out.write(bytes); out.write(0)
            } else {
                require(s.length < 0x8000)
                out.write(shorts(s.length)); out.write(s.toByteArray(Charsets.UTF_16LE)); out.write(shorts(0))
            }
            return out.toByteArray()
        }

        private fun len8(n: Int) =
            if (n < 0x80) byteArrayOf(n.toByte()) else byteArrayOf((0x80 or (n shr 8)).toByte(), n.toByte())

        private fun chunk(type: Int, headerSize: Int, payload: ByteArray): ByteArray =
            shorts(type, headerSize) + ints(8 + payload.size) + payload

        private fun ints(vararg v: Int) =
            ByteBuffer.allocate(4 * v.size).order(ByteOrder.LITTLE_ENDIAN).apply { v.forEach(::putInt) }.array()

        private fun shorts(vararg v: Int) =
            ByteBuffer.allocate(2 * v.size).order(ByteOrder.LITTLE_ENDIAN).apply { v.forEach { putShort(it.toShort()) } }.array()
    }
}
