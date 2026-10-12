// SPDX-License-Identifier: GPL-3.0-or-later
package com.apkcategorychecker.apk

import java.io.Closeable
import java.io.IOException
import java.nio.file.Path
import java.util.zip.ZipException
import java.util.zip.ZipFile

class ApkFormatException(message: String, cause: Throwable? = null) : IOException(message, cause)

/**
 * Read-only view of an APK, read directly from the zip archive: nothing is extracted to disk.
 * Everything is parsed lazily, on first access.
 */
class ApkFile(val path: Path) : Closeable {

    private val zip = try {
        ZipFile(path.toFile())
    } catch (e: ZipException) {
        throw ApkFormatException("not a valid zip archive: $path", e)
    }

    /** Paths of all the files in the archive, e.g. `assets/www/index.html`. */
    val entryNames: List<String> = zip.entries().asSequence().filterNot { it.isDirectory }.map { it.name }.toList()

    fun read(name: String): ByteArray? =
        zip.getEntry(name)?.let { entry -> zip.getInputStream(entry).use { it.readAllBytes() } }

    fun readText(name: String): String? = read(name)?.toString(Charsets.UTF_8)

    /** Decodes a compiled XML file of the APK (e.g. `res/xml/config.xml`), or null if it doesn't exist. */
    fun readBinaryXml(name: String): XmlElement? = read(name)?.let(BinaryXml::parse)

    val manifest: AndroidManifest by lazy {
        AndroidManifest(readBinaryXml(MANIFEST) ?: throw ApkFormatException("missing $MANIFEST"))
    }

    /** Fully qualified names of all the classes in the APK's DEX files (multidex included). */
    val classNames: Set<String> by lazy {
        entryNames.filter { DEX_FILE.matches(it) }.flatMapTo(HashSet()) { DexReader.classNames(read(it)!!) }
    }

    private val simpleClassNames: Set<String> by lazy { classNames.mapTo(HashSet()) { it.substringAfterLast('.') } }

    /** True if a class with this simple name (e.g. `CordovaActivity`) exists in any package. */
    fun hasClassNamed(simpleName: String): Boolean = simpleName in simpleClassNames

    override fun close() = zip.close()

    private companion object {
        const val MANIFEST = "AndroidManifest.xml"
        val DEX_FILE = Regex("""classes\d*\.dex""")
    }
}

/** The fields of `AndroidManifest.xml` used by the analysis. */
class AndroidManifest(val root: XmlElement) {

    val packageName: String? get() = root.attributes["package"]

    /** Raw value of `android:debuggable`, or null if not declared. */
    val debuggable: String? get() = root.findFirst("application")?.attributes?.get("android:debuggable")

    val permissions: List<String>
        get() = root.findAll("uses-permission").mapNotNull { it.attributes["android:name"] }.toList()

    val minSdkVersion: String? get() = usesSdk("minSdkVersion")
    val targetSdkVersion: String? get() = usesSdk("targetSdkVersion")
    val maxSdkVersion: String? get() = usesSdk("maxSdkVersion")

    private fun usesSdk(attribute: String) = root.findFirst("uses-sdk")?.attributes?.get("android:$attribute")
}
