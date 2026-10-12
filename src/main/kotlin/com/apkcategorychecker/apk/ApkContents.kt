// SPDX-License-Identifier: MIT
package com.apkcategorychecker.apk

import java.io.Closeable

/** The files of an app, from a single APK ([ApkFile]) or from several split APKs ([SplitApk]). */
interface ApkContents : Closeable {
    /** Paths of all the files, in archive order, e.g. `assets/www/index.html`. */
    val entryNames: List<String>

    fun read(name: String): ByteArray?

    fun readText(name: String): String? = read(name)?.toString(Charsets.UTF_8)

    /** Decodes a compiled XML file (e.g. `res/xml/config.xml`), or null if it doesn't exist. */
    fun readBinaryXml(name: String): XmlElement? = read(name)?.let(BinaryXml::parse)

    val manifest: AndroidManifest

    /**
     * The file of a resource according to `resources.arsc`, e.g. `res/xml/config.xml` for
     * `xml/config`. Release builds may shorten resource paths (to something like `res/ZW.xml`).
     */
    fun resourceFile(
        type: String,
        name: String,
    ): String?

    /** The value of a resource as text (e.g. a string), for a reference like `@0x7f0e0001`; null if unknown. */
    fun resourceValue(id: Int): String?

    /** Fully qualified names of all the classes in the DEX files. */
    val classNames: Set<String>

    /** True if a class with this simple name (e.g. `CordovaActivity`) exists in any package. */
    fun hasClassNamed(simpleName: String): Boolean

    /** Names of the split APKs merged into this app, empty for a single APK. */
    val splitNames: List<String> get() = emptyList()
}

/**
 * An app installed from an App Bundle: a base APK plus configuration splits (native libraries,
 * resources for a screen density or language) and feature splits (code), seen as one.
 * The manifest and the resource table come from the base APK.
 */
class SplitApk(
    private val base: ApkFile,
    private val splits: List<Pair<String, ApkFile>>,
    private val onClose: () -> Unit = {},
) : ApkContents {
    private val parts = listOf(base) + splits.map { it.second }

    override val entryNames: List<String> = parts.flatMap { it.entryNames }.distinct()

    override fun read(name: String): ByteArray? = parts.firstNotNullOfOrNull { it.read(name) }

    override val manifest: AndroidManifest get() = base.manifest

    override fun resourceFile(
        type: String,
        name: String,
    ): String? = base.resourceFile(type, name)

    override fun resourceValue(id: Int): String? = base.resourceValue(id)

    override val classNames: Set<String> by lazy { parts.flatMapTo(HashSet()) { it.classNames } }

    override fun hasClassNamed(simpleName: String): Boolean = parts.any { it.hasClassNamed(simpleName) }

    override val splitNames: List<String> = splits.map { it.first }

    override fun close() {
        try {
            parts.forEach { runCatching { it.close() } }
        } finally {
            onClose()
        }
    }
}
