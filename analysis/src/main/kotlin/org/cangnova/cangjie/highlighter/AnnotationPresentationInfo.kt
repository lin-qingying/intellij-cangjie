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

package org.cangnova.cangjie.highlighter


import com.intellij.codeInsight.daemon.HighlightDisplayKey
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.HighlightInfoType
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInsight.intention.IntentionActionWithOptions
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.SuppressableProblemGroup
import com.intellij.codeInspection.util.IntentionFamilyName
import com.intellij.codeInspection.util.IntentionName
import com.intellij.lang.annotation.ProblemGroup
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.editor.colors.TextAttributesKey
import com.intellij.openapi.project.Project
import com.intellij.openapi.util.IntellijInternalApi
import com.intellij.openapi.util.NlsContexts
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiReference
import com.intellij.util.containers.MultiMap
import com.intellij.xml.util.XmlStringUtil
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.Severity
import org.cangnova.cangjie.diagnostics.rendering.DefaultErrorMessages
import org.cangnova.cangjie.diagnostics.rendering.DiagnosticRendererRegistry
import org.cangnova.cangjie.diagnostics.rendering.IdeErrorMessages
import org.cangnova.cangjie.inspections.suppress.CangJieSuppressableWarningProblemGroup
import org.cangnova.cangjie.utils.isApplicationInternalMode
import org.cangnova.cangjie.utils.isUnitTestMode

import org.jetbrains.annotations.Nls


