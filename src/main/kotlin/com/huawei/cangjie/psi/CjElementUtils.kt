package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil

internal fun CjElement.deleteSemicolon() {
    if (this is CjElement) return

    val sibling = PsiTreeUtil.skipSiblingsForward(this, PsiWhiteSpace::class.java, PsiComment::class.java)
    if (sibling == null || sibling.node.elementType != CjTokens.SEMICOLON) return

    val lastSiblingToDelete = PsiTreeUtil.skipSiblingsForward(sibling, PsiWhiteSpace::class.java)?.prevSibling ?: sibling
    parent?.deleteChildRange(nextSibling, lastSiblingToDelete)
}
