package com.linqingying.cangjie.ide.navigationToolbar

import com.linqingying.cangjie.ide.AbstractCangJieIconProvider
import com.linqingying.cangjie.ide.CangJieIconProvider
import com.linqingying.cangjie.ide.projectView.CjDeclarationTreeNode.Companion.tryGetRepresentableText
import com.linqingying.cangjie.psi.CjDeclaration
import com.linqingying.cangjie.psi.CjFile
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
