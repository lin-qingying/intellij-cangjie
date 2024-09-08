package com.huawei.cangjie.references

import com.huawei.cangjie.name.FqName
import com.intellij.psi.PsiElement

/**
 * Service that is responsible for mutating PSI through [CjReference].
 *
 * This service is only needed by IDE to handle element renaming or change elements bound to a [CjReference].
 */
interface CjReferenceMutateService {
    /**
     * See [com.intellij.psi.PsiReference.handleElementRename].
     */
    fun handleElementRename(cjReference: CjReference, newElementName: String): PsiElement?

    /**
     * See [com.intellij.psi.PsiReference.bindToElement].
     */
    fun bindToElement(cjReference: CjReference, element: PsiElement): PsiElement

    fun bindToElement(simpleNameReference: CjSimpleNameReference, element: PsiElement, shorteningMode: CjSimpleNameReference.ShorteningMode): PsiElement

    fun bindToFqName(
        simpleNameReference: CjSimpleNameReference,
        fqName: FqName,
        shorteningMode: CjSimpleNameReference.ShorteningMode = CjSimpleNameReference.ShorteningMode.DELAYED_SHORTENING,
        targetElement: PsiElement? = null
    ): PsiElement
}
