package com.huawei.cangjie.ide.completion.handlers

import com.huawei.cangjie.psi.CjTypeStatement
import com.intellij.psi.PsiElement
import javax.swing.Icon


class PsiClassLookupObject(val psiClass: CjTypeStatement) : DeclarationLookupObjectImpl(null) {
    override val psiElement: PsiElement
        get() = psiClass

    override fun getIcon(flags: Int): Icon? = psiClass.getIcon(flags)
}
