package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.lexer.CjTokens
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement



abstract class CjLoopExpression(node: ASTNode) : CjExpressionImpl(node), CjStatementExpression {
    val body: CjExpression?
        get() = findExpressionUnder(CjNodeTypes.BODY)

    @get: IfNotParsed
    val leftParenthesis: PsiElement?
        get() = findChildByType(CjTokens.LPAR)

    @get: IfNotParsed
    val rightParenthesis: PsiElement?
        get() = findChildByType(CjTokens.RPAR)
}

