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

package org.cangnova.cangjie.macro.analysis

import com.google.common.collect.ImmutableMap
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.Diagnostics
import org.cangnova.cangjie.macro.expanded.MacroExpansionOffsetMapping
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.slicedMap.ReadOnlySlice
import org.cangnova.cangjie.resolve.binding.slicedMap.WritableSlice
import org.cangnova.cangjie.types.CangJieType

/**
 * 宏合并 BindingContext
 *
 * 组合源文件的 BindingContext 和展开文件的 BindingContext。
 * 查询策略：
 * 1. 先查源文件的 BindingContext（非宏区域的正常分析结果）
 * 2. 若查不到且 PSI 元素位于宏展开区域内，通过偏移映射在展开文件的 BindingContext 中查找对应元素
 *
 * 诊断合并策略：
 * - 源文件的所有诊断照常返回
 * - 展开文件中仅属于宏展开区域的诊断被纳入（避免非宏区域的重复诊断）
 *
 * @param sourceContext 源文件的 BindingContext
 * @param expandedContext 展开文件的 BindingContext
 * @param expandedFile 展开后的 CjFile PSI
 * @param sourceFile 原始源文件 PSI
 * @param mapping 偏移映射关系
 */
class MacroMergedBindingContext(
    private val sourceContext: BindingContext,
    private val expandedContext: BindingContext,
    private val expandedFile: CjFile,
    private val sourceFile: CjFile,
    private val mapping: MacroExpansionOffsetMapping
) : BindingContext {

    override val diagnostics: Diagnostics = MergedDiagnostics(
        sourceContext.diagnostics,
        expandedContext.diagnostics,
        mapping,
        expandedFile
    )

    override fun <K : Any, V : Any> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        // 先查源文件
        sourceContext[slice, key]?.let { return it }

        // 如果 key 是源文件的 PSI 元素，且在宏展开区域内，尝试从展开文件查找
        if (key is PsiElement && isInMacroExpansionRegion(key)) {
            val expandedElement = findCorrespondingExpandedElement(key)
            if (expandedElement != null) {
                @Suppress("UNCHECKED_CAST")
                expandedContext[slice, expandedElement as K]?.let { return it }
            }
        }

        return null
    }

    override fun <K : Any, V : Any> getKeys(slice: WritableSlice<K, V>): Collection<K> {
        // 合并两个 context 的 keys
        val sourceKeys = sourceContext.getKeys(slice)
        val expandedKeys = expandedContext.getKeys(slice)
        return sourceKeys + expandedKeys
    }

    override fun <K : Any, V : Any> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
        val map = hashMapOf<K, V>()
        map.putAll(sourceContext.getSliceContents(slice))
        map.putAll(expandedContext.getSliceContents(slice))
        return ImmutableMap.copyOf(map)
    }

    override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) {
        // Do nothing
    }

    override fun getType(expression: CjExpression): CangJieType? {
        // 先查源文件
        sourceContext.getType(expression)?.let { return it }

        // 如果在宏展开区域内，尝试从展开文件查找
        if (isInMacroExpansionRegion(expression)) {
            val expandedElement = findCorrespondingExpandedElement(expression)
            if (expandedElement is CjExpression) {
                expandedContext.getType(expandedElement)?.let { return it }
            }
        }

        return null
    }

    /**
     * 检查源文件中的 PSI 元素是否位于宏展开区域内
     */
    private fun isInMacroExpansionRegion(element: PsiElement): Boolean {
        if (element.containingFile != sourceFile) return false
        val sourceText = sourceFile.text
        val offset = element.textOffset
        val line = lineNumberAtOffset(sourceText, offset)
        return mapping.findRegionsByOriginalLine(line).isNotEmpty()
    }

    /**
     * 在展开文件中查找源文件 PSI 元素对应的元素
     *
     * 基于偏移映射，将源文件的偏移转换为展开文件的偏移，
     * 然后在展开文件 PSI 中查找同类型的元素。
     */
    private fun findCorrespondingExpandedElement(sourceElement: PsiElement): PsiElement? {
        val sourceOffset = sourceElement.textOffset
        val sourceText = sourceFile.text
        val expandedText = expandedFile.text
        val sourceLine = lineNumberAtOffset(sourceText, sourceOffset)

        // 获取展开文件中对应的行号
        val expandedLine = mapping.originalLineToExpandedLine(sourceLine) ?: return null
        val expandedLineStart = lineStartOffset(expandedText, expandedLine)

        // 计算列偏移
        val sourceLineStart = lineStartOffset(sourceText, sourceLine)
        val sourceCol = sourceOffset - sourceLineStart

        // 对于宏展开区域，列不直接对应，尝试在展开行的范围内查找同类型元素
        val region = mapping.findRegionsByOriginalLine(sourceLine).firstOrNull()
        if (region != null) {
            // 在展开区域内查找：遍历展开文件的展开行范围
            val expandedRegionStart = lineStartOffset(expandedText, region.expandedStartLine)
            val expandedRegionEnd = lineEndOffset(expandedText, region.expandedEndLine)

            return findSimilarElementInRange(
                expandedFile, expandedRegionStart, expandedRegionEnd, sourceElement
            )
        }

        // 非宏区域：直接按列偏移查找
        val expandedOffset = (expandedLineStart + sourceCol).coerceAtMost(expandedText.length)
        return expandedFile.findElementAt(expandedOffset)?.let { leaf ->
            findParentOfType(leaf, sourceElement.javaClass)
        }
    }

    /**
     * 在展开文件的指定范围内查找与源元素类型匹配的元素
     */
    private fun findSimilarElementInRange(
        file: CjFile,
        startOffset: Int,
        endOffset: Int,
        sourceElement: PsiElement
    ): PsiElement? {
        var offset = startOffset
        while (offset < endOffset) {
            val leaf = file.findElementAt(offset) ?: run { offset++; continue }
            // 向上查找与源元素相同类型的父节点
            val match = findParentOfType(leaf, sourceElement.javaClass)
            if (match != null) return match
            offset = leaf.textRange.endOffset
        }
        return null
    }

    private fun findParentOfType(element: PsiElement, targetType: Class<*>): PsiElement? {
        var current: PsiElement? = element
        while (current != null) {
            if (targetType.isInstance(current)) return current
            current = current.parent
        }
        return null
    }

    /**
     * 合并诊断：源文件诊断 + 展开文件中宏区域内的诊断
     */
    private class MergedDiagnostics(
        private val sourceDiagnostics: Diagnostics,
        private val expandedDiagnostics: Diagnostics,
        private val mapping: MacroExpansionOffsetMapping,
        private val expandedFile: CjFile
    ) : Diagnostics {

        override val modificationTracker = ModificationTracker {
            sourceDiagnostics.modificationTracker.modificationCount +
                    expandedDiagnostics.modificationTracker.modificationCount
        }

        override fun iterator(): Iterator<Diagnostic> = all().iterator()

        override fun all(): Collection<Diagnostic> {
            val result = mutableListOf<Diagnostic>()
            result.addAll(sourceDiagnostics.all())
            // 只添加展开文件中宏展开区域内的诊断
            result.addAll(expandedMacroDiagnostics())
            return result
        }

        override fun forElement(psiElement: PsiElement): Collection<Diagnostic> {
            val result = mutableListOf<Diagnostic>()
            result.addAll(sourceDiagnostics.forElement(psiElement))
            // 展开文件的诊断关联的是展开文件的 PSI 元素，不直接匹配源文件元素
            return result
        }

        override fun isEmpty(): Boolean =
            sourceDiagnostics.isEmpty() && expandedMacroDiagnostics().isEmpty()

        override fun noSuppression(): Diagnostics =
            MergedDiagnostics(
                sourceDiagnostics.noSuppression(),
                expandedDiagnostics.noSuppression(),
                mapping,
                expandedFile
            )

        private fun expandedMacroDiagnostics(): List<Diagnostic> {
            val expandedText = expandedFile.text
            return expandedDiagnostics.all().filter { diagnostic ->
                val offset = diagnostic.psiElement.textOffset
                val line = lineNumberAtOffset(expandedText, offset)
                mapping.findExpansionRegion(line) != null
            }
        }
    }

    companion object {
        private fun lineNumberAtOffset(text: String, offset: Int): Int {
            var line = 1
            for (i in 0 until offset.coerceAtMost(text.length)) {
                if (text[i] == '\n') line++
            }
            return line
        }

        private fun lineStartOffset(text: String, line: Int): Int {
            if (line <= 1) return 0
            var currentLine = 1
            for (i in text.indices) {
                if (text[i] == '\n') {
                    currentLine++
                    if (currentLine == line) return i + 1
                }
            }
            return text.length
        }

        private fun lineEndOffset(text: String, line: Int): Int {
            var currentLine = 1
            for (i in text.indices) {
                if (text[i] == '\n') {
                    if (currentLine == line) return i
                    currentLine++
                }
            }
            return text.length
        }
    }
}
