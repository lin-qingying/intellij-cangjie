/*
 * Copyright 2026 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.cjpm.build

import com.intellij.build.events.BuildEvent
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputTypes
import org.cangnova.cangjie.cjpm.project.CjpmBuildSystemId
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import org.cangnova.cangjie.project.service.CjProjectsService
import org.cangnova.cangjie.run.*
import java.nio.file.Paths
import java.util.function.Consumer

/**
 * CJPM specific build output parser.
 * Handles parsing of cjpm build command output.
 */
class CjpmBuildOutputParser : CangJieBuildOutputParser {

    private val decoder = AnsiEscapeDecoder()

    override fun getBuildSystemId(): ProjectBuildSystemId = CjpmBuildSystemId

    override fun parseLine(
        line: String,
        context: CangJieBuildContext,
        messageConsumer: Consumer<in BuildEvent>
    ): ParseResult {
        val cleanLine = removeEscapeSequences(line)

        return when {
            // Error message start
            cleanLine.startsWith("error:") -> {
                val message = cleanLine.substringAfter(":").trim()
                ParseResult.ErrorStart(message, isWarning = false)
            }

            // Warning message
            cleanLine.startsWith("warning:") -> {
                val message = cleanLine.substringAfter(":").trim()
                ParseResult.ErrorStart(message, isWarning = true)
            }

            // Error file location: " ==> path/to/file.cj:4:27:"
            cleanLine.startsWith(" ==>") -> {
                ParseResult.ErrorContext
            }

            // Error context lines: "42 |" or "  |"
            isContextLine(cleanLine) -> {
                ParseResult.ErrorContext
            }

            // Error/warning summary line marks end of all errors
            // Matches: "1 error generated, 1 error printed."
            //          "2 errors generated, 2 errors printed."
            //          "1 warning generated, 1 warning printed."
            cleanLine.matches(Regex("""\d+ (error|warning)s? generated.*""")) -> {
                ParseResult.ErrorEnd
            }

            // Build success
            cleanLine.startsWith("cjpm build success") -> {
                ParseResult.BuildSuccess
            }

            // Build failed
            cleanLine.startsWith("Error: cjpm build failed") -> {
                ParseResult.BuildFailure("cjpm")
            }

            // Empty line - could be end of error or just spacing
            cleanLine.isEmpty() -> {
                ParseResult.Unhandled
            }

            else -> ParseResult.Unhandled
        }
    }

    override fun parseErrorContext(line: String, context: CangJieBuildContext): ErrorContext? {
        // Parse file location: " ==> D:\Code\Cj\ideatest\ideatest\src\main.cj:4:27:"
        val regex = Regex("""(?<filePath>.*):(?<lineNumber>\d+):(?<columnNumber>\d+):""")
        val result = regex.find(line) ?: return null

        var filePath = result.groups["filePath"]?.value ?: ""
        if (filePath.startsWith(" ==>")) {
            filePath = filePath.substring(4)
        }
        filePath = filePath.trim()

        val fileName = Paths.get(filePath).fileName.toString()
        val lineStart = result.groups["lineNumber"]?.value?.toIntOrNull() ?: 0
        val columnStart = result.groups["columnNumber"]?.value?.toIntOrNull() ?: 0

        return ErrorContext(
            filePath = filePath,
            fileName = fileName,
            lineStart = lineStart,
            columnStart = columnStart,
            lineEnd = lineStart,
            columnEnd = columnStart + 3
        )
    }

