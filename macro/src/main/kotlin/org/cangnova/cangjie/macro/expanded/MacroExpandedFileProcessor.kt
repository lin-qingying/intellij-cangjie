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
 */

package org.cangnova.cangjie.macro.expanded

import com.intellij.openapi.diagnostic.Logger

/**
 * 宏展开文件处理器
 *
 * 处理编译器 `cjc-frontend --debug-macro` 生成的 `.macrocall` 文件，执行以下操作：
 *
 * 1. **剥离标记注释**：移除编译器插入的 `/* ===== Emitted by ... ===== */`、
 *    `/* ===== End of the Emit ===== */` 和 `/* line.col */` 前缀
 * 2. **生成干净的展开文件**：输出可直接被 IntelliJ PSI 解析的标准仓颉源码
 * 3. **构建偏移映射**：记录展开区域与原始源文件位置的对应关系
 *
 * ## 编译器输出格式示例
 *
 * ```
 * package a
 *
 * import std.math.*
 *
 * let a = /* ===== Emitted by MacroCall @abc2 in a.cj:7:9 ===== */
 * /* 7.9 */1
 * /* ===== End of the Emit ===== */
 *
 * func a2(): Unit {
 *     let b = /* ===== Emitted by MacroCall @abc2 in a.cj:10:13 ===== */
 * /* 10.13 */1
 * /* ===== End of the Emit ===== */
 * }
 * ```
 *
 * ## 处理后输出
 *
 * 干净文件（展开区域行号前缀被剥离，标记注释被移除）：
 * ```
 * package a
 *
 * import std.math.*
 *
 * let a = 1
 *
 * func a2(): Unit {
 *     let b = 1
 * }
 * ```
 *
 * 偏移映射记录每个展开区域在干净文件中的位置和原始源文件中的行列号。
 */
object MacroExpandedFileProcessor {

    private val log = Logger.getInstance(MacroExpandedFileProcessor::class.java)

    /**
     * 处理结果
     *
     * @param cleanContent 剥离所有标记注释后的干净仓颉源码
     * @param offsetMapping 展开区域偏移映射（sourceFilePath 和 expandedFilePath 由调用者填充）
     */
    data class ProcessResult(
        val cleanContent: String,
        val offsetMapping: MacroExpansionOffsetMapping
    )

    // 匹配开始标记：/* ===== Emitted by MacroCall @MacroName in file.cj:4:1 ===== */
    // 注意：开始标记可能出现在行中间（如 `let a = /* ===== ... */`）
    private val START_MARKER_REGEX = Regex(
        """/\* ===== Emitted by MacroCall @(.+?) in (.+?):(\d+):(\d+) ===== \*/"""
    )

    // 结束标记
    private const val END_MARKER = "/* ===== End of the Emit ===== */"

    // 匹配行号前缀：/* 4.1 */ （行号.列号），前面可能有空白
    private val LINE_PREFIX_REGEX = Regex("""^(\s*)/\* \d+\.\d+ \*/""")

    /**
     * 处理编译器输出的 `.macrocall` 文件内容
     *
     * @param macroCallContent `.macrocall` 文件的完整内容
     * @param sourceFilePath 原始源文件路径
     * @param expandedFilePath 生成的展开文件路径（用于偏移映射）
     * @return 处理结果，包含干净代码和偏移映射；如果内容中没有宏标记则返回 null
     */
    fun process(
        macroCallContent: String,
        sourceFilePath: String,
        expandedFilePath: String
    ): ProcessResult? {
        // 快速检查：是否包含宏展开标记
        if (!macroCallContent.contains("/* ===== Emitted by MacroCall")) {
            return null
        }

        val regions = mutableListOf<MacroExpansionOffsetMapping.ExpansionRegion>()
        val cleanLines = mutableListOf<String>()

        // 逐行处理，使用状态机跟踪当前是否在展开区域内
        val lines = macroCallContent.lines()
        var i = 0
        // 当前展开区域的上下文
        var currentMacroName: String? = null
        var currentOriginalLine = 0
        var currentOriginalCol = 0
        var expansionStartLine = -1  // 干净文件中的起始行号（1-based）
        var inExpansion = false

        while (i < lines.size) {
            val line = lines[i]

            if (!inExpansion) {
                // 非展开区域：检查是否有开始标记
                val startMatch = START_MARKER_REGEX.find(line)
                if (startMatch != null) {
                    // 提取宏信息
                    currentMacroName = startMatch.groupValues[1]
                    currentOriginalLine = startMatch.groupValues[3].toIntOrNull() ?: 0
                    currentOriginalCol = startMatch.groupValues[4].toIntOrNull() ?: 0

                    // 开始标记可能在行中间（如 `let a = /* ===== ... */`）
                    // 保留开始标记之前的部分
                    val prefix = line.substring(0, startMatch.range.first)
                    val suffix = line.substring(startMatch.range.last + 1)

                    if (prefix.isNotBlank()) {
                        // 开始标记在行中间，先把前缀追加到干净输出
                        // 但后缀（如果有）需要和展开内容拼接
                        // 实际编译器格式中，开始标记后通常是换行，没有后缀内容
                        val cleanPrefix = prefix.trimEnd()
                        // 如果后面有展开的内容行（下一行带 /* line.col */ 前缀），
                        // 需要将 prefix 和第一行展开内容合并到同一行
                        inExpansion = true
                        expansionStartLine = cleanLines.size + 1  // 1-based，下一行即展开开始

                        // 把 prefix 暂存，和展开第一行合并
                        // 因为编译器输出格式是：
                        //   let a = /* ===== Emitted ... ===== */
                        //   /* 7.9 */1
                        // 清洁后应该是：
                        //   let a = 1
                        // 所以 prefix 行先不加入 cleanLines，等展开第一行来合并
                        cleanLines.add(cleanPrefix) // 先添加，后面合并时替换最后一行
                    } else {
                        // 开始标记在行首：整行就是标记，跳过
                        inExpansion = true
                        expansionStartLine = cleanLines.size + 1
                    }
                } else {
                    // 普通行（非展开区域），直接输出
                    cleanLines.add(line)
                }
            } else {
                // 在展开区域内
                if (line.trimStart().startsWith(END_MARKER) || line.contains(END_MARKER)) {
                    // 遇到结束标记，结束当前展开区域
                    val expansionEndLine = cleanLines.size  // 1-based，当前最后一行
                    if (currentMacroName != null && expansionStartLine > 0 && expansionEndLine >= expansionStartLine) {
                        regions.add(
                            MacroExpansionOffsetMapping.ExpansionRegion(
                                macroName = currentMacroName!!,
                                originalLine = currentOriginalLine,
                                originalCol = currentOriginalCol,
                                expandedStartLine = expansionStartLine,
                                expandedEndLine = expansionEndLine
                            )
                        )
                    }
                    inExpansion = false
                    currentMacroName = null
                    expansionStartLine = -1
                } else {
                    // 展开区域内的内容行：剥离 /* line.col */ 前缀
                    val strippedLine = stripLinePrefix(line)

                    // 检查是否需要与前一行合并（开始标记在行中间的情况）
                    // 即 prefix 行（如 "let a = "）需要和展开第一行（如 "1"）合并
                    if (expansionStartLine == cleanLines.size + 1 && cleanLines.isNotEmpty()) {
                        val lastLine = cleanLines.last()
                        // 如果上一行是 prefix（开始标记前的部分），且当前是展开第一行
                        if (lastLine.isNotEmpty() && !lastLine.endsWith("\n")) {
                            // 合并：把展开首行内容追加到 prefix 行
                            cleanLines[cleanLines.size - 1] = lastLine + strippedLine
                            // 更新展开起始行为合并后的行
                            expansionStartLine = cleanLines.size
                            i++
                            continue
                        }
                    }

                    cleanLines.add(strippedLine)
                }
            }
            i++
        }

        // 如果文件结尾还在展开区域内（缺少结束标记），关闭它
        if (inExpansion && currentMacroName != null) {
            val expansionEndLine = cleanLines.size
            if (expansionStartLine > 0 && expansionEndLine >= expansionStartLine) {
                regions.add(
                    MacroExpansionOffsetMapping.ExpansionRegion(
                        macroName = currentMacroName!!,
                        originalLine = currentOriginalLine,
                        originalCol = currentOriginalCol,
                        expandedStartLine = expansionStartLine,
                        expandedEndLine = expansionEndLine
                    )
                )
            }
            log.warn("宏展开区域缺少结束标记: $currentMacroName in $sourceFilePath")
        }

        val cleanContent = cleanLines.joinToString("\n")
        val offsetMapping = MacroExpansionOffsetMapping(
            sourceFilePath = sourceFilePath,
            expandedFilePath = expandedFilePath,
            expansionRegions = regions
        )

        return ProcessResult(cleanContent, offsetMapping)
    }

    /**
     * 剥离行号前缀 `/* line.col */`
     *
     * 处理前后对比：
     * - `/* 4.1 */func foo() {` → `func foo() {`
     * - `  /* 5.5 */    return 42` → `      return 42`（保留前导空白）
     * - `no prefix line` → `no prefix line`（无前缀时原样返回）
     */
    private fun stripLinePrefix(line: String): String {
        val match = LINE_PREFIX_REGEX.find(line) ?: return line
        val leadingWhitespace = match.groupValues[1]
        return leadingWhitespace + line.substring(match.range.last + 1)
    }

    /**
     * 检查给定内容是否包含宏展开标记
     *
     * 可用于快速判断 `.macrocall` 文件是否值得处理
     */
    fun hasMacroMarkers(content: String): Boolean {
        return content.contains("/* ===== Emitted by MacroCall")
    }
}
