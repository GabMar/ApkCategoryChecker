// SPDX-License-Identifier: GPL-3.0-or-later
package com.apkcategorychecker.apk

import java.nio.BufferUnderflowException
import java.nio.ByteBuffer
import java.nio.ByteOrder

/** An element of a decoded XML document. Attribute names use the `android:` prefix as in source. */
class XmlElement(
    val name: String,
    val attributes: Map<String, String>,
    val children: List<XmlElement>,
    val text: String = "",
    /** Namespace declarations made on this element, prefix -> URI. */
    val namespaces: Map<String, String> = emptyMap(),
) {
    /** This element and all its descendants with the given name, in document order. */
    fun findAll(name: String): Sequence<XmlElement> = sequence {
        if (this@XmlElement.name == name) yield(this@XmlElement)
        for (child in children) yieldAll(child.findAll(name))
    }

    fun findFirst(name: String): XmlElement? = findAll(name).firstOrNull()

    /** Renders the element back to (unformatted) XML text, mainly for text searches. */
    fun toXml(): String = buildString { render(this) }

    private fun render(out: StringBuilder) {
        out.append('<').append(name)
        for ((prefix, uri) in namespaces) out.append(" xmlns:").append(prefix).append("=\"").append(escape(uri)).append('"')
        for ((key, value) in attributes) out.append(' ').append(key).append("=\"").append(escape(value)).append('"')
        if (children.isEmpty() && text.isEmpty()) {
            out.append("/>")
            return
        }
        out.append('>').append(escape(text))
        for (child in children) child.render(out)
        out.append("</").append(name).append('>')
    }

    private fun escape(s: String) =
        s.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;")
}

/**
 * Decoder for Android binary XML (AXML), the compiled format of `AndroidManifest.xml`
 * and of the XML files under `res/`.
 *
 * See frameworks/base/libs/androidfw/include/androidfw/ResourceTypes.h in AOSP.
 */
object BinaryXml {

    const val ANDROID_NS = "http://schemas.android.com/apk/res/android"

