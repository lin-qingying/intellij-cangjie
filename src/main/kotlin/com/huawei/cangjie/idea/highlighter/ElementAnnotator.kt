package com.huawei.cangjie.idea.highlighter

import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.psi.CjParameter
import com.intellij.codeInsight.daemon.impl.HighlightInfo
import com.intellij.codeInsight.daemon.impl.analysis.HighlightInfoHolder
import com.intellij.psi.PsiElement

internal class ElementAnnotator(
    private val element: PsiElement,
    private val shouldSuppressUnusedParameter: (CjParameter) -> Boolean
){

    fun registerDiagnosticsAnnotations(
        holder: HighlightInfoHolder,
        diagnostics: Collection<Diagnostic>,
        highlightInfoByDiagnostic: MutableMap<Diagnostic, HighlightInfo>?,
        calculatingInProgress: Boolean
    ) = diagnostics.groupBy { it.factory }
//        .forEach {
//            val sameTypeDiagnostics = it.value
//            val presentationInfo = presentationInfo(sameTypeDiagnostics)
//            if (presentationInfo != null) {
//                val fixesMap =
//                    if (calculatingInProgress) {
//                        Fe10QuickFixProvider.getInstance(element.project).createPostponedUnresolvedReferencesQuickFixes(sameTypeDiagnostics)
//                    } else {
//                        createFixesMap(sameTypeDiagnostics)
//                    }
//                presentationInfo.processDiagnostics(holder, sameTypeDiagnostics, highlightInfoByDiagnostic, fixesMap, calculatingInProgress)
//            }
//
//            TODO()
//        }
}