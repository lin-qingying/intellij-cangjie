package com.linqingying.cangjie.psi.psiUtil

import com.linqingying.cangjie.lexer.CjTokens
import com.linqingying.cangjie.psi.CjElement
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.psi.CjLambdaExpression
import com.linqingying.cangjie.psi.CjParenthesizedExpression
import com.intellij.psi.PsiComment
import com.intellij.psi.PsiWhiteSpace
import com.intellij.psi.util.PsiTreeUtil

internal fun CjElement.deleteSemicolon() {

    val sibling = PsiTreeUtil.skipSiblingsForward(this, PsiWhiteSpace::class.java, PsiComment::class.java)
    if (sibling == null || sibling.node.elementType != CjTokens.SEMICOLON) return

    val lastSiblingToDelete = PsiTreeUtil.skipSiblingsForward(sibling, PsiWhiteSpace::class.java)?.prevSibling ?: sibling
    parent?.deleteChildRange(nextSibling, lastSiblingToDelete)
}
fun CjExpression.unpackFunctionLiteral(allowParentheses: Boolean = false): CjLambdaExpression? {
    return when (this) {
        is CjLambdaExpression -> this
//        is CjLabeledExpression -> baseExpression?.unpackFunctionLiteral(allowParentheses)
//        is CjAnnotatedExpression -> baseExpression?.unpackFunctionLiteral(allowParentheses)
        is CjParenthesizedExpression -> if (allowParentheses) expression?.unpackFunctionLiteral(allowParentheses) else null
        else -> null
    }
}
