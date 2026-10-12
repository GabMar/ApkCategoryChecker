// SPDX-License-Identifier: MIT
package com.apkcategorychecker.apk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.ValueSource

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
}
