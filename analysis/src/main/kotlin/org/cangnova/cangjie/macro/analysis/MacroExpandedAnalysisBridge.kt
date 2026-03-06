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

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.TextRange
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.PsiManager
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.macro.expanded.MacroExpandedFileManager
import org.cangnova.cangjie.macro.expanded.MacroExpansionOffsetMapping
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.AnalysisResult
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.caches.analyzeWithAllCompilerChecks
import org.cangnova.cangjie.types.CangJieType

/**
 * 宏展开分析桥
 *
 * 桥接展开文件的语义分析结果到源文件，使 IDE 能够：
 * - 在源文件的宏调用处显示展开后的类型信息
 * - 将展开文件的诊断映射回源文件位置
 * - 为宏表达式提供正确的类型推导
 *
 * ## 工作流程
 *
 * ```
 * 源文件 (main.cj, 含 @macro 调用)
 *   ↓ getExpandedPsiFile()
 * 展开文件 (macro-expanded/main.cj, 干净代码)
 *   ↓ analyzeExpandedFile()
 * AnalysisResult (BindingContext + 诊断)
 *   ↓ mapDiagnosticsToSource() / getTypeAtOffset()
 * 映射回源文件位置
 * ```
 */
@Service(Service.Level.PROJECT)
class MacroExpandedAnalysisBridge(private val project: Project) {

    private val log = Logger.getInstance(MacroExpandedAnalysisBridge::class.java)

    /**
     * 映射后的诊断信息
     *
     * @param diagnostic 原始诊断
     * @param mappedRange 映射到源文件的文本范围
     * @param isInMacroExpansion 是否来自宏展开区域
     */
    data class MappedDiagnostic(
        val diagnostic: Diagnostic,
        val mappedRange: TextRange,
        val isInMacroExpansion: Boolean
    )

    /**
     * 检查宏展开分析功能是否启用
     */
    fun isEnabled(): Boolean {
        return Registry.`is`("cangjie.macro.expansion.analysis.enabled")
    }

    /**
     * 获取源文件对应的展开文件 CjFile PSI
     *
     * @param sourceFile 源文件
     * @return 展开文件的 CjFile PSI，不存在时返回 null
     */
    fun getExpandedPsiFile(sourceFile: CjFile): CjFile? {
        val vf = sourceFile.virtualFile ?: return null
        val manager = MacroExpandedFileManager.getInstance(project)
        val expandedVf = manager.getExpandedFile(vf) ?: return null
        return PsiManager.getInstance(project).findFile(expandedVf) as? CjFile
    }

    /**
     * 分析展开文件，返回 AnalysisResult
     *
     * 调用展开文件的完整语义分析管线，获取类型推导和诊断信息。
     *
     * @param sourceFile 源文件（用于查找对应的展开文件）
     * @return 展开文件的分析结果，分析失败或无展开文件时返回 null
     */
    fun analyzeExpandedFile(sourceFile: CjFile): AnalysisResult? {
        if (!isEnabled()) return null

        val expandedFile = getExpandedPsiFile(sourceFile) ?: return null
        return try {
            expandedFile.analyzeWithAllCompilerChecks()
        } catch (e: Exception) {
            log.debug("宏展开文件分析失败: ${expandedFile.virtualFile?.path}", e)
            null
        }
    }

    /**
     * 从展开文件的 BindingContext 中查询源文件某偏移处的类型
     *
     * 用于悬停显示类型信息。将源文件偏移映射到展开文件偏移，
     * 然后在展开文件的 BindingContext 中查找对应位置表达式的类型。
     *
     * @param sourceFile 源文件
     * @param offset 源文件中的偏移
     * @return 对应位置的类型，无法映射或无类型时返回 null
     */
    fun getTypeAtOffset(sourceFile: CjFile, offset: Int): CangJieType? {
        if (!isEnabled()) return null

        val expandedFile = getExpandedPsiFile(sourceFile) ?: return null
        val mapping = getOffsetMapping(sourceFile) ?: return null

        val sourceText = sourceFile.text
        val expandedText = expandedFile.text

        // 计算源文件偏移对应的行号
        val sourceLine = lineNumberAtOffset(sourceText, offset)

        // 获取展开文件行号
        val expandedLine = mapping.originalLineToExpandedLine(sourceLine) ?: return null

        // 检查是否在宏展开区域
        val region = mapping.findRegionsByOriginalLine(sourceLine)
        if (region.isEmpty()) return null  // 非宏区域，不需要从展开文件获取类型

        // 分析展开文件
        val analysisResult = try {
            expandedFile.analyzeWithAllCompilerChecks()
        } catch (e: Exception) {
            log.debug("宏展开文件分析失败", e)
            return null
        }

        if (analysisResult.isError()) return null
        val bindingContext = analysisResult.bindingContext

        // 在展开文件中查找对应行的表达式及其类型
        val expandedLineStart = lineStartOffset(expandedText, expandedLine)
        val expandedLineEnd = lineEndOffset(expandedText, expandedLine)

        return findTypeInRange(expandedFile, bindingContext, expandedLineStart, expandedLineEnd)
    }

