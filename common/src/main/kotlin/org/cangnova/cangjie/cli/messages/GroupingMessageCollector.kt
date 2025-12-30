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

import com.google.common.collect.LinkedHashMultimap
import com.google.common.collect.Multimap

/**
 * 分组消息收集器
 *
 * 将消息按位置分组，然后在刷新时批量报告。
 * 支持将警告视为错误，以及控制是否报告所有警告。
 *
 * @property delegate 委托的消息收集器
 * @property treatWarningsAsErrors 是否将警告视为错误
 * @property reportAllWarnings 是否报告所有警告
 */
class GroupingMessageCollector(
    private val delegate: MessageCollector,
    private val treatWarningsAsErrors: Boolean,
    private val reportAllWarnings: Boolean
) : MessageCollector {

    /**
     * 分组的消息
     * 注意：键可以为 null（表示没有位置信息的消息）
     */
    private val groupedMessages: Multimap<CompilerMessageSourceLocation?, Message> =
        LinkedHashMultimap.create()

    override fun clear() {
        groupedMessages.clear()
    }

    override fun report(
        severity: CompilerMessageSeverity,
        message: String,
        location: CompilerMessageSourceLocation?
    ) {
        // OUTPUT 和 VERBOSE 消息直接报告，不进行分组
        if (severity == CompilerMessageSeverity.OUTPUT || CompilerMessageSeverity.VERBOSE.contains(severity)) {
            delegate.report(severity, message, location)
        } else {
            // 其他消息进行分组
            groupedMessages.put(location, Message(severity, message, location))
        }
    }

    override fun hasErrors(): Boolean {
        return hasExplicitErrors() || (treatWarningsAsErrors && hasWarnings())
    }

    /**
     * 是否有显式错误
     */
    private fun hasExplicitErrors(): Boolean {
        return groupedMessages.entries().any { it.value.severity.isError }
    }

    /**
     * 是否有警告
     */
    private fun hasWarnings(): Boolean {
        return groupedMessages.entries().any { it.value.severity.isWarning }
    }

    /**
     * 刷新所有分组的消息
     *
     * 按位置排序后依次报告所有消息。
     * 如果设置了将警告视为错误且有警告但没有显式错误，会添加一条错误消息。
     */
    fun flush() {
        val hasExplicitErrors = hasExplicitErrors()

        // 如果将警告视为错误，且有警告但没有显式错误，添加一条错误消息
        if (treatWarningsAsErrors && !hasExplicitErrors && hasWarnings()) {
            report(CompilerMessageSeverity.ERROR, "warnings found and -Werror specified", null)
        }

        // 按位置排序
        val sortedKeys = groupedMessages.keySet().sortedWith(
            nullsFirst(CompilerMessageLocationComparator)
        )

        // 依次报告每个位置的所有消息
        for (location in sortedKeys) {
            for (message in groupedMessages[location]) {
                // 如果有显式错误，只报告错误和强警告（除非设置了报告所有警告）
                if (!hasExplicitErrors ||
                    reportAllWarnings ||
                    message.severity.isError ||
                    message.severity == CompilerMessageSeverity.STRONG_WARNING
                ) {
                    delegate.report(message.severity, message.message, message.location)
                }
            }
        }

        groupedMessages.clear()
    }

    /**
     * 消息位置比较器
     *
     * 按列、行、路径的顺序比较位置。
     * -1 值表示位置未知，未知位置排在前面。
     */
    private object CompilerMessageLocationComparator : Comparator<CompilerMessageSourceLocation> {
        override fun compare(o1: CompilerMessageSourceLocation, o2: CompilerMessageSourceLocation): Int {
            // 比较列
            if (o1.column == -1 && o2.column != -1) return -1
            if (o1.column != -1 && o2.column == -1) return 1

            // 比较行
            if (o1.line == -1 && o2.line != -1) return -1
            if (o1.line != -1 && o2.line == -1) return 1

            // 比较路径
            return o1.path.compareTo(o2.path)
        }
    }

    /**
     * 消息数据类
     *
     * @property severity 消息严重级别
     * @property message 消息内容
     * @property location 消息位置
     */
    private data class Message(
        val severity: CompilerMessageSeverity,
        val message: String,
        val location: CompilerMessageSourceLocation?
    ) {
        override fun toString(): String {
            return "[$severity] $message${if (location != null) " (at $location)" else " (no location)"}"
        }
    }
}
