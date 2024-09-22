package com.huawei.cangjie.psi

import com.huawei.cangjie.lexer.CjTokens
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement



class CjDoWhileExpression(node: ASTNode) : CjWhileExpressionBase(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitDoWhileExpression(this, data)
    }

    @get: IfNotParsed
    val whileKeyword: PsiElement?
        get() = findChildByType(CjTokens.WHILE_KEYWORD)
}


