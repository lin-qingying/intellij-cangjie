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

package org.cangnova.cangjie.run

import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.BuildEventsNls
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.StartEvent
import com.intellij.build.events.impl.FailureResultImpl
import com.intellij.build.events.impl.SkippedResultImpl
import com.intellij.build.events.impl.SuccessResultImpl
import com.intellij.build.output.BuildOutputInstantReader
import com.intellij.build.output.BuildOutputParser
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import org.cangnova.cangjie.project.CjProjectBundle
import org.cangnova.cangjie.run.compat.createFinishEvent
import org.cangnova.cangjie.run.compat.createFileMessageEvent
import org.cangnova.cangjie.run.compat.createOutputBuildEvent
import org.cangnova.cangjie.run.compat.createStartEvent
import org.jetbrains.annotations.Nls
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer

/**
 * Converter for CangJie build events.
 * Parses build output and converts it to IntelliJ Build Tool Window events.
 * Delegates build system specific parsing to CangJieBuildOutputParser extensions.
 */
class CangJieBuildEventsConverter(private val context: CangJieBuildContext) : BuildOutputParser {

    private val strBuffer: StringBuilder = StringBuilder()
    private val decoder: AnsiEscapeDecoder = AnsiEscapeDecoder()
    private val startEvents: MutableList<StartEvent> = mutableListOf()
    private val messageEvents: MutableSet<MessageEvent> = hashSetOf()

    private var errorMessage: String = ""
    private var errorContext: ErrorContext? = null
    private var kind: MessageEvent.Kind = MessageEvent.Kind.SIMPLE
    private var isRecordError: Boolean = false

    // Get the build system specific parser
    private val buildSystemParser: CangJieBuildOutputParser? by lazy {
        val buildSystemId = org.cangnova.cangjie.project.service.CjProjectBuildSystemService
            .getInstance()
            .getBuildSystem()?.id
        buildSystemId?.let { CangJieBuildOutputParser.findParser(it) }
    }

    override fun parse(
        line: String,
        reader: BuildOutputInstantReader,
        messageConsumer: Consumer<in BuildEvent>
    ): Boolean {
        val cleanLine = decoder.removeEscapeSequences(line)

        // Delegate to build system specific parser if available
        val parser = buildSystemParser
        if (parser != null) {
            when (val result = parser.parseLine(line, context, messageConsumer)) {
                is ParseResult.ErrorStart -> {
                    // If we're already recording an error, emit it first
                    if (isRecordError && strBuffer.isNotEmpty()) {
                        emitMessageEvent(messageConsumer)
                        strBuffer.clear()
                        errorContext = null
                    }
                    // Start recording new error or warning
                    errorMessage = result.message
                    isRecordError = true
                    strBuffer.append(line.withNewLine())
                    kind = if (result.isWarning) MessageEvent.Kind.WARNING else MessageEvent.Kind.ERROR
                }

                is ParseResult.ErrorContext -> {
                    strBuffer.append(line.withNewLine())
                    // Try to parse error context
                    val ctx = parser.parseErrorContext(cleanLine, context)
                    if (ctx != null) {
                        errorContext = ctx
                    }
                }

                is ParseResult.ErrorEnd -> {
                    if (strBuffer.isNotEmpty()) {
                        emitMessageEvent(messageConsumer)
                    }
                    isRecordError = false
                    strBuffer.clear()
                    errorContext = null
                }

                is ParseResult.BuildSuccess -> {
                    handleFinishedMessage(null, messageConsumer)
                    // Collect artifacts using parser
                    val artifact = parser.collectArtifacts(context)
                    if (artifact != null) {
                        context.artifacts += artifact
                    }
                }

                is ParseResult.BuildFailure -> {
                    handleFinishedMessage(result.taskName, messageConsumer)
                }

                is ParseResult.Handled -> {
                    // Line was handled by parser, just output to console
                }

                is ParseResult.Unhandled -> {
                    // If we're recording an error and encounter an unhandled line,
                    // it means the error block has ended
                    if (isRecordError && strBuffer.isNotEmpty()) {
                        emitMessageEvent(messageConsumer)
                        isRecordError = false
                        strBuffer.clear()
                        errorContext = null
                    }
                    // Line not recognized, output to console
                }
            }

            // Always output the line to the console
            messageConsumer.acceptText(context.parentId ?: context.buildId, line.withNewLine())
            return true
        }

        // Fallback: no parser available, just output to console
        messageConsumer.acceptText(context.parentId ?: context.buildId, line.withNewLine())
        return true
    }


