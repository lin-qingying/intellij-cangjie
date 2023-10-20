package com.huawei.cangjie.utils

import com.huawei.cangjie.psi.psiUtil.parentOfType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.impl.source.tree.LeafPsiElement

fun getElementTextWithContext(psiElement: PsiElement): String {
    if (!psiElement.isValid) return "<invalid element $psiElement>"

    @Suppress("LocalVariableName") val ELEMENT_TAG = "ELEMENT"
    val containingFile = psiElement.containingFile
    val context = psiElement.parentOfType("CjImportDirective")
        ?: psiElement.parentOfType("CjPackageDirective")
        ?: psiElement.parentOfType("CjDeclarationWithBody"  )
        ?: psiElement.parentOfType("CjProperty")
        ?: containingFile
    val elementTextInContext = buildString {
        context.accept(object : PsiElementVisitor() {
            override fun visitElement(element: PsiElement) {
                if (element === psiElement) append("<$ELEMENT_TAG>")
                if (element is LeafPsiElement) {
                    append(element.text)
                } else {
                    element.acceptChildren(this)
                }
                if (element === psiElement) append("</$ELEMENT_TAG>")
            }
        })
    }.trimIndent().trim()

    return buildString {
        appendLine("<File name: ${containingFile.name}, Physical: ${containingFile.isPhysical}>")
        append(elementTextInContext)
    }
}