class AnnotationPresentationInfo(
    val ranges: List<TextRange>,
    @Nls val nonDefaultMessage: String? = null,
    val highlightType: ProblemHighlightType? = null,
    val textAttributes: TextAttributesKey? = null
) {
    companion object {
        private const val CANGJIE_COMPILER_WARNING_ID = "CangJieCompilerWarningOptions"
    }

    private fun getDefaultMessage(diagnostic: Diagnostic): String {

        val message = DefaultErrorMessages.render(diagnostic)
        return if (isApplicationInternalMode() || isUnitTestMode) {
            "[${diagnostic.factory.name}] $message"
        } else {
            message
        }
    }

    private fun convertSeverityTextAttributes(
        highlightType: ProblemHighlightType?,
        severity: Severity
    ): TextAttributesKey? =
        when (highlightType) {
            null, ProblemHighlightType.GENERIC_ERROR_OR_WARNING ->
                when (severity) {
                    Severity.ERROR -> CodeInsightColors.ERRORS_ATTRIBUTES
                    Severity.WARNING -> CodeInsightColors.WARNINGS_ATTRIBUTES
                    Severity.INFO -> CodeInsightColors.WARNINGS_ATTRIBUTES

                }

            ProblemHighlightType.GENERIC_ERROR -> CodeInsightColors.ERRORS_ATTRIBUTES
            else -> null
        }

    private fun convertSeverity(highlightType: ProblemHighlightType?, severity: Severity): HighlightInfoType =
        when (severity) {
            Severity.ERROR -> HighlightInfoType.ERROR
            Severity.WARNING -> {
                if (highlightType == ProblemHighlightType.WEAK_WARNING) {
                    HighlightInfoType.WEAK_WARNING
                } else HighlightInfoType.WARNING
            }

            Severity.INFO -> HighlightInfoType.WEAK_WARNING
//            else -> HighlightInfoType.INFORMATION
        }

    private fun toHighlightInfoType(highlightType: ProblemHighlightType?, severity: Severity): HighlightInfoType =
        when (highlightType) {
            ProblemHighlightType.LIKE_UNUSED_SYMBOL -> HighlightInfoType.UNUSED_SYMBOL
            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL -> HighlightInfoType.WRONG_REF
            ProblemHighlightType.LIKE_DEPRECATED -> HighlightInfoType.DEPRECATED
            ProblemHighlightType.LIKE_MARKED_FOR_REMOVAL -> HighlightInfoType.MARKED_FOR_REMOVAL
            else -> convertSeverity(highlightType, severity)
        }

    private fun create(
        diagnostic: Diagnostic,
        range: TextRange,
        group: CangJieSuppressableWarningProblemGroup?
    ): HighlightInfo.Builder {
        val message = if (nonDefaultMessage.isNullOrEmpty()) getDefaultMessage(diagnostic) else nonDefaultMessage
        val textAttributesToApply = if (textAttributes != null) {
            textAttributes
        } else {
            convertSeverityTextAttributes(highlightType, diagnostic.severity)
        }
        return HighlightInfo
            .newHighlightInfo(toHighlightInfoType(highlightType, diagnostic.severity))
            .range(range)
            .description(message)
            .escapedToolTip(getMessage(diagnostic))
            .also {
                if (textAttributesToApply != null) {
                    it.textAttributes(textAttributesToApply)
                }
            }
            .also {
                if (group != null) {
                    it.problemGroup(group)
                }
            }
    }

    @NlsContexts.Tooltip
    private fun getMessage(diagnostic: Diagnostic): String {
        var message = IdeErrorMessages.render(diagnostic)
        if (isApplicationInternalMode() || isUnitTestMode) {
            val factoryName = diagnostic.factory.name
            message = if (message.startsWith("<html>")) {
                @Suppress("HardCodedStringLiteral")
                "<html>[$factoryName] ${message.substring("<html>".length)}"
            } else {
                "[$factoryName] $message"
            }
        }
        if (!message.startsWith("<html>")) {
            message = "<html><body>${XmlStringUtil.escapeString(message)}</body></html>"
        }
        return message
    }

    private fun applyFixes(
        quickFixes: MultiMap<Diagnostic, IntentionAction>,
        diagnostic: Diagnostic,
        range: TextRange,
        builder: HighlightInfo.Builder?,
        highlightInfo: HighlightInfo?,
        problemGroup: ProblemGroup?
    ) {
        val isWarning = diagnostic.severity == Severity.WARNING

        val element = diagnostic.psiElement

        val fixes = quickFixes[diagnostic].takeIf { it.isNotEmpty() }
            ?: if (isWarning) listOf( TODO()/*CompilerWarningIntentionAction(diagnostic.factory.name)*/) else emptyList()

        val keyForSuppressOptions = if (isWarning) {
            HighlightDisplayKey.findOrRegister(
                CANGJIE_COMPILER_WARNING_ID,
                CangJieHighlightingBundle.message("cangjie.compiler.warning")
            )
        } else null


        for (fix in fixes) {
            if (fix !is IntentionAction) {
                continue
            }


            if (fix == RegisterQuickFixesLaterIntentionAction) {
                if (builder != null) {
                    // TODO 兼容性调整：UnresolvedReferenceQuickFixUpdater 在新版本 IntelliJ Platform 中可能已移除
                    // 暂时跳过此逻辑
                    continue
                }
            }
            val options = mutableListOf<IntentionAction>()

            if (fix is IntentionActionWithOptions) {
                options += fix.options
            }

            if (problemGroup is SuppressableProblemGroup) {
                options += problemGroup.getSuppressActions(element).mapNotNull { it as IntentionAction }
            }

            val isError = diagnostic.severity == Severity.ERROR
            val message =
                CangJieHighlightingBundle.message(if (isError) "cangjie.compiler.error" else "cangjie.compiler.warning")
            builder?.registerFix(fix, options, message, range, keyForSuppressOptions)
//            highlightInfo?.registerFix(fix, options, message, range, keyForSuppressOptions)

        }

    }

    /**
     * 处理诊断信息，将其转换为编辑器中的高亮信息
     *
     * @param holder HighlightInfoHolder对象，用于存储高亮信息
     * @param diagnostics 一组诊断信息，用于识别和处理代码问题
     * @param highlightInfoByDiagnostic 一个可变映射，将诊断信息映射到高亮信息
     * @param fixesMap 多重映射，将诊断信息映射到多个快速修复动作
     * @param calculatingInProgress 布尔值，指示是否正在计算诊断信息
     */
    fun processDiagnostics(
        holder: HighlightInfoHolder,
        diagnostics: Collection<Diagnostic>,
        highlightInfoByDiagnostic: MutableMap<Diagnostic, HighlightInfo>?,
        fixesMap: MultiMap<Diagnostic, IntentionAction>,
        calculatingInProgress: Boolean
    ) {
        // 遍历所有范围，对每个范围内的诊断信息进行处理
        for (range in ranges) {
            // 对每个诊断信息进行处理
            for (diagnostic in diagnostics) {
                // 根据诊断信息的严重性决定是否创建一个可抑制的警告问题组
                val group = if (diagnostic.severity == Severity.WARNING) {
                    CangJieSuppressableWarningProblemGroup(diagnostic.factory.name)
                } else {
                    null
                }
                // 检查是否已经存在与当前诊断信息关联的高亮信息
                val existingInfo = highlightInfoByDiagnostic?.get(diagnostic)
                if (existingInfo != null) {
                    // 如果存在，并且不在计算诊断信息的过程中，则应用快速修复
                    if (!calculatingInProgress) {
                        applyFixes(
                            fixesMap,
                            diagnostic,
                            range,
                            builder = null,
                            highlightInfo = existingInfo,
                            problemGroup = group
                        )
                    }
                } else {
                    // 如果不存在，则创建一个新的高亮信息构建器
                    val builder = create(diagnostic, range, group)
                    // 如果不在计算诊断信息的过程中，或者有快速修复可以应用，则进行处理
                    if (!calculatingInProgress || !fixesMap.isEmpty) {
                        applyFixes(
                            fixesMap,
                            diagnostic,
                            range,
                            builder = builder,
                            highlightInfo = null,
                            problemGroup = group
                        )
                    }
                    // 无条件创建高亮信息，并添加到持有者中
                    val highlightInfo = builder.createUnconditionally()


                    holder.add(highlightInfo)
                    // 将诊断信息和对应的高亮信息添加到映射中
                    highlightInfoByDiagnostic?.put(diagnostic, highlightInfo)

                }
            }
        }
    }
}
