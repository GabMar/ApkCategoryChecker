// SPDX-License-Identifier: MIT
package com.apkcategorychecker.apk

import com.reandroid.arsc.chunk.xml.ResXmlAttribute
import com.reandroid.arsc.chunk.xml.ResXmlDocument
import com.reandroid.arsc.chunk.xml.ResXmlElement
import com.reandroid.arsc.value.ValueType
import java.io.ByteArrayInputStream

/** An element of a decoded XML document. Attribute names use the `android:` prefix as in source. */
class XmlElement(
    val name: String,
    val attributes: Map<String, String>,
    val children: List<XmlElement>,
) {
    /** This element and all its descendants with the given name, in document order. */
    fun findAll(name: String): Sequence<XmlElement> =
        sequence {
            if (this@XmlElement.name == name) yield(this@XmlElement)
            for (child in children) yieldAll(child.findAll(name))
        }

    fun findFirst(name: String): XmlElement? = findAll(name).firstOrNull()

    /** Renders the element back to (unformatted) XML text, mainly for text searches. */
    fun toXml(): String = buildString { render(this) }

    private fun render(out: StringBuilder) {
        out.append('<').append(name)
        for ((key, value) in attributes) {
            out
                .append(' ')
                .append(key)
                .append("=\"")
                .append(escape(value))
                .append('"')
        }
        if (children.isEmpty()) {
            out.append("/>")
            return
        }
        out.append('>')
        for (child in children) child.render(out)
        out.append("</").append(name).append('>')
    }

    private fun escape(s: String) =
        s
            .replace("&", "&amp;")
            .replace("<", "&lt;")
            .replace(">", "&gt;")
            .replace("\"", "&quot;")
}

/**
 * ARSCLib decodes the string pools of binary XML and of `resources.arsc` with a shared
 * `CharsetDecoder`, which is not thread-safe: those reads must not run concurrently.
 * (DEX parsing, the slow part, is thread-safe and stays parallel.)
 */
internal val stringPoolLock = Any()

/**
 * Decodes Android binary XML (the compiled format of `AndroidManifest.xml` and of the XML files
 * under `res/`) with ARSCLib, into an [XmlElement] tree.
 */
object BinaryXml {
    fun parse(data: ByteArray): XmlElement {
        synchronized(stringPoolLock) {
            val document = ResXmlDocument()
            try {
                document.readBytes(ByteArrayInputStream(data))
            } catch (e: Exception) {
                throw ApkFormatException("invalid binary XML: ${e.message}", e)
            }
            val root = document.documentElement ?: throw ApkFormatException("binary XML has no root element")
            return root.toXmlElement()
        }
    }

    private fun ResXmlElement.toXmlElement(): XmlElement {
        val attributes = linkedMapOf<String, String>()
        getAttributes().forEach { attributes[it.qualifiedName()] = it.valueText() }
        val children = getElements().asSequence().map { it.toXmlElement() }.toList()
        return XmlElement(name, attributes, children)
    }

    private fun ResXmlAttribute.qualifiedName(): String {
        // Android resolves framework attributes by resource id, not by name: obfuscators exploit
        // this by blanking or faking the name string, so trust the id when we know it.
        KNOWN_ATTRIBUTES[nameId]?.let { return "android:$it" }
        return getName(true)
    }

    /** References can't be resolved without the resource table: render them as ids. */
    private fun ResXmlAttribute.valueText(): String =
        when (valueType) {
            ValueType.REFERENCE, ValueType.DYNAMIC_REFERENCE -> "@0x%08x".format(data)
            ValueType.ATTRIBUTE, ValueType.DYNAMIC_ATTRIBUTE -> "?0x%08x".format(data)
            else -> runCatching { decodeValue() }.getOrNull() ?: "0x%08x".format(data)
        }

    /** Framework attribute ids (android.R.attr) of the attributes the analysis relies on. */
    private val KNOWN_ATTRIBUTES =
        mapOf(
            0x01010003 to "name",
            0x0101000f to "debuggable",
            0x0101020c to "minSdkVersion",
            0x01010270 to "targetSdkVersion",
            0x01010271 to "maxSdkVersion",
        )
}
