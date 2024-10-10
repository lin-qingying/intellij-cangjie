package com.huawei.cangjie.ide.navigationToolbar

import com.huawei.cangjie.ide.AbstractCangJieIconProvider
import com.huawei.cangjie.ide.CangJieIconProvider
import com.huawei.cangjie.ide.projectView.CjDeclarationTreeNode.Companion.tryGetRepresentableText
import com.huawei.cangjie.psi.CjDeclaration
import com.huawei.cangjie.psi.CjFile
import com.intellij.psi.PsiElement

class CangJieNavBarModelExtension: AbstractNavBarModelExtensionCompatBase() {
    override fun getPresentableText(item: Any?): String? =
        (item as? CjDeclaration)?.let { tryGetRepresentableText(it, renderArguments = false) }

    override fun adjustElementImpl(psiElement: PsiElement?): PsiElement? {
        if (psiElement is CjDeclaration) {
            return psiElement
        }

        val containingFile = psiElement?.containingFile as? CjFile ?: return psiElement
        return AbstractCangJieIconProvider.getSingleClass(containingFile) ?: psiElement
    }
}
