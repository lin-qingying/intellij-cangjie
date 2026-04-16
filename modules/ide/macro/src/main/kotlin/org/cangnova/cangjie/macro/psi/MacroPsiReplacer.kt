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

package org.cangnova.cangjie.macro.psi

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.CjPsiFactory
import org.cangnova.cangjie.resolve.source.MacroExpandedSourceElement

/**
 * 宏 PSI 替换器
 *
 * 通过文本替换 + 重新解析的方式执行宏展开替换：
 * 1. 在源文件文本上将宏调用替换为展开文本
 * 2. 用替换后的文本重新解析为完整的 PSI 树
 * 3. 对展开区域内的 PSI 元素附加 [MacroSourceInfo]
 *
 * 这确保展开后的声明（如 class）在 PSI 树中处于正确的层级，
 * 能被 [CjFile.declarations] 正确识别。
 */
object MacroPsiReplacer {

    private val LOG = Logger.getInstance(MacroPsiReplacer::class.java)

    /**
     * 单个宏展开的替换信息
     *
     * @param macroExpression 宏表达式 PSI 节点
     * @param expandedText 展开后的文本
     * @param macroName 宏名称
     */
    data class MacroReplacement(
        val macroExpression: CjMacroExpression,
        val expandedText: String,
        val macroName: String
    )

    /**
     * 对文件执行宏替换，返回重新解析的文件
     *
     * @param originalFile 原始源文件
     * @param replacements 宏替换列表（基于原始文件中的 PSI 节点）
     * @return 替换后重新解析的文件，如果没有替换则返回 null
     */
    fun replaceInCopy(originalFile: CjFile, replacements: List<MacroReplacement>): CjFile? {
        if (replacements.isEmpty()) return null

        val factory = CjPsiFactory(originalFile.project)
        val originalText = originalFile.text

        // 按偏移量降序排列，从后往前替换文本以避免偏移漂移
        val sortedReplacements = replacements.sortedByDescending { it.macroExpression.textOffset }

        val sb = StringBuilder(originalText)
        // 记录每个替换在新文本中的范围和元信息（从后往前替换，前面的偏移量不变）
        val expandedRanges = mutableListOf<ExpandedRange>()

        for (replacement in sortedReplacements) {
            val range = replacement.macroExpression.textRange
            sb.replace(range.startOffset, range.endOffset, replacement.expandedText)

            expandedRanges.add(
                ExpandedRange(
                    startInNewText = range.startOffset,
                    endInNewText = range.startOffset + replacement.expandedText.length,
                    originalRange = TextRange(range.startOffset, range.endOffset),
                    macroName = replacement.macroName,
                    originalFilePath = originalFile.virtualFile?.path ?: "",
                    macroExpression = replacement.macroExpression
                )
            )
        }

        // 用替换后的文本重新解析为完整的 CjFile
        val newFile = try {
            factory.createFile(originalFile.name, sb.toString()) as? CjFile
        } catch (e: Exception) {
            LOG.warn("重新解析宏展开后的文件失败", e)
            null
        } ?: return null

        // 对展开区域内的 PSI 元素附加 MacroSourceInfo + MACRO_EXPRESSION_KEY
        var markedCount = 0
        for (expandedRange in expandedRanges) {
            val sourceInfo = MacroSourceInfo(
                originalFilePath = expandedRange.originalFilePath,
                originalTextRange = expandedRange.originalRange,
                macroName = expandedRange.macroName
            )
            if (markElementsInRange(
                    newFile,
                    expandedRange.startInNewText,
                    expandedRange.endInNewText,
                    sourceInfo,
                    expandedRange.macroExpression
                )
            ) {
                markedCount++
            }
        }

        return if (markedCount > 0) newFile else null
    }

    /**
     * 替换后的范围信息
     */
    private data class ExpandedRange(
        val startInNewText: Int,
        val endInNewText: Int,
        val originalRange: TextRange,
        val macroName: String,
        val originalFilePath: String,
        val macroExpression: CjMacroExpression
    )

    /**
     * 对指定范围内的 PSI 元素附加 [MacroSourceInfo] 和 [MacroExpandedSourceElement.MACRO_EXPRESSION_KEY]
     *
     * 遍历 PSI 树，对文本范围完全落在展开区域内的元素标记为宏生成。
     *
     * @return 是否成功标记了至少一个元素
     */
    private fun markElementsInRange(
        root: PsiElement,
        rangeStart: Int,
        rangeEnd: Int,
        sourceInfo: MacroSourceInfo,
        macroExpression: CjMacroExpression
    ): Boolean {
        var marked = false
        var child = root.firstChild
        while (child != null) {
            val childStart = child.textRange.startOffset
            val childEnd = child.textRange.endOffset

            if (childStart >= rangeStart && childEnd <= rangeEnd) {
                // 子元素完全在展开范围内，标记整棵子树
                attachSourceInfo(child, sourceInfo, macroExpression)
                marked = true
            } else if (childEnd > rangeStart && childStart < rangeEnd) {
                // 子元素与展开范围部分重叠，递归检查更深层
                if (markElementsInRange(child, rangeStart, rangeEnd, sourceInfo, macroExpression)) {
                    marked = true
                }
            }

            child = child.nextSibling
        }
        return marked
    }

    /**
     * 将 [MacroSourceInfo] 和 [MacroExpandedSourceElement.MACRO_EXPRESSION_KEY] 附加到 PSI 元素及其所有子节点
     *
     * - [MacroSourceInfo.KEY]: 用于 [MacroSyntheticResolveExtension] 判断元素是否由宏生成
     * - [MACRO_EXPRESSION_KEY]: 用于 [toSourceElement] 自动创建 [MacroExpandedSourceElement]，
     *   使导航跳转到原始宏表达式，快速文档显示展开后的内容
     */
    private fun attachSourceInfo(element: PsiElement, sourceInfo: MacroSourceInfo, macroExpression: CjMacroExpression) {
        element.putUserData(MacroSourceInfo.KEY, sourceInfo)
        element.putUserData(MacroExpandedSourceElement.MACRO_EXPRESSION_KEY, macroExpression)
        var child = element.firstChild
        while (child != null) {
            attachSourceInfo(child, sourceInfo, macroExpression)
            child = child.nextSibling
        }
    }
}
