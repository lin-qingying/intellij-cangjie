package com.huawei.cangjie.ide.highlighter

import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.descriptors.Errors
import com.huawei.cangjie.diagnostics.Severity
import com.huawei.cangjie.highlighter.CangJieHighlightingColors
import com.huawei.cangjie.psi.CjParameter
import com.huawei.cangjie.psi.CjReferenceExpression
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.codeInsight.intention.IntentionAction
import com.intellij.codeInspection.ProblemHighlightType
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.editor.colors.CodeInsightColors
import com.intellij.openapi.util.registry.Registry
import com.intellij.psi.MultiRangeReference
import com.intellij.psi.PsiElement
import com.intellij.util.containers.MultiMap

internal class ElementAnnotator(
    private val element: PsiElement,
    private val shouldSuppressUnusedParameter: (CjParameter) -> Boolean
){



    companion object {
        val LOG = Logger.getInstance(ElementAnnotator::class.java)

        val suppressDeprecatedAnnotationRegistryKey = Registry.get("cangjie.highlighting.suppress.deprecated")
        var suppressDeprecatedAnnotation = suppressDeprecatedAnnotationRegistryKey.asBoolean()

        fun updateSuppressDeprecatedAnnotationValue() {
            suppressDeprecatedAnnotation = suppressDeprecatedAnnotationRegistryKey.asBoolean()
        }
    }
    private fun presentationInfo(diagnostics: Collection<Diagnostic>): AnnotationPresentationInfo? {
        if (diagnostics.isEmpty() || !diagnostics.any { it.isValid }) return null

        val diagnostic = diagnostics.first()
        // hack till the root cause #KT-21246 is fixed
//        if (isUnstableAbiClassDiagnosticForModulesWithEnabledUnstableAbi(diagnostic)) return null

        val factory = diagnostic.factory

//        if (suppressDeprecatedAnnotation && factory == Errors.DEPRECATION) return null

        assert(diagnostics.all { it.psiElement == element && it.factory == factory })

        val ranges = diagnostic.textRanges
        val presentationInfo: AnnotationPresentationInfo = when (factory.severity) {
            Severity.ERROR -> {
                when (factory) {
//                    in Errors.UNRESOLVED_REFERENCE_DIAGNOSTICS -> {
//                        val referenceExpression = element as CjReferenceExpression
//                        val reference = referenceExpression.mainReference
//                        if (reference is MultiRangeReference) {
//                            AnnotationPresentationInfo(
//                                ranges = reference.ranges.map { it.shiftRight(referenceExpression.textOffset) },
//                                highlightType = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL
//                            )
//                        } else {
//                            AnnotationPresentationInfo(ranges, highlightType = ProblemHighlightType.LIKE_UNKNOWN_SYMBOL)
//                        }
//                    }
//
//                    Errors.ILLEGAL_ESCAPE -> AnnotationPresentationInfo(
//                        ranges, textAttributes = CangJieHighlightingColors.INVALID_STRING_ESCAPE
//                    )

                    Errors.REDECLARATION -> AnnotationPresentationInfo(
                        ranges = listOf(diagnostic.textRanges.first()), nonDefaultMessage = ""
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
//            Severity.WARNING -> {
//                if (factory == Errors.UNUSED_PARAMETER && shouldSuppressUnusedParameter(element as CjParameter)) {
//                    return null
//                }
//
//                AnnotationPresentationInfo(
//                    ranges,
//                    textAttributes = when (factory) {
//                        Errors.DEPRECATION -> CodeInsightColors.DEPRECATED_ATTRIBUTES
//                        Errors.UNUSED_ANONYMOUS_PARAMETER -> CodeInsightColors.WEAK_WARNING_ATTRIBUTES
//                        else -> null
//                    },
//                    highlightType = when (factory) {
//                        in Errors.UNUSED_ELEMENT_DIAGNOSTICS, Errors.UNUSED_DESTRUCTURED_PARAMETER_ENTRY ->
//                            ProblemHighlightType.LIKE_UNUSED_SYMBOL
//
//                        Errors.UNUSED_ANONYMOUS_PARAMETER -> ProblemHighlightType.WEAK_WARNING
//                        else -> null
//                    }
//                )
//            }

            Severity.INFO -> AnnotationPresentationInfo(ranges, highlightType = ProblemHighlightType.INFORMATION)


            else -> AnnotationPresentationInfo(ranges,null,null)
        }
        return presentationInfo
    }

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

    fun registerDiagnosticsAnnotations(
        holder: HighlightInfoHolder,
        diagnostics: Collection<Diagnostic>,
        highlightInfoByDiagnostic: MutableMap<Diagnostic, HighlightInfo>?,
        calculatingInProgress: Boolean
    ) = diagnostics.groupBy { it.factory }
        .forEach {
            val sameTypeDiagnostics = it.value
            val presentationInfo = presentationInfo(sameTypeDiagnostics)
            if (presentationInfo != null) {
                val fixesMap =
//                    if (calculatingInProgress) {
//                        Fe10QuickFixProvider.getInstance(element.project).createPostponedUnresolvedReferencesQuickFixes(sameTypeDiagnostics)
//                    } else {
                        createFixesMap(sameTypeDiagnostics)
//                    }
                presentationInfo.processDiagnostics(holder, sameTypeDiagnostics, highlightInfoByDiagnostic, fixesMap, calculatingInProgress)
            }
        }
}
