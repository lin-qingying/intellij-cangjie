package com.huawei.cangjie.ide.presentation

import com.intellij.ide.util.PsiElementListCellRenderer
import com.intellij.psi.NavigatablePsiElement
import com.intellij.psi.presentation.java.SymbolPresentationUtil

class DeclarationByModuleRenderer : CjModuleSpecificListCellRenderer<NavigatablePsiElement>() {
    override fun getContainerText(element: NavigatablePsiElement?, name: String?): String? {

        return null
    }

    override fun getElementText(element: NavigatablePsiElement): String =
        SymbolPresentationUtil.getSymbolPresentableText(element)
}
abstract class CjModuleSpecificListCellRenderer<T : NavigatablePsiElement> : PsiElementListCellRenderer<T>()
