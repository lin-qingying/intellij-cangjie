package com.huawei.cangjie.ide.codeinsight.quickDoc

import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.platform.backend.documentation.DocumentationTarget
import com.intellij.platform.backend.documentation.DocumentationTargetProvider
import com.intellij.platform.backend.documentation.PsiDocumentationTargetProvider
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiFile



class CangJiePsiDocumentationTargetProvider : PsiDocumentationTargetProvider {
    override fun documentationTarget(element: PsiElement, originalElement: PsiElement?): DocumentationTarget? {

        return null
        val elementWithDocumentation = element.navigationElement ?: element
        return if (elementWithDocumentation.language.`is`(CangJieLanguage)) CangJieDocumentationTarget(elementWithDocumentation, originalElement) else null
    }
}
class CangJieDocumentationTargetProvider : DocumentationTargetProvider {
    override fun documentationTargets(file: PsiFile, offset: Int): List<DocumentationTarget> {
        return emptyList()
        val element = file.findElementAt(offset) ?: return emptyList()
        return if (element.isModifier()) {
            arrayListOf(CangJieDocumentationTarget(element, element))
        } else emptyList()
    }
}
