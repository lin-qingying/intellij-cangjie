package com.linqingying.cangjie.utils

import com.linqingying.cangjie.psi.psiUtil.parentOfType
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.PsiFile
import com.intellij.psi.impl.source.tree.LeafPsiElement
import com.intellij.psi.util.PsiTreeUtil

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
inline fun <reified T : PsiElement> PsiElement.getNonStrictParentOfType(): T? {
    return PsiTreeUtil.getParentOfType(this, T::class.java, false)
}
val PsiElement.parents: Sequence<PsiElement>
    get() = parentsWithSelf.drop(1)
val PsiElement.parentsWithSelf: Sequence<PsiElement>
    get() = generateSequence(this) { if (it is PsiFile) null else it.parent }

