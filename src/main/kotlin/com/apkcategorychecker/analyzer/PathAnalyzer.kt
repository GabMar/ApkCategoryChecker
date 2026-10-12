// SPDX-License-Identifier: GPL-3.0-or-later
package com.apkcategorychecker.analyzer

import com.apkcategorychecker.cli.CommandLineInterface
import com.apkcategorychecker.framework.Frameworks
import com.apkcategorychecker.json.FactoryJsCSVBuilder
import com.apkcategorychecker.writer.FactoryWriter
import com.apkcategorychecker.writer.Writer
import java.io.File
import java.nio.file.Path
import java.text.SimpleDateFormat
import java.util.Date

/** Analyzes the APK or the directory of APKs given on the command line and writes the result files. */
class PathAnalyzer {

    fun analyze() {
        val cli = CommandLineInterface.getInstance()
        val writer = FactoryWriter.getInstance().getWriter(cli.writerFormat)
        val time = SimpleDateFormat("yyyy.MM.dd HH.mm.ss").format(Date())
        val csvPath = createResultFile(writer, cli.outDir, time)

        val analyzer = ApkAnalyzer(Frameworks.all(cli.deep))
        val jsFiles = ArrayList<JsElement>()
        findApks(File(cli.path)).forEachIndexed { counter, apk ->
            val analysis = analyzer.analyze(apk)
            writer.Write(analysis.result, csvPath, counter)
            jsFiles += analysis.jsFiles
        }

        FactoryJsCSVBuilder.getInstance().jsCSVBuilder.BuildJs(jsFiles, cli.outDir, time)
    }

    /** The APK itself, or all the APKs found recursively in a directory, in a stable order. */
    private fun findApks(root: File): List<Path> =
        root.walkTopDown()
            .filter { it.isFile && it.name.endsWith(".apk", ignoreCase = true) }
            .map { it.toPath() }
            .sorted()
            .toList()

    private fun createResultFile(writer: Writer, destination: String, time: String): String {
        val dir = File(destination).apply { if (!exists()) mkdir() }
        val baseDir = if (dir.isDirectory) destination else destination.dropLast(4)
        val resultFile = File("$baseDir/Results_$time.csv").absolutePath
        writer.createHeader(resultFile)
        return resultFile
    }
}
