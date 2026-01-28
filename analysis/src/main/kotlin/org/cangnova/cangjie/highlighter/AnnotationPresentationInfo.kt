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

/**
 * 注解展示信息类
 *
 * 用于管理和展示代码诊断信息在编辑器中的高亮显示。
 * 该类负责将编译器的诊断信息（错误、警告等）转换为 IntelliJ 编辑器可识别的高亮信息。
 *
 * @property ranges 需要高亮显示的文本范围列表
 * @property nonDefaultMessage 自定义的非默认消息文本（可选）
 * @property highlightType 问题高亮类型，决定如何在编辑器中展示问题（可选）
 * @property textAttributes 文本属性键，用于自定义高亮的视觉样式（可选）
 */
class AnnotationPresentationInfo(
    val ranges: List<TextRange>,
    @field:Nls val nonDefaultMessage: String? = null,
    val highlightType: ProblemHighlightType? = null,
    val textAttributes: TextAttributesKey? = null
) {
    companion object {
        /** 仓颉编译器警告的唯一标识符 */
        private const val CANGJIE_COMPILER_WARNING_ID = "CangJieCompilerWarningOptions"
    }

    /**
     * 获取诊断信息的默认消息
     *
     * 根据当前运行模式（内部模式或单元测试模式）决定消息格式：
     * - 内部模式/测试模式：包含诊断工厂名称，格式为 "[工厂名] 消息内容"
     * - 普通模式：仅显示消息内容
     *
     * @param diagnostic 诊断信息对象
     * @return 格式化后的默认消息字符串
     */
    private fun getDefaultMessage(diagnostic: Diagnostic): String {
        val message = DefaultErrorMessages.render(diagnostic)
        return if (isApplicationInternalMode() || isUnitTestMode) {
            "[${diagnostic.factory.name}] $message"
        } else {
            message
        }
    }

    /**
     * 根据高亮类型和严重性转换为对应的文本属性
     *
     * 将问题高亮类型和诊断严重性映射到 IntelliJ 预定义的文本属性键，
     * 用于控制编辑器中问题的视觉呈现（如颜色、下划线样式等）。
     *
     * @param highlightType 问题高亮类型
     * @param severity 诊断严重性级别
     * @return 对应的文本属性键，如果不需要特殊属性则返回 null
     */
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

    /**
     * 根据高亮类型和严重性转换为高亮信息类型
     *
     * 将诊断的严重性级别转换为 IntelliJ 的 HighlightInfoType，
     * 支持特殊处理弱警告类型。
     *
     * @param highlightType 问题高亮类型
     * @param severity 诊断严重性级别
     * @return 对应的高亮信息类型
     */
    private fun convertSeverity(highlightType: ProblemHighlightType?, severity: Severity): HighlightInfoType =
        when (severity) {
            Severity.ERROR -> HighlightInfoType.ERROR
            Severity.WARNING -> {
                if (highlightType == ProblemHighlightType.WEAK_WARNING) {
                    HighlightInfoType.WEAK_WARNING
                } else HighlightInfoType.WARNING
            }

            Severity.INFO -> HighlightInfoType.WEAK_WARNING
        }

    /**
     * 将问题高亮类型转换为高亮信息类型
     *
     * 根据问题高亮类型的具体分类（如未使用符号、未知符号、已废弃等），
     * 转换为对应的 IntelliJ 高亮信息类型，以便在编辑器中正确展示。
     *
     * @param highlightType 问题高亮类型
     * @param severity 诊断严重性级别（用于默认转换）
     * @return 对应的高亮信息类型
     */
    private fun toHighlightInfoType(highlightType: ProblemHighlightType?, severity: Severity): HighlightInfoType =
        when (highlightType) {
            ProblemHighlightType.LIKE_UNUSED_SYMBOL -> HighlightInfoType.UNUSED_SYMBOL
            ProblemHighlightType.LIKE_UNKNOWN_SYMBOL -> HighlightInfoType.WRONG_REF
            ProblemHighlightType.LIKE_DEPRECATED -> HighlightInfoType.DEPRECATED
            ProblemHighlightType.LIKE_MARKED_FOR_REMOVAL -> HighlightInfoType.MARKED_FOR_REMOVAL
            else -> convertSeverity(highlightType, severity)
        }

    /**
     * 创建高亮信息构建器
     *
     * 根据诊断信息创建一个 HighlightInfo.Builder 对象，该对象用于构建
     * 最终在编辑器中显示的高亮信息。包括消息内容、文本范围、工具提示等。
     *
     * @param diagnostic 诊断信息对象
     * @param range 要高亮显示的文本范围
     * @param group 可抑制的警告问题组（用于警告类诊断）
     * @return 配置好的高亮信息构建器
     */
    private fun create(
        diagnostic: Diagnostic,
        range: TextRange,
        group: CangJieSuppressableWarningProblemGroup?
    ): HighlightInfo.Builder {
        // 确定要显示的消息：优先使用自定义消息，否则使用默认消息
        val message = if (nonDefaultMessage.isNullOrEmpty()) getDefaultMessage(diagnostic) else nonDefaultMessage

        // 确定要应用的文本属性：优先使用指定的文本属性，否则根据严重性转换
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
                // 如果有文本属性需要应用，则应用它
                if (textAttributesToApply != null) {
                    it.textAttributes(textAttributesToApply)
                }
            }
            .also {
                // 如果存在问题组，则设置它（用于支持抑制警告功能）
                if (group != null) {
                    it.problemGroup(group)
                }
            }
    }

    /**
     * 获取诊断信息的完整消息（用于工具提示）
     *
     * 生成用于编辑器工具提示的 HTML 格式消息。在内部模式或测试模式下，
     * 会在消息前添加诊断工厂名称以便调试。
     *
     * @param diagnostic 诊断信息对象
     * @return HTML 格式的工具提示消息
     */
    @NlsContexts.Tooltip
    private fun getMessage(diagnostic: Diagnostic): String {
        var message = IdeErrorMessages.render(diagnostic)

        // 在内部模式或测试模式下，添加诊断工厂名称
        if (isApplicationInternalMode() || isUnitTestMode) {
            val factoryName = diagnostic.factory.name
            message = if (message.startsWith("<html>")) {
                @Suppress("HardCodedStringLiteral")
                "<html>[$factoryName] ${message.substring("<html>".length)}"
            } else {
                "[$factoryName] $message"
            }
        }

        // 如果消息不是 HTML 格式，则包装为 HTML 并转义特殊字符
        if (!message.startsWith("<html>")) {
            message = "<html><body>${XmlStringUtil.escapeString(message)}</body></html>"
        }

        return message
    }

    /**
     * 应用快速修复操作
     *
     * 将诊断信息相关的快速修复操作（IntentionAction）注册到高亮信息中，
     * 使用户可以在编辑器中通过 Alt+Enter 等快捷键触发修复操作。
     *
     * @param quickFixes 诊断信息到快速修复操作的多重映射
     * @param diagnostic 当前诊断信息
     * @param range 要应用修复的文本范围
     * @param builder 高亮信息构建器（用于新建高亮信息）
     * @param highlightInfo 已存在的高亮信息（用于更新已有高亮信息）
     * @param problemGroup 问题组（用于添加抑制选项）
     */
    private fun applyFixes(
        quickFixes: MultiMap<Diagnostic, IntentionAction>,
        diagnostic: Diagnostic,
        range: TextRange,
        builder: HighlightInfo.Builder?,
        highlightInfo: HighlightInfo?,
        problemGroup: ProblemGroup?
    ) {
        // 判断是否为警告级别的诊断
        val isWarning = diagnostic.severity == Severity.WARNING

        // 获取诊断关联的 PSI 元素
        val element = diagnostic.psiElement

        // 获取该诊断的所有修复操作，如果为警告且没有修复操作，则使用空列表
        val fixes = quickFixes[diagnostic].takeIf { it.isNotEmpty() }
            ?: if (isWarning) listOf(/*CompilerWarningIntentionAction(diagnostic.factory.name)*/) else emptyList()

        // 如果是警告，则注册高亮显示键以支持配置抑制选项
        val keyForSuppressOptions = if (isWarning) {
            HighlightDisplayKey.findOrRegister(
                CANGJIE_COMPILER_WARNING_ID,
                CangJieHighlightingBundle.message("cangjie.compiler.warning")
            )
        } else null

        // 遍历所有修复操作
        for (fix in fixes) {
            if (fix !is IntentionAction) {
                continue
            }

            // 特殊处理：跳过延迟注册的快速修复（兼容性考虑）
            if (fix == RegisterQuickFixesLaterIntentionAction) {
                if (builder != null) {
                    // TODO 兼容性调整：UnresolvedReferenceQuickFixUpdater 在新版本 IntelliJ Platform 中可能已移除
                    // 暂时跳过此逻辑
                    continue
                }
            }

            // 收集修复操作的选项
            val options = mutableListOf<IntentionAction>()

            // 如果修复操作支持选项，则添加这些选项
            if (fix is IntentionActionWithOptions) {
                options += fix.options
            }

            // 如果问题组支持抑制操作，则添加抑制选项
            if (problemGroup is SuppressableProblemGroup) {
                options += problemGroup.getSuppressActions(element).mapNotNull { it as IntentionAction }
            }

            // 根据严重性确定消息文本
            val isError = diagnostic.severity == Severity.ERROR
            val message =
                CangJieHighlightingBundle.message(if (isError) "cangjie.compiler.error" else "cangjie.compiler.warning")

            // 将修复操作注册到构建器或高亮信息中
            builder?.registerFix(fix, options, message, range, keyForSuppressOptions)
        }
    }

    /**
     * 处理诊断信息，将其转换为编辑器中的高亮信息
     *
     * 这是该类的核心方法，负责将编译器产生的诊断信息批量转换为
     * IntelliJ 编辑器可识别和展示的高亮信息。处理流程包括：
     * 1. 遍历所有需要高亮的文本范围
     * 2. 对每个范围内的诊断信息创建或更新高亮信息
     * 3. 应用相应的快速修复操作
     * 4. 将高亮信息添加到持有者中以便在编辑器中显示
     *
     * @param holder HighlightInfoHolder 对象，用于存储和管理高亮信息
     * @param diagnostics 一组诊断信息，表示代码中发现的问题
     * @param highlightInfoByDiagnostic 可变映射，将诊断信息映射到对应的高亮信息（用于避免重复创建）
     * @param fixesMap 多重映射，将诊断信息映射到可用的快速修复操作
     * @param calculatingInProgress 布尔值，指示是否正在计算诊断信息（影响修复操作的应用时机）
     */
    fun processDiagnostics(
        holder: HighlightInfoHolder,
        diagnostics: Collection<Diagnostic>,
        highlightInfoByDiagnostic: MutableMap<Diagnostic, HighlightInfo>?,
        fixesMap: MultiMap<Diagnostic, IntentionAction>,
        calculatingInProgress: Boolean
    ) {
        // 遍历所有需要高亮的文本范围
        for (range in ranges) {
            // 对每个诊断信息进行处理
            for (diagnostic in diagnostics) {
                // 如果是警告级别，创建可抑制的警告问题组（支持用户抑制特定警告）
                val group = if (diagnostic.severity == Severity.WARNING) {
                    CangJieSuppressableWarningProblemGroup(diagnostic.factory.name)
                } else {
                    null
                }

                // 检查是否已经存在与当前诊断信息关联的高亮信息
                val existingInfo = highlightInfoByDiagnostic?.get(diagnostic)
                if (existingInfo != null) {
                    // 如果高亮信息已存在，并且不在计算诊断信息的过程中，则应用快速修复
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
                    // 如果高亮信息不存在，则创建一个新的高亮信息构建器
                    val builder = create(diagnostic, range, group)

                    // 如果不在计算诊断信息的过程中，或者有快速修复可以应用，则应用修复操作
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

                    // 无条件创建高亮信息对象
                    val highlightInfo = builder.createUnconditionally()

                    // 将高亮信息添加到持有者中，以便在编辑器中显示
                    holder.add(highlightInfo)

                    // 将诊断信息和对应的高亮信息添加到映射中，避免重复创建
                    highlightInfoByDiagnostic?.put(diagnostic, highlightInfo)
                }
            }
        }
    }
}