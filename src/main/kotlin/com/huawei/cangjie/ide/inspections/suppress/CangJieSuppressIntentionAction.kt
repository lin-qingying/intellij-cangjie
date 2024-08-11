package com.huawei.cangjie.ide.inspections.suppress

import com.huawei.cangjie.ide.codeinsight.CangJieCodeInsightBundle
import com.huawei.cangjie.psi.CjDestructuringDeclarationEntry
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjParameter
import com.intellij.codeInsight.intention.FileModifier
import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.openapi.editor.Editor
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement


class CangJieSuppressIntentionAction(
    suppressAt: CjElement,
    private val suppressionKey: String,
    @FileModifier.SafeFieldForPreview private val kind: AnnotationHostKind
) : SuppressIntentionAction() {
    override fun getFamilyName(): String  = CangJieCodeInsightBundle.message("intention.suppress.family")
    private fun isLambdaParameter(element: PsiElement): Boolean {
        if (kind.kind != CangJieCodeInsightBundle.message("declaration.kind.parameter")) return false
        val parentParameter = element.parent as? CjParameter
            ?: (element.parent as? CjDestructuringDeclarationEntry)?.parent?.parent as? CjParameter
        return parentParameter?.isLambdaParameter == true
    }
    override fun isAvailable(project: Project, editor: Editor?, element: PsiElement): Boolean {
        if (isLambdaParameter(element)) {
            // Lambda parameters can't be annotated: KT-13900
            return false
        }

        return element.isValid
    }

    override fun invoke(project: Project, editor: Editor?, element: PsiElement) {
//        TODO("Not yet implemented")
    }
}
