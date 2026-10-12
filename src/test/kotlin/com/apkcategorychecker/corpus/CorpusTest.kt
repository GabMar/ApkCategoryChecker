// SPDX-License-Identifier: MIT
package com.apkcategorychecker.corpus

import com.apkcategorychecker.analyzer.ApkAnalyzer
import com.apkcategorychecker.analyzer.ApkReport
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assumptions.abort
import org.junit.jupiter.api.DynamicTest
import org.junit.jupiter.api.DynamicTest.dynamicTest
import org.junit.jupiter.api.Tag
import org.junit.jupiter.api.TestFactory
import java.io.IOException
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest
import kotlin.io.path.createDirectories
import kotlin.io.path.exists
import kotlin.io.path.inputStream
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.name

/**
 * Checks the detection on the real apps listed in `corpus/apps.json`, downloading their latest
 * (or pinned) APKs. Run with `./gradlew corpusTest`: it needs the network and is not part of `build`.
 * An app that can't be downloaded is skipped, not failed: that's not a detection regression.
 */
@Tag("corpus")
class CorpusTest {
    private val cache = Path.of(System.getProperty("corpus.cache", "build/corpus")).createDirectories()
    private val http = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NORMAL).build()
    private val analyzer = ApkAnalyzer()

    @TestFactory
    fun `real apps`(): List<DynamicTest> {
        val apps = Json.parseToJsonElement(javaClass.getResource("/corpus/apps.json")!!.readText()).jsonObject["apps"]!!.jsonArray
        val used = mutableSetOf<String>()
        return apps.map { it.jsonObject }.map { app ->
            dynamicTest(app.string("name")!!) {
                val apk = download(app["source"]!!.jsonObject)
                used += apk.name
                check(analyzer.analyze(apk), app["expect"]!!.jsonObject)
            }
        } + dynamicTest("remove APKs no longer in the corpus") { pruneCache(used) }
    }

    private fun check(
        report: ApkReport,
        expect: JsonObject,
    ) {
        val context = "evidence: ${report.evidence}, details: ${report.details}"
        assertNull(report.error, "analysis failed")
        assertEquals(expect.string("mainFramework"), report.mainFramework, context)
        if ("baseFramework" in expect) assertEquals(expect.string("baseFramework"), report.baseFramework, context)
        expect["details"]?.jsonObject?.forEach { (framework, facts) ->
            facts.jsonObject.forEach { (key, value) ->
                assertEquals(value.jsonPrimitive.content, report.details[framework]?.get(key), "$framework $key; $context")
            }
        }
    }

    /** The APK in the cache, downloading it if missing. Cached files are named after their URL. */
    private fun download(source: JsonObject): Path {
        val url = resolveUrl(source)
        val file = cache.resolve(sha256(url.toByteArray()).take(16) + "-" + url.substringAfterLast('/').take(80))
        if (!file.exists()) {
            val partial = Files.createTempFile(cache, "download", ".part")
            try {
                val response = http.send(request(url), HttpResponse.BodyHandlers.ofFile(partial))
                if (response.statusCode() != 200) abort<Unit>("HTTP ${response.statusCode()} downloading $url")
                Files.move(partial, file, StandardCopyOption.ATOMIC_MOVE)
            } catch (e: IOException) {
                abort<Unit>("could not download $url: $e")
            } finally {
                Files.deleteIfExists(partial)
            }
        }
        source.string("sha256")?.let { expected ->
            assertEquals(expected, file.inputStream().use { sha256(it.readAllBytes()) }, "SHA-256 of $url")
        }
        return file
    }

    private fun resolveUrl(source: JsonObject): String {
        source.string("url")?.let { return it }
        source.string("fdroid")?.let { pkg ->
            val code = getJson("https://f-droid.org/api/v1/packages/$pkg").string("suggestedVersionCode")
            return "https://f-droid.org/repo/${pkg}_$code.apk"
        }
        val repo = source.string("github") ?: error("unknown source $source")
        val release = source.string("tag")?.let { "tags/$it" } ?: "latest"
        val pattern = Regex(source.string("asset")!!)
        val assets = getJson("https://api.github.com/repos/$repo/releases/$release")["assets"]!!.jsonArray.map { it.jsonObject }
        val asset =
            assets.singleOrNull { pattern.matches(it.string("name")!!) }
                ?: abort("no single asset matching $pattern in $repo $release")
        return asset.string("browser_download_url")!!
    }

    private fun getJson(url: String): JsonObject {
        val response =
            try {
                http.send(request(url), HttpResponse.BodyHandlers.ofString())
            } catch (e: IOException) {
                abort("could not fetch $url: $e")
            }
        if (response.statusCode() != 200) abort<Unit>("HTTP ${response.statusCode()} fetching $url")
        return Json.parseToJsonElement(response.body()).jsonObject
    }

    private fun request(url: String): HttpRequest {
        val request = HttpRequest.newBuilder(URI(url)).header("User-Agent", "apk-category-checker-corpus")
        // Raises the GitHub API rate limit (60 requests per hour without a token).
        System.getenv("GITHUB_TOKEN")?.takeIf { url.startsWith("https://api.github.com/") }?.let {
            request.header("Authorization", "Bearer $it")
        }
        return request.build()
    }

    /** Old versions of the apps would otherwise pile up in the cache. */
    private fun pruneCache(used: Set<String>) {
        if (used.isEmpty()) return // nothing downloaded: keep the cache as it is
        cache.listDirectoryEntries("*.apk").filter { it.name !in used }.forEach(Files::delete)
    }

    private fun JsonObject.string(key: String): String? = this[key]?.takeIf { it != JsonNull }?.jsonPrimitive?.content

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it) }
}
