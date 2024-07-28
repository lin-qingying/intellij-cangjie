package com.huawei.cangjie.ide

import com.intellij.lang.documentation.DocumentationProvider
import com.intellij.lang.documentation.ExternalDocumentationProvider
import com.intellij.psi.PsiElement

class CangJieDocumentationProvider: DocumentationProvider, ExternalDocumentationProvider {
    override fun hasDocumentationFor(element: PsiElement?, originalElement: PsiElement?): Boolean {
        TODO("Not yet implemented")
    }

    override fun canPromptToConfigureDocumentation(element: PsiElement?): Boolean {
        TODO("Not yet implemented")
    }

    override fun promptToConfigureDocumentation(element: PsiElement?) {
        TODO("Not yet implemented")
    }

    override fun getQuickNavigateInfo(element: PsiElement?, originalElement: PsiElement?): String? {
        return super.getQuickNavigateInfo(element, originalElement)
    }


}