    private fun emitMessageEvent(messageConsumer: Consumer<in BuildEvent>) {
        // Get module name from build system or use generic name
        val parentEventId = buildSystemParser?.getBuildSystemId()?.id ?: "build"

        val filePosition = getFilePosition(errorContext)
        val messageEvent = createMessageEvent(
            context.workingDirectory,
            parentEventId,
            kind,
            errorMessage,
            strBuffer.toString(),
            filePosition
        )

        if (messageEvents.add(messageEvent)) {
            // Ensure parent event exists
            if (startEvents.none { it.id == parentEventId }) {
                handleCompilingMessage(
                    CjProjectBundle.message("run.configuration.build.compiling", parentEventId),
                    false,
                    messageConsumer
                )
            }

            messageConsumer.accept(messageEvent)

            // Update error/warning counters
            if (kind == MessageEvent.Kind.ERROR) {
                context.errors.incrementAndGet()
            } else {
                context.warnings.incrementAndGet()
            }
        }
    }

    private fun getFilePosition(errorContext: ErrorContext?): FilePosition {
        if (errorContext == null) {
            return FilePosition(context.workingDirectory.toFile(), 0, 0, 0, 0)
        }

        val filePath = run {
            var path = Paths.get(errorContext.fileName)
            if (!path.isAbsolute) {
                path = Paths.get(errorContext.filePath)
            }
            path
        }

        return FilePosition(
            filePath.toFile(),
            errorContext.lineStart - 1,
            errorContext.columnStart - 1,
            errorContext.lineEnd - 1,
            errorContext.columnEnd - 1
        )
    }


    private fun handleCompilingMessage(
        @BuildEventsNls.Message originalMessage: String,
        isUpToDate: Boolean,
        messageConsumer: Consumer<in BuildEvent>
    ) {
        val message = originalMessage.substringBefore("(").trimEnd()
        val eventId = message.substringAfter(" ").replace(" v", " ")
        val startEvent = createStartEvent(
            id = eventId,
            parentId = context.parentId ?: context.buildId,
            eventTime = System.currentTimeMillis(),
            message = message
        )
        messageConsumer.accept(startEvent)

        if (isUpToDate) {
            val finishEvent = createFinishEvent(
                id = eventId,
                parentId = context.parentId ?: context.buildId,
                eventTime = System.currentTimeMillis(),
                message = message,
                result = SuccessResultImpl(isUpToDate)
            )
            messageConsumer.accept(finishEvent)
        } else {
            startEvents.add(startEvent)
        }
    }

    private fun handleFinishedMessage(failedTaskName: String?, messageConsumer: Consumer<in BuildEvent>) {
        for (startEvent in startEvents) {
            val finishEvent = createFinishEvent(
                id = startEvent.id,
                parentId = context.parentId ?: context.buildId,
                eventTime = System.currentTimeMillis(),
                message = startEvent.message,
                result = when (failedTaskName) {
                    startEvent.taskName -> FailureResultImpl(null as Throwable?)
                    null -> SuccessResultImpl()
                    else -> SkippedResultImpl()
                }
            )
            messageConsumer.accept(finishEvent)
        }
        startEvents.clear()
    }

    private fun Consumer<in BuildEvent>.acceptText(parentId: Any, @BuildEventsNls.Message text: String) =
        accept(createOutputBuildEvent(parentId, text, true))

    companion object {
        private val ERROR_OR_WARNING: List<MessageEvent.Kind> =
            listOf(MessageEvent.Kind.ERROR, MessageEvent.Kind.WARNING)

        private val StartEvent.taskName: String?
            get() = (id as? String)?.substringBefore(" ")?.substringBefore("(")?.trimEnd()

        private fun createMessageEvent(
            workingDirectory: Path,
            parentEventId: Any,
            kind: MessageEvent.Kind,
            @BuildEventsNls.Message message: String,
            @Nls detailedMessage: String?,
            filePosition: FilePosition? = null
        ): MessageEvent = createFileMessageEvent(
            parentId = parentEventId,
            kind = kind,
            group = CjProjectBundle.message("run.configuration.build.compiler"),
            message = message,
            detailedMessage = detailedMessage,
            filePosition = filePosition ?: FilePosition(workingDirectory.toFile(), 0, 0)
        )
    }
}

/**
 * Extension function to remove ANSI escape sequences from text
 */
fun AnsiEscapeDecoder.removeEscapeSequences(text: String): String {
    val chunks = mutableListOf<String>()
    escapeText(text, ProcessOutputTypes.STDOUT) { chunk, _ ->
        chunks.add(chunk)
    }
    return chunks.joinToString("")
}

/**
 * Extension function to capitalize a string
 */
@NlsActions.ActionText
fun String.capitalized(): String = StringUtil.capitalize(this)

/**
 * Extension function to add newline if not present
 */
@NlsSafe
private fun String.withNewLine(): String = if (StringUtil.endsWithLineBreak(this)) this else this + '\n'