    override fun collectArtifacts(context: CangJieBuildContext): CompilerArtifact? {
        val project = context.environment.project
        val cjProjectsService = CjProjectsService.getInstance(project)
        val cjProject = cjProjectsService.cjProject

        // Try to find the main module
        val module = if (cjProject.isWorkspace) {
            cjProject.workspace?.modules?.firstOrNull()
        } else {
            cjProject.module
        } ?: return null

        val moduleName = module.name
        val profile = detectBuildProfile(context)

        // Collect all artifact files
        val artifactFiles = mutableListOf<ArtifactFile>()
        val executables = mutableListOf<String>()

        // 1. Collect .cjo files from output directories (compilation output)
        for (sourceSet in module.sourceSets) {
            for (outputDir in sourceSet.outputDirectory) {
                val dirFile = java.io.File(outputDir.path)
                if (dirFile.exists() && dirFile.isDirectory) {
                    // Skip .bin-cache directories
                    if (dirFile.name == ".bin-cache" || dirFile.absolutePath.contains(".bin-cache")) {
                        continue
                    }
                    // Check if this directory matches the build profile
                    if (matchesBuildProfile(dirFile, profile)) {
                        // Collect .cjo files
                        dirFile.walkTopDown()
                            .filter { it.isFile && it.extension == "cjo" }
                            .forEach { file ->
                                artifactFiles.add(ArtifactFile(file.absolutePath, ArtifactFileType.OBJECT))
                            }

                        // Collect library files
                        dirFile.walkTopDown()
                            .filter { it.isFile && (it.extension == "a" || it.extension == "so" || it.extension == "dll" || it.extension == "dylib") }
                            .forEach { file ->
                                artifactFiles.add(ArtifactFile(file.absolutePath, ArtifactFileType.LIBRARY))
                            }
                    }
                }
            }
        }

        // 2. Find executable files (usually in bin directory)
        val executableDir = findExecutableDirectory(context, profile)
        if (executableDir != null) {
            // Try multiple possible executable names
            val possibleNames = listOf(
                moduleName,           // Module name
                "main",              // Common default name
                module.name          // Explicit module name
            ).distinct()

            for (name in possibleNames) {
                val executablePath = findExecutableInDirectory(executableDir.absolutePath, name)
                if (executablePath != null && executablePath !in executables) {
                    executables.add(executablePath)
                    artifactFiles.add(ArtifactFile(executablePath, ArtifactFileType.EXECUTABLE))
                    break  // Found one, stop searching
                }
            }

            // If still not found, collect all executables in the directory
            if (executables.isEmpty()) {
                val allExecutables = findAllExecutablesInDirectory(executableDir)
                executables.addAll(allExecutables)
                allExecutables.forEach { path ->
                    artifactFiles.add(ArtifactFile(path, ArtifactFileType.EXECUTABLE))
                }
            }
        }

        return CompilerArtifact(
            name = moduleName,
            executables = executables,
            profile = profile,
            files = artifactFiles
        )
    }

    /**
     * Check if line is an empty context line (like "  |" with only whitespace after |)
     * This marks the end of an error block
     */
    private fun isEmptyContextLine(line: String): Boolean {
        // Match lines like "  |" or "  |   " (pipe with only whitespace before and after)
        val regex = Regex("""\s*\|\s*""")
        return regex.matches(line)
    }

    /**
     * Check if line is an error context line (like "42 |" or "  |")
     */
    private fun isContextLine(line: String): Boolean {
        val regex = Regex("""(\d+ |  )\|.*""")
        return regex.matches(line)
    }

    /**
     * Remove ANSI escape sequences from text
     */
    private fun removeEscapeSequences(text: String): String {
        val chunks = mutableListOf<String>()
        decoder.escapeText(text, ProcessOutputTypes.STDOUT) { chunk, _ ->
            chunks.add(chunk)
        }
        return chunks.joinToString("")
    }

    /**
     * Get build profile from context
     * The build profile is determined by the runner/executor that initiated the build
     */
    private fun detectBuildProfile(context: CangJieBuildContext): BuildProfile {
        // Use the build profile from context, which is set by the runner
        return context.buildProfile
    }

