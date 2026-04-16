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

package org.cangnova.cangjie.utils

import com.intellij.openapi.util.text.StringUtil
import kotlin.math.max
import kotlin.math.min
import kotlin.text.iterator

/**
 * 光标位置标记，用于在调试信息中表示光标位置。
 */
private const val CARET_MARKER = "<~!!~>"

/**
 * 起始位置标记，用于在调试信息中表示选择区域的开始。
 */
private const val BEGIN_MARKER = "<~BEGIN~>"

/**
 * 结束位置标记，用于在调试信息中表示选择区域的结束。
 */
private const val END_MARKER = "<~END~>"

/**
 * 折叠字符串中的连续空白字符。
 * 将多个连续的空白字符（空格、制表符、换行符等）替换为单个空格。
 *
 * @return 折叠空白字符后的字符串
 *
 * 示例：
 * ```kotlin
 * "hello    world\n\ttest".collapseSpaces()
 * // 返回 "hello world test"
 * ```
 */
fun String.collapseSpaces(): String {
    val builder = StringBuilder()
    var haveSpaces = false
    for (c in this) {
        if (c.isWhitespace()) {
            haveSpaces = true
        } else {
            if (haveSpaces) {
                builder.append(" ")
                haveSpaces = false
            }
            builder.append(c)
        }
    }
    return builder.toString()
}

/**
 * 提取字符串的子串并附加上下文信息。
 * 在调试和错误报告中非常有用，可以显示子串在原字符串中的位置和周围内容。
 *
 * @param beginIndex 子串的起始索引
 * @param endIndex 子串的结束索引（不包含）
 * @param range 上下文范围，表示在子串前后各取多少个字符
 * @return 带有位置标记和上下文的字符串
 *
 * 返回的字符串格式：
 * - 如果不是从开头开始：`<~...(line: N)~>` 前缀
 * - `<~BEGIN~>` 标记子串开始
 * - 目标子串内容
 * - `<~END~>` 标记子串结束（如果开始和结束位置不同）
 * - `<~!!~>` 光标标记（如果开始和结束位置相同）
 * - 如果不是到结尾：`<~(line: N)...~>` 后缀
 *
 * 示例：
 * ```kotlin
 * val text = "line1\nline2\nline3\nline4"
 * text.substringWithContext(6, 11, 5)
 * // 可能返回: "line1\n<~BEGIN~>line2<~END~>\nline3<~(line: 3)...~>"
 * ```
 */
fun CharSequence.substringWithContext(beginIndex: Int, endIndex: Int, range: Int): String {
    val start = max(0, beginIndex - range)
    val end = min(this.length, endIndex + range)

    val notFromBegin = start != 0
    val notToEnd = end != this.length

    val updatedStart = beginIndex - start
    val updatedEnd = endIndex - start

    return StringBuilder(this.toString().substring(start, end))
        .insert(updatedEnd, if (updatedEnd == updatedStart) CARET_MARKER else END_MARKER)
        .insert(updatedStart, if (updatedEnd == updatedStart) "" else BEGIN_MARKER)
        .insert(0, if (notFromBegin) "<~...${position(this, start)}~>" else "")
        .append(if (notToEnd) "<~${position(this, end)}...~>" else "").toString()
}

/**
 * 计算字符序列中指定偏移量对应的行号位置字符串。
 * 用于生成调试信息中的位置提示。
 *
 * @param str 字符序列
 * @param offset 偏移量
 * @return 格式化的位置字符串，例如 "(line: 5)"
 */
private fun position(str: CharSequence, offset: Int): String {
    val line = StringUtil.offsetToLineNumber(str, offset) + 1
    return "(line: $line)"
}

/**
 * 将集合中的元素用分隔符连接成字符串。
 * 这是标准库 [Iterable.joinToString] 的简化版本。
 *
 * @param collection 要连接的元素集合
 * @param separator 元素之间的分隔符
 * @return 连接后的字符串
 *
 * 示例：
 * ```kotlin
 * join(listOf(1, 2, 3), ", ") // 返回 "1, 2, 3"
 * ```
 */
fun join(collection: Iterable<Any>, separator: String) = collection.joinToString(separator)



