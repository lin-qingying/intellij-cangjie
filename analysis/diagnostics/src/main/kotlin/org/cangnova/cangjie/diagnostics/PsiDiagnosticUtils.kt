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

package org.cangnova.cangjie.diagnostics

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException

/**
 * PSI 诊断工具类
 *
 * 提供了一组静态工具方法，用于将 PSI 元素的位置信息转换为人类可读的格式。
 * 主要用于生成诊断错误消息、异常堆栈信息和调试输出。
 *
 * ## 主要功能
 * - **位置转换**：将 PSI 元素位置转换为"(行,列) in 文件路径"格式
 * - **偏移量解析**：将文档偏移量转换为行列号
 * - **容错处理**：处理无效 PSI 元素和空文档的情况
 *
 * ## 使用场景
 * 1. **错误报告**：在编译错误中显示精确位置
 * 2. **异常消息**：为异常添加源代码位置信息
 * 3. **调试输出**：记录 PSI 元素的位置用于调试
 * 4. **IDE 集成**：生成可点击跳转的位置信息
 *
 * @see LineAndColumn 行列信息数据类
 * @see org.cangnova.cangjie.diagnostics.Diagnostic 诊断信息接口
 *
 * @sample
 * ```kotlin
 * // 获取元素位置字符串
 * val location = PsiDiagnosticUtils.atLocation(psiElement)
 * // 输出: "(12,5) in /path/to/file.cj"
 *
 * // 将偏移量转换为行列
 * val lineCol = PsiDiagnosticUtils.offsetToLineAndColumn(document, 150)
 * println("位置: 第 ${lineCol.line} 行，第 ${lineCol.column} 列")
 * ```
 */
class PsiDiagnosticUtils {

    /**
     * 行列信息数据类
     *
     * 封装了源代码中某个位置的行号、列号和行内容信息。
     * 行号和列号都是从 1 开始计数（用户友好的格式）。
     *
     * ## 特殊值
     * - 行号为负数表示位置未知
     * - [NONE] 常量表示无效位置
     *
     * @property line 行号，从 1 开始计数，负数表示未知
     * @property column 列号，从 1 开始计数（当行号未知时表示偏移量）
     * @property lineContent 该行的文本内容，用于显示上下文，可能为 null
     *
     * @sample
     * ```kotlin
     * val loc = LineAndColumn(10, 5, "    let x = 42")
     * println(loc)  // 输出: (10,5)
     * ```
     */
    class LineAndColumn(val line: Int, val column: Int, val lineContent: String?) {
        /**
         * 返回位置的字符串表示
         *
         * 此方法用于向用户展示位置信息，格式为：
         * - 正常情况：`(行号,列号)`
         * - 行号未知：`(offset: 偏移量 line unknown)`
         *
         * 注意：此格式专门设计为用户友好的展示格式。
         *
         * @return 位置的字符串表示
         */
        override fun toString(): String {
            if (line < 0) {
                return "(offset: $column line unknown)"
            }
            return "($line,$column)"
        }

        companion object {
            /**
             * 表示无效或未知位置的常量
             *
             * 当无法确定元素位置时使用此常量。
             * 行号和列号都为 -1，行内容为 null。
             */
            val NONE: LineAndColumn = LineAndColumn(-1, -1, null)
        }
    }

