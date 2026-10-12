// SPDX-License-Identifier: MIT
package com.apkcategorychecker.framework

import com.apkcategorychecker.analyzer.ApkAnalyzer
import com.apkcategorychecker.apk.syntheticApk
import com.apkcategorychecker.cli.run
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.CsvSource
import java.io.PrintWriter
import java.io.StringWriter
import java.nio.file.Path
import kotlin.io.path.writeText

class RulesTest {
    @TempDir
    lateinit var dir: Path

    @Test
    fun `built-in rules list the frameworks from generic to specific`() {
        val names = Rules.builtIn.frameworks.map { it.name }
        assertEquals(29, names.size)
        assertEquals("Apache Cordova", names.first())
        assertEquals(".NET MAUI", names.last())
        // Frameworks built on a runtime must come after it, to win as main framework.
        Rules.builtIn.frameworks.forEachIndexed { i, framework ->
            framework.runsOn.forEach { base -> assertTrue(names.indexOf(base) < i, "${framework.name} before $base") }
        }
        assertEquals(1, Rules.builtIn.nativeDetails.size)
    }

    @Test
    fun `custom rules detect custom frameworks`() {
        val rules =
            Rules.parse(
                """
                {
                  "schemaVersion": 1,
                  "frameworks": [
                    { "name": "Engine", "signals": [{ "nativeLib": "libengine.so" }] },
                    {
                      "name": "Toolkit",
                      "match": "all",
                      "signals": [{ "classPackage": "com.toolkit" }, { "anyOf": [{ "file": "assets/a" }, { "file": "assets/b" }] }],
                      "runsOn": ["Engine"],
                      "details": [{ "key": "mode", "options": [{ "value": "pro", "signal": { "fileUnder": "assets/pro/" } }] }]
                    }
                  ],
                  "nativeDetails": [{ "key": "kind", "options": [{ "value": "game", "signal": { "nativeLibRegex": "libgame.*\\.so" } }] }]
                }
                """.trimIndent(),
                "test",
            )
        val analyzer = ApkAnalyzer(rules)
        val apk = { name: String, files: Map<String, String>, classes: List<String> -> syntheticApk(dir.resolve(name), files, classes) }

        val toolkit =
            analyzer.analyze(
                apk("t.apk", mapOf("lib/x86/libengine.so" to "", "assets/b" to "", "assets/pro/x" to ""), listOf("com.toolkit.Main")),
            )
        assertEquals("Toolkit", toolkit.mainFramework)
        assertEquals("Engine", toolkit.baseFramework)
        assertEquals(mapOf("Toolkit" to mapOf("mode" to "pro")), toolkit.details)

        val native = analyzer.analyze(apk("n.apk", mapOf("lib/x86/libgame3d.so" to ""), listOf("com.toolkit.Main")))
        assertEquals("Native", native.mainFramework) // the toolkit misses assets/a or assets/b
        assertEquals(mapOf("Native" to mapOf("kind" to "game")), native.details)
    }

