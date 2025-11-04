/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.run

import com.intellij.build.events.BuildEvent
import com.intellij.openapi.extensions.ExtensionPointName
import org.cangnova.cangjie.project.extension.ProjectBuildSystemId
import java.util.function.Consumer

/**
 * Extension point for parsing build system specific output.
 * Each build system (cjpm, cjc, etc.) can provide its own parser implementation.
 */
interface CangJieBuildOutputParser {

    /**
     * Get the build system ID this parser supports
     */
    fun getBuildSystemId(): ProjectBuildSystemId

    /**
     * Parse a line of build output
     *
     * @param line The output line to parse
     * @param context The build context
     * @param messageConsumer Consumer for build events
     * @return ParseResult indicating what was parsed
     */
    fun parseLine(
        line: String,
        context: CangJieBuildContext,
        messageConsumer: Consumer<in BuildEvent>
    ): ParseResult

    /**
     * Collect artifacts after build completion
     *
     * @param context The build context
     * @return The collected artifact, or null if none found
     */
    fun collectArtifacts(context: CangJieBuildContext): CompilerArtifact?

    /**
     * Parse error context (file location and error details)
     *
     * @param line The line containing error context
     * @param context The build context
     * @return ErrorContext if parsed successfully, null otherwise
     */
    fun parseErrorContext(line: String, context: CangJieBuildContext): ErrorContext?

    companion object {
        val EP_NAME = ExtensionPointName<CangJieBuildOutputParser>(
            "org.cangnova.cangjie.run.buildOutputParser"
        )

        /**
         * Find parser for the current build system
         */
        fun findParser(buildSystemId: String): CangJieBuildOutputParser? {
            return EP_NAME.extensionList.find { it.getBuildSystemId().id == buildSystemId }
        }
    }
}

/**
 * Result of parsing a line
 */
sealed class ParseResult {
    /** Line was successfully parsed and handled */
    object Handled : ParseResult()

    /** Line contains error message, start collecting error context */
    data class ErrorStart(val message: String) : ParseResult()

    /** Line is part of error context */
    object ErrorContext : ParseResult()

    /** End of error context */
    object ErrorEnd : ParseResult()

    /** Build finished successfully */
    object BuildSuccess : ParseResult()

    /** Build failed */
    data class BuildFailure(val taskName: String?) : ParseResult()

    /** Line was not recognized, pass through */
    object Unhandled : ParseResult()
}

/**
 * Error context information
 */
data class ErrorContext(
    val filePath: String,
    val fileName: String,
    val lineStart: Int,
    val columnStart: Int,
    val lineEnd: Int,
    val columnEnd: Int
)