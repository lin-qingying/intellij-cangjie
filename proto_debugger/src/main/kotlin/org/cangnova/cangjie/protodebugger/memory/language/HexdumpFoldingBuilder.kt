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

package org.cangnova.cangjie.protodebugger.memory.language

import com.intellij.lang.ASTNode
import com.intellij.lang.folding.FoldingBuilder
import com.intellij.lang.folding.FoldingDescriptor
import com.intellij.openapi.editor.Document
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import com.intellij.psi.util.PsiTreeUtil

/**
 * Hexdump代码折叠构建器
 *
 * 支持折叠：
 * 1. 连续的相同内容行（如大量零字节）
 * 2. 大的数据块
 * 3. 注释块
 *
 * 示例：
 * ```
 * 0000000000001000: 00 00 00 00 00 00 00 00  |........|
 * 0000000000001010: 00 00 00 00 00 00 00 00  |........|  <-- 可折叠
 * 0000000000001020: 00 00 00 00 00 00 00 00  |........|
 * 0000000000001030: 48 89 E5                  |H..|
 * ```
 *
 * 折叠后显示：
 * ```
 * 0000000000001000: 00 00 00 00 00 00 00 00  |........|
 * [+] ... 2 more lines with zeros ...
 * 0000000000001030: 48 89 E5                  |H..|
 * ```
 */
class HexdumpFoldingBuilder : FoldingBuilder {

    override fun buildFoldRegions(node: ASTNode, document: Document): Array<FoldingDescriptor> {
        val descriptors = mutableListOf<FoldingDescriptor>()
        val element = node.psi

        // 查找所有行
        val lines = PsiTreeUtil.findChildrenOfType(element, HexdumpPsiElement::class.java)
            .filter { it.node.elementType == HexdumpElementTypes.LINE }

        // 折叠连续的相似行
        foldSimilarLines(lines, descriptors)

        // 折叠注释块
        foldCommentBlocks(element, descriptors)

        return descriptors.toTypedArray()
    }

    /**
     * 折叠连续的相似行
     *
     * 相似行定义：
     * - 所有字节都是相同值（如全0、全FF）
     * - 连续3行以上
     */
    private fun foldSimilarLines(lines: List<PsiElement>, descriptors: MutableList<FoldingDescriptor>) {
        var startLine: PsiElement? = null
        var startPattern: String? = null
        var count = 0

        for (line in lines) {
            val pattern = extractPattern(line)

            if (pattern != null && pattern == startPattern) {
                // 相同模式，继续累积
                count++
            } else {
                // 模式改变，检查是否需要折叠
                if (count >= 3 && startLine != null) {
                    val endLine = lines[lines.indexOf(line) - 1]
                    addFoldingDescriptor(startLine, endLine, count, descriptors)
                }

                // 开始新的序列
                startLine = line
                startPattern = pattern
                count = 1
            }
        }

        // 处理最后一个序列
        if (count >= 3 && startLine != null) {
            val endLine = lines.last()
            addFoldingDescriptor(startLine, endLine, count, descriptors)
        }
    }

    /**
     * 提取行的模式
     *
     * 如果所有字节都是相同值，返回该值；否则返回null
     */
    private fun extractPattern(line: PsiElement): String? {
        val text = line.text
        // 提取十六进制部分
        val hexMatch = Regex(""":\s*((?:[0-9A-Fa-f]{2}\s*)+)""").find(text) ?: return null
        val hexBytes = hexMatch.groupValues[1].trim().split(Regex("""\s+"""))

        if (hexBytes.isEmpty()) return null

        // 检查是否所有字节都相同
        val firstByte = hexBytes[0]
        return if (hexBytes.all { it == firstByte }) {
            firstByte
        } else {
            null
        }
    }

    /**
     * 添加折叠描述符
     */
    private fun addFoldingDescriptor(
        startLine: PsiElement,
        endLine: PsiElement,
        count: Int,
        descriptors: MutableList<FoldingDescriptor>
    ) {
        val startOffset = startLine.textRange.startOffset
        val endOffset = endLine.textRange.endOffset

        // 计算占位符文本
        val pattern = extractPattern(startLine) ?: "??"
        val placeholderText = "... $count lines with 0x$pattern ..."

        descriptors.add(
            FoldingDescriptor(
                startLine.node,
                TextRange(startOffset, endOffset),
                null,
                placeholderText
            )
        )
    }

    /**
     * 折叠注释块
     *
     * 连续3行以上的注释可以折叠
     */
    private fun foldCommentBlocks(element: PsiElement, descriptors: MutableList<FoldingDescriptor>) {
        val comments = PsiTreeUtil.findChildrenOfType(element, PsiElement::class.java)
            .filter { it.node.elementType == HexdumpTokenTypes.COMMENT }

        var startComment: PsiElement? = null
        var count = 0
        var lastEndOffset = -1

        for (comment in comments) {
            val startOffset = comment.textRange.startOffset

            // 检查是否连续
            if (lastEndOffset >= 0 && startOffset - lastEndOffset > 10) {
                // 不连续，检查是否需要折叠
                if (count >= 3 && startComment != null) {
                    val endComment = comments[comments.indexOf(comment) - 1]
                    addCommentFoldingDescriptor(startComment, endComment, count, descriptors)
                }

                // 重新开始
                startComment = comment
                count = 1
            } else {
                if (startComment == null) {
                    startComment = comment
                }
                count++
            }

            lastEndOffset = comment.textRange.endOffset
        }

        // 处理最后一个序列
        if (count >= 3 && startComment != null) {
            val endComment = comments.last()
            addCommentFoldingDescriptor(startComment, endComment, count, descriptors)
        }
    }

    /**
     * 添加注释折叠描述符
     */
    private fun addCommentFoldingDescriptor(
        startComment: PsiElement,
        endComment: PsiElement,
        count: Int,
        descriptors: MutableList<FoldingDescriptor>
    ) {
        val startOffset = startComment.textRange.startOffset
        val endOffset = endComment.textRange.endOffset

        descriptors.add(
            FoldingDescriptor(
                startComment.node,
                TextRange(startOffset, endOffset),
                null,
                "... $count comment lines ..."
            )
        )
    }

    override fun getPlaceholderText(node: ASTNode): String {
        return "..."
    }

    override fun isCollapsedByDefault(node: ASTNode): Boolean {
        // 默认展开，不自动折叠
        return false
    }
}