    /**
     * 将展开文件的诊断映射回源文件位置
     *
     * @param sourceFile 源文件
     * @param expandedDiagnostics 展开文件产生的诊断集合
     * @return 映射后的诊断列表
     */
    fun mapDiagnosticsToSource(
        sourceFile: CjFile,
        expandedDiagnostics: Collection<Diagnostic>
    ): List<MappedDiagnostic> {
        val mapping = getOffsetMapping(sourceFile) ?: return emptyList()
        if (mapping.lineMappings.isEmpty()) return emptyList()

        val expandedFile = getExpandedPsiFile(sourceFile) ?: return emptyList()
        val expandedText = expandedFile.text
        val sourceText = sourceFile.text

        return expandedDiagnostics.mapNotNull { diagnostic ->
            mapSingleDiagnostic(diagnostic, mapping, expandedText, sourceText)
        }
    }

    private fun mapSingleDiagnostic(
        diagnostic: Diagnostic,
        mapping: MacroExpansionOffsetMapping,
        expandedText: String,
        sourceText: String
    ): MappedDiagnostic? {
        val psiElement = diagnostic.psiElement
        val expandedRange = psiElement.textRange ?: return null

        val mapped = mapping.mapExpandedRangeToOriginal(
            expandedRange.startOffset,
            expandedRange.endOffset,
            expandedText,
            sourceText
        ) ?: return null

        val (mappedStart, mappedEnd) = mapped
        if (mappedStart < 0 || mappedEnd > sourceText.length || mappedStart >= mappedEnd) return null

        val expandedLine = lineNumberAtOffset(expandedText, expandedRange.startOffset)
        val isInMacro = mapping.findExpansionRegion(expandedLine) != null

        return MappedDiagnostic(
            diagnostic = diagnostic,
            mappedRange = TextRange(mappedStart, mappedEnd),
            isInMacroExpansion = isInMacro
        )
    }

    /**
     * 获取源文件的偏移映射
     */
    private fun getOffsetMapping(sourceFile: CjFile): MacroExpansionOffsetMapping? {
        val vf = sourceFile.virtualFile ?: return null
        return MacroExpandedFileManager.getInstance(project).getOffsetMapping(vf.path)
    }

    /**
     * 在展开文件的指定范围内查找表达式类型
     */
    private fun findTypeInRange(
        expandedFile: CjFile,
        bindingContext: BindingContext,
        startOffset: Int,
        endOffset: Int
    ): CangJieType? {
        var offset = startOffset
        while (offset < endOffset) {
            val element = expandedFile.findElementAt(offset)
            if (element != null) {
                // 向上查找最近的表达式节点
                var current = element
                while (current != null && current.textOffset >= startOffset && current.textOffset < endOffset) {
                    if (current is org.cangnova.cangjie.psi.CjExpression) {
                        val type = bindingContext.getType(current)
                        if (type != null) return type
                    }
                    current = current.parent
                }
                offset = element.textRange.endOffset
            } else {
                offset++
            }
        }
        return null
    }

    /**
     * 合并源文件和展开文件的分析结果
     *
     * 这是核心合并入口。在 ResolutionFacade 层调用此方法，
     * 将展开文件的 BindingContext 合并进源文件的 AnalysisResult，
     * 使所有下游消费者（高亮、补全、悬停等）自动获得宏展开后的分析信息。
     *
     * @param sourceFile 源文件
     * @param sourceResult 源文件的原始分析结果
     * @return 合并后的 AnalysisResult，无法合并时返回原始结果
     */
    fun mergeAnalysisResults(sourceFile: CjFile, sourceResult: AnalysisResult): AnalysisResult {
        if (sourceResult.isError()) return sourceResult

        val expandedFile = getExpandedPsiFile(sourceFile) ?: return sourceResult
        val mapping = getOffsetMapping(sourceFile) ?: return sourceResult
        if (mapping.lineMappings.isEmpty()) return sourceResult

        val expandedResult = try {
            expandedFile.analyzeWithAllCompilerChecks()
        } catch (e: Exception) {
            log.debug("宏展开文件分析合并失败: ${expandedFile.virtualFile?.path}", e)
            return sourceResult
        }

        if (expandedResult.isError()) return sourceResult

        val mergedBindingContext = MacroMergedBindingContext(
            sourceContext = sourceResult.bindingContext,
            expandedContext = expandedResult.bindingContext,
            expandedFile = expandedFile,
            sourceFile = sourceFile,
            mapping = mapping
        )

        return AnalysisResult.success(
            mergedBindingContext,
            sourceResult.moduleDescriptor,
            sourceResult.shouldGenerateCode
        )
    }

    companion object {
        @JvmStatic
        fun getInstance(project: Project): MacroExpandedAnalysisBridge {
            return project.getService(MacroExpandedAnalysisBridge::class.java)
        }

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
