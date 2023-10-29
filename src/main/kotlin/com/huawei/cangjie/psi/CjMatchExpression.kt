package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.lexer.CjTokens
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement



class CjMatchExpression(node: ASTNode) : CjExpressionImpl(node) {
    val entries: List<Any>
        get() = findChildrenByType<CjMatchEntry>(CjNodeTypes.MATCH_ENTRY)
    val subjectVariable
        get() = findChildByClass(CjProperty::class.java)
    val subjectExpression
        get() = findChildByClass<CjExpression>(CjExpression::class.java)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitMatchExpression(this, data)
    }

    val matchKeyword: PsiElement?
        get() = findChildByType(CjTokens.MATCH_KEYWORD)
    val closeBrace: PsiElement?
        get() = findChildByType(CjTokens.RBRACE)
    val openBrace: PsiElement?
        get() = findChildByType(CjTokens.LBRACE)
    val leftParenthesis: PsiElement?
        get() = findChildByType(CjTokens.LPAR)
    val rightParenthesis: PsiElement?
        get() = findChildByType(CjTokens.RPAR)

}

