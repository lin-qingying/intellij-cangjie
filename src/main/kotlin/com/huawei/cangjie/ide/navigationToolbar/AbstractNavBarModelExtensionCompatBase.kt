package com.huawei.cangjie.ide.navigationToolbar

import com.huawei.cangjie.ide.CangJieIconProvider
import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.psi.CjFile
import com.intellij.ide.navigationToolbar.StructureAwareNavBarModelExtension
import com.intellij.lang.Language
import com.intellij.psi.PsiElement


abstract class AbstractNavBarModelExtensionCompatBase : StructureAwareNavBarModelExtension(){

    protected abstract fun adjustElementImpl(psiElement: PsiElement?): PsiElement?
    override fun adjustElement(psiElement: PsiElement): PsiElement? =
        adjustElementImpl(psiElement)
    override val language: Language
        get() = CangJieLanguage
    override fun acceptParentFromModel(psiElement: PsiElement?): Boolean {
        if (psiElement is CjFile) {
            return CangJieIconProvider.getSingleClass(psiElement) == null
        }
        return true
    }
}
