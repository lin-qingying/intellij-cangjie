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

import org.jetbrains.annotations.Contract
import org.jetbrains.annotations.NonNls

/**
 * 表示源码文件位置
 *
 * 封装了文件路径和行号信息，用于精确定位源码中的位置。
 * 行号从0开始计数，与调试器的内部表示一致。
 *
 * @property path 源码文件的完整路径
 * @property line 行号（从0开始）
 *
 * 使用场景：
 * - 断点设置：在源码的特定行设置断点
 * - 错误报告：指示编译错误或运行时错误的位置
 * - 调试导航：在调试过程中跳转到指定源码位置
 * - 代码覆盖：记录代码执行的行号信息
 *
 * 示例用法：
 * ```kotlin
 * val location = SourceLocation("/path/to/file.cj", 10)
 * val parsed = SourceLocation.parse("/path/to/file.cj:11")
 * val fromEditor = SourceLocation.fromEditorPosition("/path/to/file.cj", 11)
 * ```
 */
data class SourceLocation(
    @field:NonNls val path: String,
    val line: Int
) {
    /**
     * 获取文件名（不含路径）
     */
    val fileName: String
        get() = path.substringAfterLast('/', path.substringAfterLast('\\'))

    /**
     * 转换为编辑器行号（从1开始）
     */
    val editorLine: Int
        get() = line + 1

    /**
     * 格式化为标准位置字符串 "path:line"
     */
    fun format(): String = "$path:$editorLine"

    override fun toString(): String = format()

    companion object {
        /**
         * 从编辑器位置创建 SourceLocation
         *
         * 编辑器行号从1开始计数，会自动转换为调试器内部表示（从0开始）。
         *
         * @param path 源码文件路径
         * @param editorLine 编辑器行号（从1开始计数）
         * @return SourceLocation实例
         */
        @JvmStatic
        fun fromEditorPosition(
            @NonNls path: String,
            editorLine: Int
        ): SourceLocation = SourceLocation(path, editorLine - 1)

        /**
         * 解析位置字符串为 SourceLocation
         *
         * 支持格式 "path:line"，例如 "/path/to/file.cj:10"。
         *
         * @param locationString 位置字符串
         * @return 解析成功的 SourceLocation
         * @throws IllegalArgumentException 如果格式不正确
         */
        @JvmStatic
        fun parse(@NonNls locationString: String): SourceLocation {
            return tryParse(locationString)
                ?: throw IllegalArgumentException("Invalid location format: $locationString")
        }

        /**
         * 尝试解析位置字符串为 SourceLocation
         *
         * 支持格式 "path:line"，例如 "/path/to/file.cj:10"。
         * 如果解析失败，返回 null。
         *
         * @param locationString 位置字符串
         * @return 解析成功的 SourceLocation 或 null
         */
        @JvmStatic
        @JvmOverloads
        @Contract("_, !null -> !null")
        fun tryParse(
            @NonNls locationString: String,
            defaultValue: SourceLocation? = null
        ): SourceLocation? {
            return LocationParser.parse(locationString) ?: defaultValue
        }
    }
}