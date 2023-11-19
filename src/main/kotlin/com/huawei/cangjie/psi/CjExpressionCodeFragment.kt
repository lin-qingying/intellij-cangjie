package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement


class CjExpressionCodeFragment(
    project: Project,
    name: String,
    text: CharSequence,
    imports: String?,
    context: PsiElement?
) : CjCodeFragment(project, name, text, imports, CjNodeTypes.EXPRESSION_CODE_FRAGMENT, context) {

    override fun getContentElement() = findChildByClass(CjExpression::class.java)
}
