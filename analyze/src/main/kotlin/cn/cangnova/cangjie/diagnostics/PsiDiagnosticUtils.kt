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

package cn.cangnova.cangjie.diagnostics

import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException

/**
 * PSI诊断工具类
 * 
 * 提供用于处理PSI元素位置信息的实用方法，用于诊断报告
 */
class PsiDiagnosticUtils {
    /**
     * 行和列信息类
     * 
     * 表示源代码中的位置信息，包括行号、列号和行内容
     *
     * @property line 行号（从1开始）
     * @property column 列号（从1开始）
     * @property lineContent 行内容文本
     */
    class LineAndColumn(val line: Int, val column: Int, val lineContent: String?) {
        /**
         * 获取位置的字符串表示
         * 
         * 注意：此方法用于向用户展示位置信息
         *
         * @return 位置的字符串表示，格式为"(行,列)"
         */
        override fun toString(): String {
            if (line < 0) {
                return "(offset: $column line unknown)"
            }
            return "($line,$column)"
        }

        companion object {
            /**
             * 表示无效位置的常量
             */
            val NONE: LineAndColumn = LineAndColumn(-1, -1, null)
        }
    }

    companion object {
        /**
         * 获取元素位置的字符串表示
         *
         * @param element 要获取位置的PSI元素
         * @return 元素位置的字符串表示
         */
        @JvmStatic
        fun atLocation(element: PsiElement): String {
            if (element.isValid) {
                return atLocation(
                    element.containingFile,
                    element.textRange
                )
            }

            var file: PsiFile? = null
            var offset = -1
            try {
                file = element.containingFile
                offset = element.textOffset
            } catch (invalidException: PsiInvalidElementAccessException) {
                // ignore
            }

            return "at offset: " + (if (offset != -1) offset else "<unknown>") + " file: " + (file
                ?: "<unknown>")
        }

        /**
         * 将偏移量转换为行和列信息
         *
         * @param document 文档对象
         * @param offset 偏移量
         * @return 行和列信息
         */
        @JvmStatic
        fun offsetToLineAndColumn(
            document: Document?,
            offset: Int
        ): LineAndColumn {
            if (document == null || document.textLength == 0) {
                return LineAndColumn(-1, offset, null)
            }

            val lineNumber = document.getLineNumber(offset)
            val lineStartOffset = document.getLineStartOffset(lineNumber)
            val column = offset - lineStartOffset

            val lineEndOffset = document.getLineEndOffset(lineNumber)
            val lineContent = document.charsSequence.subSequence(lineStartOffset, lineEndOffset)

            return LineAndColumn(
                lineNumber + 1,
                column + 1,
                lineContent.toString()
            )
        }

        /**
         * 获取文件中指定文本范围的位置信息
         *
         * @param file PSI文件
         * @param textRange 文本范围
         * @param document 文档对象
         * @return 位置的字符串表示
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
         * 获取文件中指定文本范围的位置信息
         *
         * @param file PSI文件
         * @param textRange 文本范围
         * @return 位置的字符串表示
         */
        @JvmStatic
        fun atLocation(file: PsiFile, textRange: TextRange): String {
            val document = file.viewProvider.document
            return atLocation(file, textRange, document)
        }
    }
}
