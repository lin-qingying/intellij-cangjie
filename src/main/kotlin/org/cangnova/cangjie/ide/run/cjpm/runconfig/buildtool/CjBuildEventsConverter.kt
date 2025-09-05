/*
 * Copyright 2024 LinQingYing. and contributors.
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

package org.cangnova.cangjie.ide.run.cjpm.runconfig.buildtool

import org.cangnova.cangjie.ide.run.cjpm.CompilerArtifactMessage
import org.cangnova.cangjie.ide.run.cjpm.runconfig.CjAnsiEscapeDecoder.Companion.quantizeAnsiColors
import org.cangnova.cangjie.messages.CangJieBundle
import com.intellij.build.FilePosition
import com.intellij.build.events.BuildEvent
import com.intellij.build.events.BuildEventsNls
import com.intellij.build.events.MessageEvent
import com.intellij.build.events.StartEvent
import com.intellij.build.events.impl.*
import com.intellij.build.output.BuildOutputInstantReader
import com.intellij.build.output.BuildOutputParser
import com.intellij.execution.process.AnsiEscapeDecoder
import com.intellij.execution.process.ProcessOutputTypes
import com.intellij.openapi.util.NlsActions
import com.intellij.openapi.util.NlsSafe
import com.intellij.openapi.util.text.StringUtil
import org.jetbrains.annotations.Nls
import java.nio.file.Path
import java.nio.file.Paths
import java.util.function.Consumer


class CjBuildEventsConverter(private val context: CjpmBuildContextBase) : BuildOutputParser {


    private val strBuffer: StringBuilder = StringBuilder()
    private val decoder: AnsiEscapeDecoder = AnsiEscapeDecoder()
    private val startEvents: MutableList<StartEvent> = mutableListOf()
    private val messageEvents: MutableSet<MessageEvent> = hashSetOf()

    private var errorMessage: String = ""
    private var span: Span? = null

    private var kind: MessageEvent.Kind = MessageEvent.Kind.SIMPLE
//    正则匹配  42 |
//    或    |
//    | 后面可以为任意字符

    val regex = Regex("""(\d+ |  )\|.*""")

    fun isMatch(line: String): Boolean {
        return regex.matches(line)
    }

    //    是否要记录单元粗错误信息
    private var isRecordError: Boolean = false
    override fun parse(
        line: String,
        reader: BuildOutputInstantReader,
        messageConsumer: Consumer<in BuildEvent>
    ): Boolean {

//        如果line包含1 error generated, 1 error printed. 获取数字
        val errorGenerated = Regex("""(\d+) error generated, (\d+) error printed.""").find(line)

        val cleanLine = decoder.removeEscapeSequences(line)


        if (strBuffer.isEmpty() && !isRecordError) {
            return tryHandleCjpmMessage(quantizeAnsiColors(line), messageConsumer)

        }
//        如果开头是 ==> 说明是错误的文件

        when {
            cleanLine.startsWith(" ==>") -> {

                strBuffer.append(line.withNewLine())

                val regex = Regex("""(?<filePath>.*):(?<lineNumber>\d+):(?<columnNumber>\d+):""")
                val result = regex.find(cleanLine)

                if (result != null) {
                    var file_path = result.groups["filePath"]?.value ?: ""
//                  如果开头是 ==> 就去掉
                    if (file_path.startsWith(" ==>")) {
                        file_path = file_path.substring(4)
                    }
                    file_path = file_path.trim()

                    val file_name = Paths.get(file_path).fileName.toString()
                    val line_start = result.groups["lineNumber"]?.value?.toIntOrNull() ?: 0
                    val column_start = result.groups["columnNumber"]?.value?.toIntOrNull() ?: 0
                    span = Span(file_path, file_name, line_start, column_start, line_start, column_start + 3)


                }

            }

            isMatch(cleanLine) -> {
//                 如果是单元粗错误信息，添加到strBuffer中
                strBuffer.append(line.withNewLine())


            }

            else -> {

                val parentEventId = context.project.name

                val filePosition = getFilePosition(span)

                val messageEvent = createMessageEvent(
                    context.workingDirectory,
                    parentEventId,
                    kind,
                    errorMessage,
                    strBuffer.toString(),
                    filePosition
                )
                if (messageEvents.add(messageEvent)) {
                    if (startEvents.none { it.id == parentEventId }) {
                        handleCompilingMessage(
                            CangJieBundle.message(
                                "build.event.message.compiling.0",
                                parentEventId
                            ), false, messageConsumer
                        )
                    }

                    messageConsumer.accept(messageEvent)

                    if (kind == MessageEvent.Kind.ERROR) {
                        context.errors.incrementAndGet()
                    } else {
                        context.warnings.incrementAndGet()
                    }

                }

                isRecordError = false


                strBuffer.clear()

                messageConsumer.acceptText(context.parentId, "".withNewLine())
            }
        }


        if (cleanLine.startsWith("error")) {
            return tryHandleCjpmMessage(quantizeAnsiColors(line), messageConsumer)
        }
        messageConsumer.acceptText(context.parentId, line.withNewLine())




        return true


    }

    private fun getFilePosition(span: Span?): FilePosition {
        if (span == null) {
            return FilePosition(
                context.workingDirectory.toFile(),
                0,
                0,
                0,
                0
            )
        }
        val filePath = run {
            var filePath = Paths.get(span.file_name)
            if (!filePath.isAbsolute) {
                filePath = Paths.get(span.file_path)
            }
            filePath
        }
        return FilePosition(
            filePath.toFile(),
            span.line_start - 1,
            span.column_start - 1,
            span.line_end - 1,
            span.column_end - 1
        )
    }

    private val ERROR_OR_WARNING: List<MessageEvent.Kind> =
        listOf(MessageEvent.Kind.ERROR, MessageEvent.Kind.WARNING)

//    private fun handleProgressMessage(message: String, messageConsumer: Consumer<in BuildEvent>) {
//
//        fun finishNonActiveTasks(activeTaskNames: List<String>) {
//            val startEventsIterator = startEvents.iterator()
//            while (startEventsIterator.hasNext()) {
//                val startEvent = startEventsIterator.next()
//                val taskName = startEvent.taskName
//                if (taskName !in activeTaskNames) {
//                    startEventsIterator.remove()
//                    val finishEvent = FinishEventImpl(
//                        startEvent.id,
//                        context.parentId,
//                        System.currentTimeMillis(),
//                        startEvent.message,
//                        SuccessResultImpl()
//                    )
//                    messageConsumer.accept(finishEvent)
//                }
//            }
//        }
//
//        fun parseProgress(message: String): Progress {
//            val result = PROGRESS_TOTAL_RE.find(message)?.destructured
//            val current = result?.component1()?.toLongOrNull() ?: -1
//            val total = result?.component2()?.toLongOrNull() ?: -1
//            return Progress(current, total)
//        }
//
//        fun ProgressIndicator.update(title: @Nls String, description: @Nls String, progress: Progress) {
//            isIndeterminate = progress.total < 0
//            text = title
//            text2 = description
//            fraction = progress.fraction
//        }
//
//        val activeTaskNames = message
//            .substringAfter(":")
//            .split(",")
//            .map { it.substringBefore("(").trim() }
//        finishNonActiveTasks(activeTaskNames)
//
//        context.indicator?.update(context.progressTitle, message.substringAfter(":").trim(), parseProgress(message))
//    }

    private fun tryHandleCjpmMessage(@NlsSafe line: String, messageConsumer: Consumer<in BuildEvent>): Boolean {
        val cleanLine = decoder.removeEscapeSequences(line)
        if (cleanLine.isEmpty()) return true

        kind = getMessageKind(cleanLine.substringBefore(":"))
        @Suppress("HardCodedStringLiteral") val message = cleanLine
            .let { if (kind in ERROR_OR_WARNING) it.substringAfter(":") else it }
            .removePrefix(CangJieBundle.message("build.event.message.internal.compiler.error"))
            .trim()
            .capitalized()
            .trimEnd('.')


        if (kind == MessageEvent.Kind.ERROR) {
            errorMessage = message
            strBuffer.append(line.withNewLine())
            isRecordError = true
        }
        when {
            cleanLine.startsWith("Error: cjpm build failed")
                    || cleanLine.startsWith("Error: cjpm test failed") -> {
                val taskName = "cjpm"
                handleFinishedMessage(taskName, messageConsumer)
            }

            cleanLine.startsWith("cjpm build success") || cleanLine.startsWith("cjpm test success") -> {

                handleFinishedMessage(null, messageConsumer)


//                val executable = if(context.environment.)

                context.artifacts += CompilerArtifactMessage(
                    CjpmBuildManager.getExecutable(
                        context ,
                        context.buildId.toString()
                    )
                )


            }

            kind in ERROR_OR_WARNING -> {
                errorMessage = message
//                strBuffer.append(line.withNewLine())
                isRecordError = true

//        handleProblemMessage(kind, message, null, messageConsumer)
            }

        }

//        when{
//            kind == MessageEvent.Kind.ERROR -> {
////        error: expected ';' or '<NL>', found literal '1'
//// ==> D:\Code\Cj\ideatest\ideatest\src\main.cj:4:27:
////  |
////4 |     println("hello world")1
////  |                           ^ expected ';' or '<NL>' here
////  |
////
////1 error generated, 1 error printed.
////Error: cjpm build failed
////                记录错误信息，并添加到strBuffer中  line是一行一行的，并不是所有
////                //        error: expected ';' or '<NL>', found literal '1'
////// ==> D:\Code\Cj\ideatest\ideatest\src\main.cj:4:27:
//////  |
//////4 |     println("hello world")1
//////  |                           ^ expected ';' or '<NL>' here
//////  |为一个单元粗错误信息
//
//                isRecordError = true
//
//
//
//            }
//            else -> {
//                messageConsumer.acceptText(context.parentId, line.withNewLine())
//            }
//        }

//        when {
//
//            kind in ERROR_OR_WARNING ->
//                handleProblemMessage(kind, message, line, messageConsumer)
//        }
        messageConsumer.acceptText(context.parentId, line.withNewLine())
        return true
    }

    private fun Consumer<in BuildEvent>.acceptText(parentId: Any?, @BuildEventsNls.Message text: String) =
        accept(OutputBuildEventImpl(parentId, text, true))

    @NlsSafe
    private fun String.withNewLine(): String = if (StringUtil.endsWithLineBreak(this)) this else this + '\n'

//    private fun handleProblemMessage(
//        kind: MessageEvent.Kind,
//        @BuildEventsNls.Message message: String,
//        @Nls detailedMessage: String?,
//        messageConsumer: Consumer<in BuildEvent>
//    ) {
//
//        val messageEvent =
//            createMessageEvent(context.workingDirectory, context.parentId, kind, message, detailedMessage)
//        if (messageEvents.add(messageEvent)) {
//            messageConsumer.accept(messageEvent)
//            if (kind == MessageEvent.Kind.ERROR) {
//                context.errors.incrementAndGet()
//            } else {
//                context.warnings.incrementAndGet()
//            }
//        }
//    }

    private fun handleFinishedMessage(failedTaskName: String?, messageConsumer: Consumer<in BuildEvent>) {
        for (startEvent in startEvents) {
            val finishEvent = FinishEventImpl(
                startEvent.id,
                context.parentId,
                System.currentTimeMillis(),
                startEvent.message,
                when (failedTaskName) {
                    startEvent.taskName -> FailureResultImpl(null as Throwable?)
                    null -> SuccessResultImpl()
                    else -> SkippedResultImpl()
                }
            )
            messageConsumer.accept(finishEvent)

        }

    }

    private fun getMessageKind(kind: String): MessageEvent.Kind =
        when (kind) {
            "error", "Internal Compiler Error" -> MessageEvent.Kind.ERROR
            "warning" -> MessageEvent.Kind.WARNING
            else -> MessageEvent.Kind.SIMPLE
        }

    private fun handleCompilingMessage(
        @BuildEventsNls.Message originalMessage: String,
        isUpToDate: Boolean,
        messageConsumer: Consumer<in BuildEvent>
    ) {
        val message = originalMessage.replace(
            CangJieBundle.message("build.event.message.fresh"),
            CangJieBundle.message("build.event.message.compiling")
        ).substringBefore("(").trimEnd()
        val eventId = message.substringAfter(" ").replace(" v", " ")
        val startEvent = StartEventImpl(eventId, context.parentId, System.currentTimeMillis(), message)
        messageConsumer.accept(startEvent)
        if (isUpToDate) {
            val finishEvent = FinishEventImpl(
                eventId,
                context.parentId,
                System.currentTimeMillis(),
                message,
                SuccessResultImpl(isUpToDate)
            )
            messageConsumer.accept(finishEvent)
        } else {
            startEvents.add(startEvent)
        }
    }

    companion object {


        private fun createMessageEvent(
            workingDirectory: Path,
            parentEventId: Any,
            kind: MessageEvent.Kind,
            @BuildEventsNls.Message message: String,
            @Nls detailedMessage: String?,
            filePosition: FilePosition? = null
        ): MessageEvent = FileMessageEventImpl(
            parentEventId,
            kind,
            CangJieBundle.message("cangjie.compiler"),
            message,
            detailedMessage,

            // We are using `FileMessageEventImpl` instead of `MessageEventImpl`
            // even if `filePosition` is `null` because of the issue with `MessageEventImpl`:
            // https://youtrack.jetbrains.com/issue/IDEA-258407
            filePosition ?: FilePosition(workingDirectory.toFile(), 0, 0)
        )

        private val PROGRESS_TOTAL_RE: Regex = """(\d+)/(\d+)""".toRegex()
        private val MESSAGES_TO_IGNORE: List<String> =
            listOf("Build failed, waiting for other jobs to finish", "Build failed")

        private val StartEvent.taskName: String?
            get() = (id as? String)?.substringBefore(" ")?.substringBefore("(")?.trimEnd()

        private data class Progress(val current: Long, val total: Long) {
            val fraction: Double = current.toDouble() / total
        }
    }
}


fun AnsiEscapeDecoder.removeEscapeSequences(text: String): String {
    val chunks = mutableListOf<String>()
    escapeText(text, ProcessOutputTypes.STDOUT) { chunk, _ ->
        chunks.add(chunk)
    }
    return chunks.joinToString("")
}

@NlsActions.ActionText
fun String.capitalized(): String = StringUtil.capitalize(this)

//错误单元
data class Span(
    val file_path: String,
    val file_name: String,
    val line_start: Int,
    val column_start: Int,
    val line_end: Int,
    val column_end: Int
)
