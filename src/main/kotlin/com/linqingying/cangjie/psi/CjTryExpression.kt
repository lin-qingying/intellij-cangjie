package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.linqingying.cangjie.lexer.CjTokens
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement



class CjTryExpression(node: ASTNode) : CjExpressionImpl(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTryExpression(this, data)
    }

    val tryBlock: CjBlockExpression
        get() = findChildByType< PsiElement>(CjNodeTypes.BLOCK) as CjBlockExpression
    val catchClauses: List<Any>
        get() = findChildrenByType<CjCatchClause>(CjNodeTypes.CATCH)
    val finallyBlock: CjFinallySection?
        get() = findChildByType< PsiElement>(CjNodeTypes.FINALLY) as CjFinallySection?
    val tryKeyword: PsiElement?
        get() = findChildByType(CjTokens.TRY_KEYWORD)
}

