package com.huawei.cangjie.ide.inspections.suppress

import com.intellij.codeInspection.SuppressIntentionAction
import com.intellij.codeInspection.SuppressableProblemGroup
import com.intellij.psi.PsiElement


class CangJieSuppressableWarningProblemGroup(private val factoryName: String) : SuppressableProblemGroup {
    override fun getProblemName(): String? {
        TODO("Not yet implemented")
    }

    override fun getSuppressActions(element: PsiElement?): Array<SuppressIntentionAction> {
        TODO("Not yet implemented")
    }
}
