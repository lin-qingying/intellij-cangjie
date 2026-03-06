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

package org.cangnova.cangjie.macro.compiler

/**
 * 解析编译器 `--debug-macro` 生成的 `.macrocall` 文件
 *
 * 编译器输出格式：
 * ```
 * /* ===== Emitted by MacroCall @MacroName in file.cj:4:1 ===== */
 * /* 4.1 */func foo() { ... }
 * /* ===== End of the Emit ===== */
 * ```
 *
 * 本解析器提取每个宏展开块的：
 * - 宏名称（如 `MacroName`）
 * - 源文件名（如 `file.cj`）
 * - 行列号
 * - 展开后的代码（剥离行号前缀 `/* line.col */`）
 */
internal object MacroCallFileParser {

    /**
     * 解析后的单个宏展开块
     */
    data class ParsedMacroBlock(
        val macroName: String,
        val sourceFileName: String,
        val line: Int,
        val column: Int,
        val expandedText: String
    )

    // 匹配开始标记：/* ===== Emitted by MacroCall @MacroName in file.cj:4:1 ===== */
    private val START_MARKER_REGEX = Regex(
        """/\* ===== Emitted by MacroCall @(.+?) in (.+?):(\d+):(\d+) ===== \*/"""
    )

    private const val END_MARKER = "/* ===== End of the Emit ===== */"

    // 匹配行号前缀：/* 4.1 */ （行号.列号），允许前面有缩进空白
    // 表达式宏（如 let a = @macro(...)）展开后带有缩进，前缀不在行首
    private val LINE_PREFIX_REGEX = Regex("""^\s*/\* \d+\.\d+ \*/""")

    /**
     * 解析 `.macrocall` 文件内容，提取所有宏展开块
     *
     * @param content `.macrocall` 文件的全部内容
     * @return 解析出的宏展开块列表，无标记时返回空列表
     */
    fun parse(content: String): List<ParsedMacroBlock> {
        val blocks = mutableListOf<ParsedMacroBlock>()
        var searchStart = 0

        while (searchStart < content.length) {
            // 查找下一个开始标记
            val startMatch = START_MARKER_REGEX.find(content, searchStart) ?: break

            val macroName = startMatch.groupValues[1]
            val sourceFileName = startMatch.groupValues[2]
            val line = startMatch.groupValues[3].toIntOrNull() ?: 0
            val column = startMatch.groupValues[4].toIntOrNull() ?: 0

            // 块内容从开始标记之后开始
            val blockContentStart = startMatch.range.last + 1

            // 查找对应的结束标记
            val endIndex = content.indexOf(END_MARKER, blockContentStart)
            if (endIndex == -1) {
                // 缺少结束标记，跳过该块继续查找
                searchStart = blockContentStart
                continue
            }

            // 提取标记之间的原始内容
            val rawContent = content.substring(blockContentStart, endIndex)

            // 剥离每行的行号前缀并组装展开代码
            val expandedText = stripLinePrefixes(rawContent)

            blocks.add(
                ParsedMacroBlock(
                    macroName = macroName,
                    sourceFileName = sourceFileName,
                    line = line,
                    column = column,
                    expandedText = expandedText
                )
            )

            // 继续从结束标记之后搜索
            searchStart = endIndex + END_MARKER.length
        }

        return blocks
    }

    /**
     * 剥离每行的行号前缀 `/* line.col */`
     *
     * 输入示例：
     * ```
     * /* 4.1 */func foo() {
     * /* 5.5 */    return 42
     * /* 6.1 */}
     * ```
     *
     * 输出：
     * ```
     * func foo() {
     *     return 42
     * }
     * ```
     */
    private fun stripLinePrefixes(rawContent: String): String {
        return rawContent.lineSequence()
            .map { line -> LINE_PREFIX_REGEX.replaceFirst(line, "") }
            .joinToString("\n")
            .trim()
    }
}
