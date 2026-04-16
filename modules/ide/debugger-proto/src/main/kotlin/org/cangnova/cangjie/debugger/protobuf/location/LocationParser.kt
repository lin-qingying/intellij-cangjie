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

package org.cangnova.cangjie.debugger.protobuf.location

import org.jetbrains.annotations.NonNls
import java.util.regex.Pattern

/**
 * 源码位置解析器
 *
 * 负责解析各种格式的位置字符串为 SourceLocation 对象。
 * 支持的格式：
 * - "path:line" - 标准格式，例如 "/path/to/file.cj:10"
 *
 * 内部使用正则表达式进行模式匹配，确保解析的准确性和性能。
 */
internal object LocationParser {
    /**
     * 位置字符串的正则模式: path:line
     * - 捕获组1: 文件路径（任意字符）
     * - 捕获组2: 行号（数字）
     */
    private val LOCATION_PATTERN: Pattern = Pattern.compile("^(.*):(\\d+)$")

    /**
     * 解析位置字符串
     *
     * @param locationString 位置字符串
     * @return 解析成功的 SourceLocation，失败返回 null
     */
    fun parse(@NonNls locationString: String): SourceLocation? {
        if (locationString.isBlank()) {
            return null
        }

        val matcher = LOCATION_PATTERN.matcher(locationString)
        if (!matcher.matches()) {
            return null
        }

        val path = matcher.group(1) ?: return null
        val lineStr = matcher.group(2) ?: return null

        return try {
            val editorLine = lineStr.toInt()
            if (editorLine <= 0) {
                null // 行号必须大于0
            } else {
                SourceLocation.fromEditorPosition(path, editorLine)
            }
        } catch (e: NumberFormatException) {
            null
        }
    }
}