    /**
     * Find the output directory based on build profile
     */
    private fun findOutputDirectory(context: CangJieBuildContext, profile: BuildProfile): java.io.File? {
        val project = context.environment.project
        val cjProjectsService = CjProjectsService.getInstance(project)
        val cjProject = cjProjectsService.cjProject

        // Try to find the main module
        val module = if (cjProject.isWorkspace) {
            cjProject.workspace?.modules?.firstOrNull()
        } else {
            cjProject.module
        }

        // First, try to get output directory from project model
        if (module != null) {
            for (sourceSet in module.sourceSets) {
                for (outputDir in sourceSet.outputDirectory) {
                    val dirFile = java.io.File(outputDir.path)
                    if (dirFile.exists() && dirFile.isDirectory) {
                        // Skip .bin-cache directories
                        if (dirFile.name == ".bin-cache" || dirFile.absolutePath.contains(".bin-cache")) {
                            continue
                        }
                        // Check if this directory matches the build profile
                        if (matchesBuildProfile(dirFile, profile)) {
                            return dirFile
                        }
                    }
                }
            }
        }

        // Fallback: try common cjpm output directory patterns
        val profileDir = when (profile) {
            BuildProfile.DEBUG -> "debug"
            BuildProfile.RELEASE -> "release"
        }

        val candidates = listOf(
            context.workingDirectory.resolve("target").resolve(profileDir).resolve("bin"),
            context.workingDirectory.resolve("target").resolve(profileDir),
        )

        for (dir in candidates) {
            val dirFile = dir.toFile()
            if (dirFile.exists() && dirFile.isDirectory) {
                // Skip .bin-cache directories
                if (dirFile.name == ".bin-cache" || dirFile.absolutePath.contains(".bin-cache")) {
                    continue
                }
                return dirFile
            }
        }

        return null
    }

    /**
     * Check if the directory matches the build profile
     */
    private fun matchesBuildProfile(dir: java.io.File, profile: BuildProfile): Boolean {
        val path = dir.absolutePath.lowercase()
        return when (profile) {
            BuildProfile.DEBUG -> path.contains("debug")
            BuildProfile.RELEASE -> path.contains("release")
        }
    }

    /**
     * Find the directory containing executable files (usually bin directory)
     */
    private fun findExecutableDirectory(context: CangJieBuildContext, profile: BuildProfile): java.io.File? {
        val profileDir = when (profile) {
            BuildProfile.DEBUG -> "debug"
            BuildProfile.RELEASE -> "release"
        }

        // Try common executable directory patterns for cjpm
        val candidates = listOf(
            context.workingDirectory.resolve("target").resolve(profileDir).resolve("bin"),
            context.workingDirectory.resolve("target").resolve("bin"),
            context.workingDirectory.resolve("bin"),
        )

        for (dir in candidates) {
            val dirFile = dir.toFile()
            if (dirFile.exists() && dirFile.isDirectory) {
                // Skip .bin-cache directories
                if (dirFile.name == ".bin-cache" || dirFile.absolutePath.contains(".bin-cache")) {
                    continue
                }
                return dirFile
            }
        }

        return null
    }

    /**
     * Find executable file in the given directory by name
     */
    private fun findExecutableInDirectory(directoryPath: String, executableName: String): String? {
        val dir = java.io.File(directoryPath)
        if (!dir.exists() || !dir.isDirectory) {
            return null
        }

        // Skip .bin-cache directories
        if (dir.name == ".bin-cache" || dir.absolutePath.contains(".bin-cache")) {
            return null
        }

        // Try exact name
        val exactFile = java.io.File(dir, executableName)
        if (exactFile.exists() && isExecutable(exactFile)) {
            return exactFile.absolutePath
        }

        // Try with .exe extension on Windows
        if (com.intellij.openapi.util.SystemInfo.isWindows) {
            val exeFile = java.io.File(dir, "$executableName.exe")
            if (exeFile.exists() && isExecutable(exeFile)) {
                return exeFile.absolutePath
            }
        }

        return null
    }

    /**
     * Find all executable files in the given directory
     */
    private fun findAllExecutablesInDirectory(dir: java.io.File): List<String> {
        if (!dir.exists() || !dir.isDirectory) {
            return emptyList()
        }

        // Skip .bin-cache directories
        if (dir.name == ".bin-cache" || dir.absolutePath.contains(".bin-cache")) {
            return emptyList()
        }

        val executables = dir.listFiles { file ->
            file.isFile && isExecutable(file)
        }

        return executables?.map { it.absolutePath } ?: emptyList()
    }

    /**
     * Check if a file is executable
     */
    private fun isExecutable(file: java.io.File): Boolean {
        return file.canExecute() ||
            (com.intellij.openapi.util.SystemInfo.isWindows && file.name.endsWith(".exe"))
    }
}