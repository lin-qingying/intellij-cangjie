package com.huawei.cangjie.diagnostics

import com.huawei.cangjie.psi.psiUtil.endOffset
import com.huawei.cangjie.psi.psiUtil.startOffset
import com.intellij.openapi.util.TextRange
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiElement
import com.intellij.psi.PsiErrorElement
import com.intellij.psi.PsiWhiteSpace

open class PositioningStrategy<in E : PsiElement>{
    open fun markDiagnostic(diagnostic: DiagnosticMarker): List<TextRange> {
        @Suppress("UNCHECKED_CAST")
        return mark(diagnostic.psiElement as E)
    }
    open fun mark(element: E): List<TextRange> {
        return markElement(element)
    }

    open fun isValid(element: E): Boolean {
        return !hasSyntaxErrors(element)
    }
}

fun hasSyntaxErrors(psiElement: PsiElement): Boolean {
    if (psiElement is PsiErrorElement) return true

    val children = psiElement.children
    return children.isNotEmpty() && hasSyntaxErrors(children.last())
}
fun markRange(range: TextRange): List<TextRange> {
    return listOf(range)
}
fun markRange(from: PsiElement, to: PsiElement): List<TextRange> {
    return markRange(TextRange(getStartOffset(from), getEndOffset(to)))
}
fun markElement(element: PsiElement): List<TextRange> {
    return listOf(TextRange(getStartOffset(element), getEndOffset(element)))
}

private fun getEndOffset(element: PsiElement): Int {
    var child = element.lastChild
    if (child != null) {
        while (child is PsiComment || child is PsiWhiteSpace) {
            child = child.prevSibling
        }
        if (child != null) {
            return getEndOffset(child)
        }
    }

    return element.endOffset
}
private fun getStartOffset(element: PsiElement): Int {
    var child = element.firstChild
    if (child != null) {
        while (child is PsiComment || child is PsiWhiteSpace) {
            child = child.nextSibling
        }
        if (child != null) {
            return getStartOffset(child)
        }
    }
    return element.startOffset
}

