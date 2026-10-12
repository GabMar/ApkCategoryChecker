// SPDX-License-Identifier: MIT
package com.apkcategorychecker.apk

import com.reandroid.apk.ApkModule
import java.io.IOException
import java.nio.file.Path

class ApkFormatException(
    message: String,
    cause: Throwable? = null,
) : IOException(message, cause)

/**
 * Read-only view of an APK, read directly from the archive: nothing is extracted to disk.
 * ARSCLib reads the archive, the binary XML and the resource table. Everything is parsed
 * lazily, on first access.
 */
class ApkFile(
    val path: Path,
) : ApkContents {
    private val module =
        try {
            ApkModule.loadApkFile(path.toFile())
        } catch (e: IOException) {
            throw ApkFormatException("not a valid APK: ${e.message}", e)
        }

    /** Paths of all the files in the archive, in archive order, e.g. `assets/www/index.html`. */
    override val entryNames: List<String> =
        module.zipEntryMap
            .listInputSources()
            .map { it.alias }
            .filterNot { it.endsWith("/") }

    override fun read(name: String): ByteArray? =
        module.zipEntryMap
            .getInputSource(name)
            ?.openStream()
            ?.use { it.readAllBytes() }

    override val manifest: AndroidManifest by lazy {
        AndroidManifest(readBinaryXml(MANIFEST) ?: throw ApkFormatException("missing $MANIFEST"))
    }

    /**
     * The file of a resource according to `resources.arsc`, e.g. `res/xml/config.xml` for
     * `xml/config`. Release builds may shorten resource paths (to something like `res/ZW.xml`).
     */
    override fun resourceFile(
        type: String,
        name: String,
    ): String? =
        synchronized(stringPoolLock) {
            runCatching {
                if (!module.hasTableBlock()) return null
                module.tableBlock.listPackages().firstNotNullOfOrNull { it.getResource(type, name)?.get()?.valueAsString }
            }.getOrNull() // the resource table is a best-effort hint: a broken one must not fail the analysis
        }

    /**
     * Fully qualified names of all the classes in the APK's DEX files (multidex included).
     * Read with [DexReader] rather than ARSCLib, which builds a full model of every DEX:
     * several times slower and needing ~10x the memory.
     */
    override val classNames: Set<String> by lazy {
        entryNames.filter { DEX_FILE.matches(it) }.flatMapTo(HashSet()) { DexReader.classNames(read(it)!!) }
    }

    private val simpleClassNames: Set<String> by lazy { classNames.mapTo(HashSet()) { it.substringAfterLast('.') } }

    override fun hasClassNamed(simpleName: String): Boolean = simpleName in simpleClassNames

    override fun close() = module.close()

    private companion object {
        const val MANIFEST = "AndroidManifest.xml"
        val DEX_FILE = Regex("""classes\d*\.dex""")
    }
}

/** The fields of `AndroidManifest.xml` used by the analysis. */
class AndroidManifest(
    val root: XmlElement,
) {
    val packageName: String? get() = root.attributes["package"]

    val versionCode: String? get() = root.attributes["android:versionCode"]

    /** The name of the split (e.g. `config.arm64_v8a`) for split APKs, null for base or standalone APKs. */
    val split: String? get() = root.attributes["split"]

    /** Raw value of `android:debuggable`, or null if not declared. */
    val debuggable: String? get() = root.findFirst("application")?.attributes?.get("android:debuggable")

    val permissions: List<String>
        get() = root.findAll("uses-permission").mapNotNull { it.attributes["android:name"] }.toList()

    val minSdkVersion: String? get() = usesSdk("minSdkVersion")
    val targetSdkVersion: String? get() = usesSdk("targetSdkVersion")
    val maxSdkVersion: String? get() = usesSdk("maxSdkVersion")

    private fun usesSdk(attribute: String) = root.findFirst("uses-sdk")?.attributes?.get("android:$attribute")
}