    fun parse(data: ByteArray): XmlElement {
        val buf = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)
        try {
            return Parser(buf).parse()
        } catch (e: IndexOutOfBoundsException) {
            throw ApkFormatException("truncated or corrupted binary XML", e)
        } catch (e: BufferUnderflowException) {
            throw ApkFormatException("truncated or corrupted binary XML", e)
        }
    }

    private class ElementBuilder(val name: String, val attributes: Map<String, String>, val namespaces: Map<String, String>) {
        val children = mutableListOf<XmlElement>()
        val text = StringBuilder()
        fun build() = XmlElement(name, attributes, children, text.toString(), namespaces)
    }

    private class Parser(private val buf: ByteBuffer) {
        private var strings: List<String> = emptyList()
        private var resourceIds = IntArray(0)
        private val prefixByUri = mutableMapOf<String, String>()
        private val pendingNamespaces = linkedMapOf<String, String>()
        private val stack = ArrayDeque<ElementBuilder>()
        private var root: XmlElement? = null

        fun parse(): XmlElement {
            if (u16(0) != RES_XML_TYPE) throw ApkFormatException("not a binary XML file")
            var pos = u16(2)
            while (pos + CHUNK_HEADER_SIZE <= buf.limit()) {
                val type = u16(pos)
                val headerSize = u16(pos + 2)
                val size = buf.getInt(pos + 4)
                if (size < CHUNK_HEADER_SIZE || pos + size > buf.limit()) {
                    throw ApkFormatException("invalid chunk size $size at offset $pos")
                }
                when (type) {
                    RES_STRING_POOL_TYPE -> strings = readStringPool(pos)
                    RES_XML_RESOURCE_MAP_TYPE ->
                        resourceIds = IntArray((size - headerSize) / 4) { buf.getInt(pos + headerSize + it * 4) }
                    RES_XML_START_NAMESPACE_TYPE -> {
                        val prefix = string(buf.getInt(pos + headerSize))
                        val uri = string(buf.getInt(pos + headerSize + 4))
                        prefixByUri[uri] = prefix
                        pendingNamespaces[prefix] = uri
                    }
                    RES_XML_START_ELEMENT_TYPE -> startElement(pos + headerSize)
                    RES_XML_END_ELEMENT_TYPE -> {
                        val element = stack.removeLast().build()
                        if (stack.isEmpty()) root = element else stack.last().children += element
                    }
                    RES_XML_CDATA_TYPE -> stack.lastOrNull()?.text?.append(string(buf.getInt(pos + headerSize)))
                }
                pos += size
            }
            return root ?: throw ApkFormatException("binary XML has no root element")
        }

        private fun startElement(ext: Int) {
            val name = string(buf.getInt(ext + 4))
            val attributeStart = u16(ext + 8)
            val attributeSize = u16(ext + 10)
            val attributeCount = u16(ext + 12)

            val attributes = linkedMapOf<String, String>()
            repeat(attributeCount) { i ->
                val at = ext + attributeStart + i * attributeSize
                attributes[attributeName(buf.getInt(at), buf.getInt(at + 4))] =
                    attributeValue(buf.getInt(at + 8), buf.get(at + 15).toInt() and 0xFF, buf.getInt(at + 16))
            }
            stack.addLast(ElementBuilder(name, attributes, pendingNamespaces.toMap()))
            pendingNamespaces.clear()
        }

        private fun attributeName(nsIndex: Int, nameIndex: Int): String {
            // Android resolves framework attributes by resource id, not by name: obfuscators exploit
            // this by blanking or faking the name string, so trust the id when we know it.
            val name = resourceIds.getOrNull(nameIndex)?.let(KNOWN_ATTRIBUTES::get) ?: string(nameIndex)
            if (nsIndex == NO_INDEX) return name
            val uri = string(nsIndex)
            val prefix = prefixByUri[uri] ?: if (uri == ANDROID_NS) "android" else uri
            return "$prefix:$name"
        }

        private fun attributeValue(rawIndex: Int, dataType: Int, data: Int): String = when {
            rawIndex != NO_INDEX -> string(rawIndex)
            dataType == TYPE_STRING -> string(data)
            dataType == TYPE_REFERENCE -> "@0x%08x".format(data)
            dataType == TYPE_ATTRIBUTE -> "?0x%08x".format(data)
            dataType == TYPE_FLOAT -> Float.fromBits(data).toString()
            dataType == TYPE_INT_DEC -> data.toString()
            dataType == TYPE_INT_HEX -> "0x%x".format(data)
            dataType == TYPE_INT_BOOLEAN -> (data != 0).toString()
            dataType in TYPE_FIRST_COLOR..TYPE_LAST_COLOR -> "#%08x".format(data)
            else -> "0x%08x".format(data)
        }

        private fun readStringPool(start: Int): List<String> {
            val headerSize = u16(start + 2)
            val count = buf.getInt(start + 8)
            val utf8 = buf.getInt(start + 16) and UTF8_FLAG != 0
            val stringsStart = start + buf.getInt(start + 20)
            return List(count) { i ->
                val offset = stringsStart + buf.getInt(start + headerSize + i * 4)
                if (utf8) readUtf8(offset) else readUtf16(offset)
            }
        }

        private fun readUtf8(offset: Int): String {
            var pos = offset
            pos += if (buf.get(pos).toInt() and 0x80 != 0) 2 else 1 // length in UTF-16 units, unused
            var length = buf.get(pos).toInt() and 0xFF
            if (length and 0x80 != 0) length = ((length and 0x7F) shl 8) or (buf.get(++pos).toInt() and 0xFF)
            pos++
            return String(buf.array(), pos, length, Charsets.UTF_8)
        }

        private fun readUtf16(offset: Int): String {
            var pos = offset
            var length = u16(pos)
            pos += 2
            if (length and 0x8000 != 0) {
                length = ((length and 0x7FFF) shl 16) or u16(pos)
                pos += 2
            }
            return String(buf.array(), pos, length * 2, Charsets.UTF_16LE)
        }

        private fun string(index: Int): String =
            if (index == NO_INDEX) "" else strings.getOrNull(index) ?: throw ApkFormatException("invalid string index $index")

        private fun u16(pos: Int) = buf.getShort(pos).toInt() and 0xFFFF
    }

    private const val NO_INDEX = -1
    private const val CHUNK_HEADER_SIZE = 8
    private const val UTF8_FLAG = 0x100

    private const val RES_STRING_POOL_TYPE = 0x0001
    private const val RES_XML_TYPE = 0x0003
    private const val RES_XML_START_NAMESPACE_TYPE = 0x0100
    private const val RES_XML_START_ELEMENT_TYPE = 0x0102
    private const val RES_XML_END_ELEMENT_TYPE = 0x0103
    private const val RES_XML_CDATA_TYPE = 0x0104
    private const val RES_XML_RESOURCE_MAP_TYPE = 0x0180

    private const val TYPE_REFERENCE = 0x01
    private const val TYPE_ATTRIBUTE = 0x02
    private const val TYPE_STRING = 0x03
    private const val TYPE_FLOAT = 0x04
    private const val TYPE_INT_DEC = 0x10
    private const val TYPE_INT_HEX = 0x11
    private const val TYPE_INT_BOOLEAN = 0x12
    private const val TYPE_FIRST_COLOR = 0x1C
    private const val TYPE_LAST_COLOR = 0x1F

    /** Framework attribute ids (android.R.attr) of the attributes the analysis relies on. */
    private val KNOWN_ATTRIBUTES = mapOf(
        0x01010003 to "name",
        0x0101000f to "debuggable",
        0x0101020c to "minSdkVersion",
        0x01010270 to "targetSdkVersion",
        0x01010271 to "maxSdkVersion",
    )
}