    companion object {

        /**
         * 获取 PSI 元素的位置字符串
         *
         * 将 PSI 元素的位置转换为人类可读的字符串格式。
         * 此方法会处理元素有效和无效两种情况：
         *
         * - **元素有效**：返回格式化的位置信息 "(行,列) in 文件路径"
         * - **元素无效**：尝试获取部分信息，返回 "at offset: {偏移量} file: {文件名}"
         *
         * ## 容错处理
         * 当元素无效时，会捕获 [PsiInvalidElementAccessException] 并返回部分信息，
         * 而不是抛出异常，确保诊断信息始终可以生成。
         *
         * @param element 要获取位置的 PSI 元素
         * @return 位置的字符串表示，格式为 "(行,列) in 文件路径" 或降级格式
         *
         * @sample
         * ```kotlin
         * val location = PsiDiagnosticUtils.atLocation(functionElement)
         * // 输出: "(15,8) in /src/main.cj"
         * ```
         */
        @JvmStatic
        fun atLocation(element: PsiElement): String {
            if (element.isValid) {
                return atLocation(
                    element.containingFile,
                    element.textRange
                )
            }

            // 处理无效元素的情况
            var file: PsiFile? = null
            var offset = -1
            try {
                file = element.containingFile
                offset = element.textOffset
            } catch (invalidException: PsiInvalidElementAccessException) {
                // 忽略异常，使用默认值
            }

            return "at offset: " + (if (offset != -1) offset else "<unknown>") + " file: " + (file
                ?: "<unknown>")
        }

        /**
         * 将文档偏移量转换为行列号
         *
         * 给定文档中的偏移量（字符位置），计算对应的行号和列号。
         * 返回的 [LineAndColumn] 对象还包含该行的文本内容，便于显示上下文。
         *
         * ## 坐标系统
         * - 行号和列号都从 1 开始计数（用户友好）
         * - 内部计算使用从 0 开始的索引，但最终会转换
         *
         * ## 特殊情况
         * - 文档为 null 或空：返回 `LineAndColumn(-1, offset, null)`
         * - 偏移量超出范围：由 IntelliJ Document API 处理
         *
         * @param document 包含文本的文档对象，可能为 null
         * @param offset 文档中的字符偏移量（从 0 开始）
         * @return 包含行号、列号和行内容的 [LineAndColumn] 对象
         *
         * @sample
         * ```kotlin
         * val document = psiFile.viewProvider.document
         * val lineCol = PsiDiagnosticUtils.offsetToLineAndColumn(document, 150)
         * println("错误位置: 第 ${lineCol.line} 行，第 ${lineCol.column} 列")
         * println("代码: ${lineCol.lineContent}")
         * ```
         */
        @JvmStatic
        fun offsetToLineAndColumn(
            document: Document?,
            offset: Int
        ): LineAndColumn {
            if (document == null || document.textLength == 0) {
                return LineAndColumn(-1, offset, null)
            }

            // 获取行号（从 0 开始）
            val lineNumber = document.getLineNumber(offset)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val column = offset - lineStartOffset

            // 提取行内容
            val lineEndOffset = document.getLineEndOffset(lineNumber)
            val lineContent = document.charsSequence.subSequence(lineStartOffset, lineEndOffset)

            // 转换为从 1 开始的行列号
            return LineAndColumn(
                lineNumber + 1,
                column + 1,
                lineContent.toString()
            )
        }

        /**
         * 获取文件中指定文本范围的位置字符串
         *
         * 根据文件、文本范围和文档生成完整的位置字符串。
         * 格式为：`(行号,列号) in 文件路径`
         *
         * ## 路径优先级
         * - 优先使用虚拟文件的完整路径（VirtualFile.path）
         * - 降级使用文件名（PsiFile.name）
         *
         * @param file PSI 文件对象
         * @param textRange 文件中的文本范围，使用起始偏移量作为位置
         * @param document 文档对象，用于偏移量到行列的转换，可能为 null
         * @return 格式化的位置字符串，如 "(10,5) in /path/to/file.cj"
         *
         * @sample
         * ```kotlin
         * val location = PsiDiagnosticUtils.atLocation(
         *     psiFile,
         *     element.textRange,
         *     document
         * )
         * ```
         */
        @JvmStatic
        fun atLocation(file: PsiFile, textRange: TextRange, document: Document?): String {
            val offset = textRange.startOffset
            val virtualFile = file.virtualFile
            val pathSuffix = " in " + (virtualFile?.path ?: file.name)
            return offsetToLineAndColumn(document, offset)
                .toString() + pathSuffix
        }

        /**
         * 获取文件中指定文本范围的位置字符串（自动获取文档）
         *
         * 便利方法，自动从文件的视图提供者获取文档对象，
         * 然后调用 [atLocation] 的完整版本。
         *
         * @param file PSI 文件对象
         * @param textRange 文件中的文本范围
         * @return 格式化的位置字符串，如 "(10,5) in /path/to/file.cj"
         *
         * @see atLocation(PsiFile, TextRange, Document?)
         *
         * @sample
         * ```kotlin
         * val location = PsiDiagnosticUtils.atLocation(psiFile, element.textRange)
         * // 输出: "(10,5) in /src/main/example.cj"
         * ```
         */
        @JvmStatic
        fun atLocation(file: PsiFile, textRange: TextRange): String {
            val document = file.viewProvider.document
            return atLocation(file, textRange, document)
        }
    }
}
