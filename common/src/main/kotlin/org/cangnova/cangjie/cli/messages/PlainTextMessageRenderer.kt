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

package org.cangnova.cangjie.cli.messages

import org.cangnova.cangjie.cli.messages.CompilerMessageSeverity.*
import org.cangnova.cangjie.utils.decapitalizeAsciiOnly
import org.cangnova.cangjie.utils.isWindows
import org.fusesource.jansi.Ansi
import org.fusesource.jansi.AnsiConsole
import org.fusesource.jansi.internal.CLibrary
import java.util.*

/**
 * 纯文本消息渲染器
 *
 * 以纯文本格式渲染编译器消息，支持 ANSI 颜色（如果终端支持）。
 *
 * @property colorEnabled 是否启用颜色输出
 */
abstract class PlainTextMessageRenderer(
    private val colorEnabled: Boolean = IS_STDERR_A_TTY
) : MessageRenderer {

    override fun renderPreamble(): String = ""

    override fun render(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
    ): String = buildString {
        val line = location?.line ?: -1
        val column = location?.column ?: -1
        val lineEnd = location?.lineEnd ?: -1
        val columnEnd = location?.columnEnd ?: -1
        val lineContent = location?.lineContent

        // 渲染路径和位置
        val path = location?.let { getPath(it) }
        if (path != null) {
            append(path)
            append(":")
            if (line > 0) {
                append(line).append(":")
                if (column > 0) {
                    append(column).append(":")
                }
            }
            append(" ")
        }

        // 渲染严重级别和消息
        if (colorEnabled) {
            val ansi = Ansi.ansi()
                .bold()
                .fg(severityColor(severity))
                .a(severity.presentableName)
                .a(": ")
                .reset()

            if (IMPORTANT_MESSAGE_SEVERITIES.contains(severity)) {
                ansi.bold()
            }

            // 只将消息的第一行设为粗体，否则长的重载歧义错误或异常很难阅读
            val decapitalized = decapitalizeIfNeeded(message)
            val firstNewline = decapitalized.indexOf(LINE_SEPARATOR)
            if (firstNewline < 0) {
                append(ansi.a(decapitalized).reset())
            } else {
                append(
                    ansi.a(decapitalized.substring(0, firstNewline))
                        .reset()
                        .a(decapitalized.substring(firstNewline))
                )
            }
        } else {
            append(severity.presentableName)
            append(": ")
            append(decapitalizeIfNeeded(message))
        }

        // 渲染源代码行和错误位置标记
        if (lineContent != null && column in 1..lineContent.length + 1) {
            append(LINE_SEPARATOR)
            append(lineContent)
            append(LINE_SEPARATOR)
            append(" ".repeat(column - 1))

            when {
                lineEnd > line -> {
                    // 跨多行
                    append("^".repeat(lineContent.length - column + 1))
                }
                lineEnd == line && columnEnd > column -> {
                    // 同一行多列
                    append("^".repeat(columnEnd - column))
                }
                else -> {
                    // 单个位置
                    append("^")
                }
            }
        }
    }

    /**
     * 获取路径字符串
     *
     * 由子类实现，决定如何显示路径（绝对路径、相对路径或不显示）。
     */
    protected abstract fun getPath(location: CompilerMessageSourceLocation): String?

    override fun renderUsage(usage: String): String = usage

    override fun renderConclusion(): String = ""

    /**
     * 如果需要则启用颜色
     */
    fun enableColorsIfNeeded() {
        if (colorEnabled) {
            AnsiConsole.systemInstall()
        }
    }

    /**
     * 如果需要则禁用颜色
     */
    fun disableColorsIfNeeded() {
        if (colorEnabled) {
            AnsiConsole.systemUninstall()
        }
    }

    companion object {
        /**
         * 检测 stderr 是否是 TTY
         */
        private val IS_STDERR_A_TTY: Boolean by lazy {
            var isStderrATty = false
            // TODO: 调查为什么 ANSI 转义码在 Windows 上只能在 REPL 中工作
            if (!isWindows() && "true" == CompilerSystemProperties.CANGJIE_COLORS_ENABLED_PROPERTY.value) {
                try {
                    isStderrATty = CLibrary.isatty(CLibrary.STDERR_FILENO) != 0
                } catch (_: UnsatisfiedLinkError) {
                    // 忽略
                }
            }
            isStderrATty
        }

        private val LINE_SEPARATOR = System.lineSeparator()

        /**
         * 重要消息的严重级别
         */
        private val IMPORTANT_MESSAGE_SEVERITIES = EnumSet.of(
            EXCEPTION,
            ERROR,
            STRONG_WARNING,
            WARNING
        )

        /**
         * 如果需要则将消息首字母小写
         */
        private fun decapitalizeIfNeeded(message: String): String {
            // TODO: 发明更聪明的东西
            // 一个临时启发式规则，防止某些名称被小写化
            if (message.startsWith("Java") || message.startsWith("Kotlin")) {
                return message
            }

            // 对于缩写和大写文本
            if (message.length >= 2 &&
                message[0].isUpperCase() &&
                message[1].isUpperCase()
            ) {
                return message
            }

            return message.decapitalizeAsciiOnly()
        }

        /**
         * 获取严重级别对应的颜色
         */
        private fun severityColor(severity: CompilerMessageSeverity): Ansi.Color = when (severity) {
            EXCEPTION -> Ansi.Color.RED
            ERROR -> Ansi.Color.RED
            STRONG_WARNING -> Ansi.Color.YELLOW
            WARNING -> Ansi.Color.YELLOW
            INFO -> Ansi.Color.BLUE
            LOGGING -> Ansi.Color.BLUE
            OUTPUT -> Ansi.Color.BLUE
        }
    }
}