    @ParameterizedTest(name = "{0}")
    @CsvSource(
        delimiter = '|',
        value = [
            "not JSON                  | {                                                                          | test: invalid JSON",
            "schema version            | {\"schemaVersion\": 2, \"frameworks\": []}                                     | test: schemaVersion: unsupported schema version 2",
            "no frameworks             | {\"schemaVersion\": 1, \"frameworks\": []}                                     | test: frameworks: no frameworks",
            "unknown top-level field   | {\"schemaVersion\": 1, \"framework\": []}                                      | test: unknown field \"framework\"",
            "unknown framework field   | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signal\": []}]}          | test: frameworks[0]: unknown field \"signal\"",
            "missing name              | {\"schemaVersion\": 1, \"frameworks\": [{\"signals\": [{\"file\": \"a\"}]}]}        | test: frameworks[0]: missing \"name\"",
            "no signals                | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": []}]}         | test: frameworks[0].signals: no signals",
            "unknown signal            | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"clas\": \"x\"}]}]} | test: frameworks[0].signals[0]: unknown signal \"clas\"",
            "two signals in one        | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"file\": \"a\", \"className\": \"B\"}]}]} | test: frameworks[0].signals[0]: expected exactly one of",
            "extract without pattern  | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"file\": \"a\"}], \"details\": [{\"key\": \"v\", \"extract\": {\"file\": \"a\"}}]}]} | test: frameworks[0].details[0].extract: missing \"pattern\"",
            "extract without group    | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"file\": \"a\"}], \"details\": [{\"key\": \"v\", \"extract\": {\"file\": \"a\", \"pattern\": \"v1\"}}]}]} | test: frameworks[0].details[0].extract.pattern: the regular expression needs a group",
            "extract two sources      | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"file\": \"a\"}], \"details\": [{\"key\": \"v\", \"extract\": {\"file\": \"a\", \"nativeLib\": \"b\", \"pattern\": \"(v)\"}}]}]} | test: frameworks[0].details[0].extract: expected exactly one of",
            "two kinds of detail      | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"file\": \"a\"}], \"details\": [{\"key\": \"v\", \"manifestMetaData\": \"m\", \"extract\": {\"file\": \"a\", \"pattern\": \"(v)\"}}]}]} | test: frameworks[0].details[0]: expected exactly one of",
            "misplaced extension       | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"file\": \"a\", \"extension\": \".js\"}]}]} | test: frameworks[0].signals[0]: \"extension\" only applies to webAssetContains",
            "empty value               | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"file\": \"\"}]}]} | test: frameworks[0].signals[0].file: empty value",
            "bad regex                 | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"nativeLibRegex\": \"lib(.so\"}]}]} | test: frameworks[0].signals[0].nativeLibRegex: invalid regular expression",
            "bad match                 | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"match\": \"some\", \"signals\": [{\"file\": \"a\"}]}]} | test: frameworks[0].match: expected \"any\" or \"all\"",
            "unknown runsOn            | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"file\": \"a\"}], \"runsOn\": [\"B\"]}]} | test: frameworks[0].runsOn: unknown framework \"B\"",
            "duplicate name            | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"file\": \"a\"}]}, {\"name\": \"A\", \"signals\": [{\"file\": \"b\"}]}]} | test: frameworks: duplicate framework \"A\"",
            "detail without signal     | {\"schemaVersion\": 1, \"frameworks\": [{\"name\": \"A\", \"signals\": [{\"file\": \"a\"}], \"details\": [{\"key\": \"k\", \"options\": [{\"value\": \"v\"}]}]}]} | test: frameworks[0].details[0].options[0]: missing \"signal\"",
        ],
    )
    fun `invalid rules are rejected with the location of the error`(
        @Suppress("UNUSED_PARAMETER") case: String,
        json: String,
        message: String,
    ) {
        val error = assertThrows<RulesException> { Rules.parse(json, "test") }
        assertTrue(error.message!!.startsWith(message), "\"${error.message}\" should start with \"$message\"")
    }

    @Test
    fun `the command line uses custom rules and prints the built-in ones`() {
        val apk = syntheticApk(dir.resolve("app.apk"), mapOf("lib/x86/libengine.so" to ""))
        val rules =
            dir.resolve("rules.json").apply {
                writeText("""{"schemaVersion": 1, "frameworks": [{"name": "Engine", "signals": [{"nativeLib": "libengine.so"}]}]}""")
            }
        val out = StringWriter()
        assertEquals(
            0,
            run(arrayOf(apk.toString(), "--rules", rules.toString(), "-f", "jsonl"), PrintWriter(out, true), PrintWriter(StringWriter())),
        )
        assertTrue("\"mainFramework\":\"Engine\"" in out.toString(), out.toString())

        val printed = StringWriter()
        assertEquals(0, run(arrayOf("--print-rules"), PrintWriter(printed, true), PrintWriter(StringWriter())))
        assertEquals(Rules.builtInText, printed.toString())
        assertEquals(Rules.builtIn.frameworks.map { it.name }, Rules.parse(printed.toString(), "printed").frameworks.map { it.name })

        val err = StringWriter()
        val broken = dir.resolve("broken.json").apply { writeText("""{"schemaVersion": 1, "frameworks": [{"name": "A"}]}""") }
        assertEquals(2, run(arrayOf(apk.toString(), "--rules", broken.toString()), PrintWriter(StringWriter()), PrintWriter(err, true)))
        assertTrue("frameworks[0]: expected an array" in err.toString() || "signals: expected an array" in err.toString(), err.toString())
        assertEquals(
            2,
            run(
                arrayOf(apk.toString(), "--rules", dir.resolve("missing.json").toString()),
                PrintWriter(StringWriter()),
                PrintWriter(StringWriter()),
            ),
        )
    }
}
