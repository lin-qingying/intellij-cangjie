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

package cn.cangnova.cangjie.highlighter

import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.codeInspection.ProblemsHolder
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap
import cn.cangnova.cangjie.diagnostics.Diagnostic
import cn.cangnova.cangjie.diagnostics.Errors
import cn.cangnova.cangjie.diagnostics.Severity
import cn.cangnova.cangjie.diagnostics.rendering.DefaultErrorMessages
import cn.cangnova.cangjie.psi.CjParameter
import cn.cangnova.cangjie.psi.CjReferenceExpression
import cn.cangnova.cangjie.references.mainReference

/**
 * 用于为 PSI 元素注册诊断注解的类。
 *
 * @param element 目标 PSI 元素。
 * @param shouldSuppressUnusedParameter 用于判断是否抑制未使用参数警告的函数，默认为 `false`。
 */
internal class ElementAnnotator(
    private val element: PsiElement,
    private val shouldSuppressUnusedParameter: (CjParameter) -> Boolean = { false }
) {

    companion object {
        /**
         * 日志工具，用于记录调试信息。
         */
        val LOG = Logger.getInstance(ElementAnnotator::class.java)

        /**
         * 注册表键，用于决定是否抑制已废弃注解的高亮显示。
         */
        val suppressDeprecatedAnnotationRegistryKey = Registry.get("cangjie.highlighting.suppress.deprecated")

        /**
         * 当前是否抑制已废弃注解的标志。
         */
        var suppressDeprecatedAnnotation = suppressDeprecatedAnnotationRegistryKey.asBoolean()

        /**
         * 更新 `suppressDeprecatedAnnotation` 的值。
         * 从注册表中读取最新配置。
         */
        fun updateSuppressDeprecatedAnnotationValue() {
            suppressDeprecatedAnnotation = suppressDeprecatedAnnotationRegistryKey.asBoolean()
        }
    }

    /**
     * 生成与诊断相关的注解展示信息。
     *
     * @param diagnostics 诊断集合。
     * @return 如果有有效的诊断，返回注解展示信息，否则返回 `null`。
     */
    private fun presentationInfo(diagnostics: Collection<Diagnostic>): AnnotationPresentationInfo? {
        if (diagnostics.isEmpty() || !diagnostics.any { it.isValid }) return null

        val diagnostic = diagnostics.first()


        val factory = diagnostic.factory


        assert(diagnostics.all { it.psiElement == element && it.factory == factory })

        val ranges = diagnostic.textRanges
        val presentationInfo: AnnotationPresentationInfo = when (factory.severity) {
            Severity.ERROR -> {
                when (factory) {
                    in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS -> {
                        val referenceExpression = element as CjReferenceExpression
                        val reference = referenceExpression.mainReference
                        if (reference is MultiRangeReference) {
                            AnnotationPresentationInfo(
                                ranges = reference.ranges.map { it.shiftRight(referenceExpression.textOffset) },
                                highlightType = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                            )
                        } else {
                            AnnotationPresentationInfo(ranges, highlightType = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
                        }
                    }

                    Errors.ILLEGAL_ESCAPE -> AnnotationPresentationInfo(
                        ranges, textAttributes = CangJieHighlightingColors.INVALID_STRING_ESCAPE
                    )

                    Errors.REDECLARATION -> AnnotationPresentationInfo(
                        ranges = listOf(diagnostic.textRanges.first())
                    )

                    else -> {
                        AnnotationPresentationInfo(
                            ranges,
                            highlightType =
                                when (factory) {
                                    Errors.INVISIBLE_REFERENCE, Errors.DELEGATE_SPECIAL_FUNCTION_MISSING, Errors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, Errors.TOO_MANY_ARGUMENTS -> ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
                                    else -> null
                                },
                            textAttributes =
                                when (factory) {
                                    Errors.DELEGATE_SPECIAL_FUNCTION_MISSING, Errors.DELEGATE_SPECIAL_FUNCTION_NONE_APPLICABLE, Errors.TOO_MANY_ARGUMENTS -> CodeInsightColors.ERRORS_ATTRIBUTES
                                    else -> null
                                },
                        )
                    }
                }
            }
//
            Severity.WARNING -> {
                if (factory == Errors.UNUSED_PARAMETER && shouldSuppressUnusedParameter(element as CjParameter)) {
                    return null
                }

                AnnotationPresentationInfo(
                    ranges,
                    textAttributes = when (factory) {
                        Errors.DEPRECATION -> CodeInsightColors.DEPRECATED_ATTRIBUTES
                        Errors.UNUSED_ANONYMOUS_PARAMETER -> CodeInsightColors.WEAK_WARNING_ATTRIBUTES
                        else -> null
                    },
                    highlightType = when (factory) {
                        in Errors.UNUSED_ELEMENT_DIAGNOSTICS, Errors.UNUSED_DESTRUCTURED_PARAMETER_ENTRY ->
                            ProblemHighlightType.LIKE_UNUSED_SYMBOL

                        Errors.UNUSED_ANONYMOUS_PARAMETER -> ProblemHighlightType.WEAK_WARNING
                        else -> null
                    }
                )
            }

            Severity.INFO -> AnnotationPresentationInfo(ranges, highlightType = ProblemHighlightType.INFORMATION)


            else -> AnnotationPresentationInfo(ranges, null, null)
        }
        return presentationInfo
    }

    /**
     * 创建诊断对应的修复操作映射。
     *
     * @param sameTypeDiagnostics 同类型的诊断集合。
     * @return 一个包含诊断和对应修复操作的多映射集合。
     */
    private fun createFixesMap(sameTypeDiagnostics: Collection<Diagnostic>): MultiMap<Diagnostic, IntentionAction> =
        try {
            CangJieQuickFixProvider.getInstance(element.project).createQuickFixes(sameTypeDiagnostics)
        } catch (e: Exception) {
            if (e is ControlFlowException) {
                throw e
            }
            LOG.error(e)
            MultiMap()
        }

    /**
     * 注册诊断注解到高亮信息持有者。
     *
     * @param holder 高亮信息持有者。
     * @param diagnostics 诊断集合。
     * @param highlightInfoByDiagnostic 可选的诊断到高亮信息的映射。
     * @param calculatingInProgress 是否处于计算中的标志。
     */
    fun registerDiagnosticsAnnotations(
        holder: HighlightInfoHolder,
        diagnostics: Collection<Diagnostic>,
        highlightInfoByDiagnostic: MutableMap<Diagnostic, HighlightInfo>? = null,
        calculatingInProgress: Boolean = true
    ) = diagnostics.groupBy { it.factory }
        .forEach {
            val sameTypeDiagnostics = it.value
            val presentationInfo = presentationInfo(sameTypeDiagnostics)
            if (presentationInfo != null) {
                val fixesMap = if (calculatingInProgress) {
                    CangJieQuickFixProvider.getInstance(element.project)
                        .createPostponedUnresolvedReferencesQuickFixes(sameTypeDiagnostics)
                } else {
                    createFixesMap(sameTypeDiagnostics)
                }
                presentationInfo.processDiagnostics(
                    holder,
                    sameTypeDiagnostics,
                    highlightInfoByDiagnostic,
                    fixesMap,
                    calculatingInProgress
                )
            }
        }
}


fun HighlightInfoHolder.report(diagnostic: Diagnostic) {

    ElementAnnotator(diagnostic.psiElement).registerDiagnosticsAnnotations(
        this, listOf(diagnostic)
    )

}
