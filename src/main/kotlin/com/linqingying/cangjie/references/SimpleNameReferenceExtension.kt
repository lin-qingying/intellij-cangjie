package com.linqingying.cangjie.references

import com.linqingying.cangjie.psi.CjPsiFactory
import com.intellij.openapi.extensions.ExtensionPointName
import com.intellij.psi.PsiElement

interface SimpleNameReferenceExtension {
    companion object {
        val EP_NAME: ExtensionPointName<SimpleNameReferenceExtension> =
            ExtensionPointName.create("com.linqingying.cangjie.simpleNameReferenceExtension")
    }

    fun isReferenceTo(reference: CjSimpleNameReference, element: PsiElement): Boolean

    fun handleElementRename(reference: CjSimpleNameReference, psiFactory: CjPsiFactory, newElementName: String): PsiElement?
}
