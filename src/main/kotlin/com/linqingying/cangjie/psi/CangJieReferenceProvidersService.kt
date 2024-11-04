package com.linqingying.cangjie.psi

import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiReference

open class CangJieReferenceProvidersService {
    open fun getReferences(psiElement: PsiElement): Array<PsiReference> = PsiReference.EMPTY_ARRAY

    companion object {
        private val NO_REFERENCES_SERVICE = CangJieReferenceProvidersService()

        @JvmStatic
        fun getInstance(project: Project): CangJieReferenceProvidersService {
            return project.getService(CangJieReferenceProvidersService::class.java) ?: NO_REFERENCES_SERVICE
        }

        @JvmStatic
        fun getReferencesFromProviders(psiElement: PsiElement): Array<PsiReference> {
            return getInstance(psiElement.project).getReferences(psiElement)
        }
    }
}
