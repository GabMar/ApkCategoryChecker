// SPDX-License-Identifier: MIT
package com.apkcategorychecker.apk

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path
import kotlin.io.path.toPath
import kotlin.io.path.writeText

class ApkFileTest {
    private fun sample(name: String) = ApkFile(javaClass.getResource("/apks/$name")!!.toURI().toPath())

    @Test
    fun `reads the manifest of a cordova app`() =
        sample("cordova/HelloWorld.apk").use { apk ->
            val manifest = apk.manifest
            assertEquals("com.example.hello", manifest.packageName)
            assertEquals("true", manifest.debuggable)
            assertEquals(listOf("android.permission.INTERNET"), manifest.permissions)
            assertEquals("10", manifest.minSdkVersion)
            assertEquals("19", manifest.targetSdkVersion)
            assertNull(manifest.maxSdkVersion)
        }

    @Test
    fun `reads the manifest of a titanium app`() =
        sample("titanium/TodoList.apk").use { apk ->
            val manifest = apk.manifest
            assertEquals("com.appcelerator.sample.todo", manifest.packageName)
            assertEquals("false", manifest.debuggable)
            assertEquals(7, manifest.permissions.size)
            assertEquals("android.permission.ACCESS_MOCK_LOCATION", manifest.permissions.last())
        }

    @Test
    fun `lists the classes defined in the dex files`() {
        // Reference counts: the .class files produced by dex2jar for the same APKs.
        sample("cordova/HelloWorld.apk").use { apk ->
            assertEquals(213, apk.classNames.size)
            assertTrue("org.apache.cordova.CordovaActivity" in apk.classNames)
            assertTrue(apk.hasClassNamed("CordovaActivity"))
            assertFalse(apk.hasClassNamed("TiActivity"))
        }
        sample("sencha-cordova/SenToCo.apk").use { apk -> assertEquals(181, apk.classNames.size) }
        sample("titanium/TodoList.apk").use { apk ->
            assertEquals(1558, apk.classNames.size)
            assertTrue("ti.modules.titanium.TitaniumModule" in apk.classNames)
            assertTrue("org.appcelerator.titanium.TiActivity" in apk.classNames)
        }
    }

    @Test
    fun `decodes compiled resource xml files`() =
        sample("cordova/HelloWorld.apk").use { apk ->
            val config = apk.readBinaryXml("res/xml/config.xml")!!
            assertEquals("widget", config.name)
            assertTrue("org.apache.cordova" in config.toXml())
            assertNull(apk.readBinaryXml("res/xml/missing.xml"))
        }

    @Test
    fun `lists the archive entries`() =
        sample("cordova/HelloWorld.apk").use { apk ->
            assertEquals(27, apk.entryNames.size)
            assertTrue("assets/www/index.html" in apk.entryNames)
            assertTrue(apk.entryNames.none { it.endsWith("/") })
        }

    @Test
    fun `rejects files that are not zip archives`(
        @TempDir dir: Path,
    ) {
        val notAnApk = dir.resolve("fake.apk").apply { writeText("hello") }
        assertThrows<ApkFormatException> { ApkFile(notAnApk) }
    }

    /** A DEX header declaring 127 classes, with nothing after it. */
    private val truncatedDex = "dex\n035\u0000".toByteArray() + ByteArray(0x68).also { it[0x58] = 0x7F }

    @Test
    fun `rejects corrupted dex files`(
        @TempDir dir: Path,
    ) {
        val apk = syntheticApk(dir.resolve("bad-dex.apk"), dex = truncatedDex)
        ApkFile(apk).use { assertThrows<ApkFormatException> { it.classNames } }
    }

    @Test
    fun `finds resource files through resources arsc`(
        @TempDir dir: Path,
    ) {
        val apk = syntheticApk(dir.resolve("shortened.apk"), resources = mapOf("xml/config" to "res/ZW.xml"))
        ApkFile(apk).use {
            assertEquals("res/ZW.xml", it.resourceFile("xml", "config"))
            assertNull(it.resourceFile("xml", "missing"))
        }
        sample("cordova/HelloWorld.apk").use { assertEquals("res/xml/config.xml", it.resourceFile("xml", "config")) }
    }

    @Test
    fun `reads manifests and classes of synthetic apks`(
        @TempDir dir: Path,
    ) {
        val apk =
            syntheticApk(
                dir.resolve("app.apk"),
                classes = listOf("com.example.app.MainActivity", "io.flutter.embedding.engine.FlutterEngine"),
                activities = listOf("com.example.app.MainActivity"),
            )
        ApkFile(apk).use {
            assertEquals("com.example.app", it.manifest.packageName)
            assertEquals(
                listOf("com.example.app.MainActivity"),
                it.manifest.root
                    .findAll("activity")
                    .map { a -> a.attributes["android:name"] }
                    .toList(),
            )
            assertEquals(setOf("com.example.app.MainActivity", "io.flutter.embedding.engine.FlutterEngine"), it.classNames)
        }
    }
}
