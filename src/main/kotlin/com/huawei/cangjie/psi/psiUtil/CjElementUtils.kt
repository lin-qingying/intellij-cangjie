package com.huawei.cangjie.psi.psiUtil

import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.psi.CjLambdaExpression
import com.huawei.cangjie.psi.CjParenthesizedExpression
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