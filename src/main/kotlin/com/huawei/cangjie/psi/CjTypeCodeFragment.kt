package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement


class CjTypeCodeFragment(
    project: Project,
    name: String,
    text: CharSequence,
    context: PsiElement?
) : CjCodeFragment(project, name, text, null, CjNodeTypes.TYPE_CODE_FRAGMENT, context) {
    override fun getContentElement() = findChildByClass(CjTypeReference::class.java)
}
