// SPDX-License-Identifier: MIT
package com.apkcategorychecker.sdk

import com.apkcategorychecker.analyzer.ApkAnalyzer
import com.apkcategorychecker.apk.syntheticApk
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.nio.file.Path

class SdkRulesTest {
    @TempDir
    lateinit var dir: Path

    private fun sdks(
        rules: SdkRules = SdkRules.builtIn,
        files: Map<String, String> = emptyMap(),
        classes: List<String> = emptyList(),
        components: List<Pair<String, String>> = emptyList(),
        actions: List<String> = emptyList(),
    ): List<SdkFinding> {
        val apk = syntheticApk(dir.resolve("app.apk"), files, classes, components = components, actions = actions)
        return ApkAnalyzer(sdkRules = rules).analyze(apk).sdks
    }

    private val testRules =
        SdkRules.parse(
            """
            {
              "libraries": {
                "A": { "label": "Alpha", "team": "Team A" },
                "B": { "label": "Beta" },
                "B2": { "label": "Beta", "team": "Team B" },
                "C": { "label": "Gamma" },
                "D": { "label": "Delta" }
              },
              "rules": [
                { "type": "nativeLib", "name": "libalpha.so", "regex": false, "priority": 5, "id": 1, "library": "A" },
                { "type": "nativeLib", "name": "libalpha(.*)\\.so", "regex": true, "priority": 1, "id": 2, "library": "C" },
                { "type": "nativeLib", "name": "lib(.*)\\.so", "regex": true, "priority": 9, "id": 3, "library": "D" },
                { "type": "service", "name": "com.example.app.SyncService", "regex": false, "priority": 1, "id": 4, "library": "B" },
                { "type": "provider", "name": "com.beta.Provider", "regex": false, "priority": 1, "id": 5, "library": "B2" },
                { "type": "action", "name": "com.alpha.PUSH", "regex": false, "priority": 1, "id": 6, "library": "A" },
                { "type": "dex", "name": "com.gamma", "regex": false, "priority": 1, "id": 7, "library": "C" },
                { "type": "dex", "name": "org\\.delta\\.(.*)", "regex": true, "priority": 1, "id": 8, "library": "D" }
              ]
            }
            """.trimIndent(),
        )

    @Test
    fun `built-in rules load and all their patterns compile`() {
        val data = Json.parseToJsonElement(javaClass.getResource("/sdks/libchecker-rules.json")!!.readText()).jsonObject
        assertTrue(data["rules"]!!.jsonArray.size > 2000)
        assertEquals(0, SdkRules.builtIn.skippedPatterns)
    }

    @Test
    fun `exact names win over patterns, and patterns go by priority`() {
        val found =
            sdks(testRules, mapOf("lib/arm64-v8a/libalpha.so" to "", "lib/arm64-v8a/libalpha2.so" to "", "lib/x86/libother.so" to ""))
        assertEquals(
            listOf(
                SdkFinding("Alpha", "Team A", listOf("lib/arm64-v8a/libalpha.so")),
                SdkFinding("Delta", null, listOf("lib/x86/libother.so")),
                SdkFinding("Gamma", null, listOf("lib/arm64-v8a/libalpha2.so")),
            ),
            found,
        )
    }

    @Test
    fun `manifest components with relative names, actions and dex packages`() {
        val found =
            sdks(
                testRules,
                classes = listOf("com.gamma.sub.Client", "org.delta.Tracker", "com.gammaray.Other"),
                components = listOf("service" to ".SyncService", "provider" to "com.beta.Provider"),
                actions = listOf("com.alpha.PUSH"),
            )
        assertEquals(
            listOf(
                SdkFinding("Alpha", "Team A", listOf("action com.alpha.PUSH")),
                // Two libraries labelled Beta are reported once, with the first team found.
                SdkFinding("Beta", "Team B", listOf("provider com.beta.Provider", "service com.example.app.SyncService")),
                SdkFinding("Delta", null, listOf("class org.delta.Tracker")),
                SdkFinding("Gamma", null, listOf("package com.gamma")),
            ),
            found,
        )
    }

    @Test
    fun `evidence is capped`() {
        val libs = (1..8).associate { "lib/arm64-v8a/libz$it.so" to "" }
        val delta = sdks(testRules, libs).single()
        assertEquals("Delta", delta.name)
        assertEquals(SdkRules.MAX_EVIDENCE, delta.evidence.size)
    }

    @Test
    fun `built-in rules recognize common sdks`() {
        val found =
            sdks(
                files = mapOf("lib/arm64-v8a/libflutter.so" to "", "lib/arm64-v8a/libhermes.so" to ""),
                components = listOf("service" to "com.google.firebase.messaging.FirebaseMessagingService"),
            ).map { it.name }
        assertTrue("Flutter" in found, found.toString())
        assertTrue("Hermes JS Engine" in found, found.toString())
        assertTrue(found.any { "Firebase" in it || it == "FCM" }, found.toString())
    }

    @Test
    fun `corrected upstream rules`() {
        // Generic names that LibChecker-Rules attributes to a single SDK.
        val found =
            sdks(
                files =
                    mapOf(
                        "lib/arm64-v8a/libmain.so" to "",
                        "lib/armeabi/libstlport_shared.so" to "",
                        "lib/x86/libfbjni.so" to "",
                    ),
            )
        assertEquals(emptyList<String>(), found.map { it.name }.filter { it in setOf("Unity", "SQLCipher", "Facebook SDK") })
        assertEquals(listOf("React Native"), sdks(files = mapOf("lib/x86/libreact_nativemodule_core.so" to "")).map { it.name })
        assertEquals(listOf("Qt"), sdks(files = mapOf("lib/x86/libplugins_imageformats_qjpeg_x86.so" to "")).map { it.name })
    }

    @Test
    fun `sdk detection can be turned off`() {
        val apk = syntheticApk(dir.resolve("off.apk"), mapOf("lib/arm64-v8a/libflutter.so" to ""))
        assertEquals(emptyList<SdkFinding>(), ApkAnalyzer(sdkRules = null).analyze(apk).sdks)
    }
}
