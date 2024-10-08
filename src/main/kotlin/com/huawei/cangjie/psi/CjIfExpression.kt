package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.lexer.CjTokens
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


class CjIfExpression(node: ASTNode) : CjExpressionImpl(node) ,CjPatternEntryBlock{
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitIfExpression(this, data)
    }

    @get: IfNotParsed
    val condition: CjExpression?
        get() = findExpressionUnder(CjNodeTypes.CONDITION)


    val letExpression get() = findChildByType<CjLetExpression>(CjNodeTypes.LET_EXPRESSION)

    @get:IfNotParsed
    val leftParenthesis: PsiElement?
        get() = findChildByType(CjTokens.LPAR)

    @get:IfNotParsed
    val rightParenthesis: PsiElement?
        get() = findChildByType(CjTokens.RPAR)
    val then: CjExpression?
        get() = findExpressionUnder(CjNodeTypes.THEN)
    val `else`: CjExpression?
        get() = findExpressionUnder(CjNodeTypes.ELSE)
    val elseKeyword: PsiElement?
        get() = findChildByType(CjTokens.ELSE_KEYWORD)
    val ifKeyword: PsiElement
        get() = findChildByType(CjTokens.IF_KEYWORD)!!
}

