package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.lexer.CjTokens
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement




class CjReturnExpression(node: ASTNode) : CjExpressionWithLabel(node), CjStatementExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitReturnExpression(this, data)
    }

    val returnedExpression: CjExpression?
        get() = findChildByClass(CjExpression::class.java)
    val returnKeyword: PsiElement?
        get() = findChildByType(CjTokens.RETURN_KEYWORD)
    val labeledExpression: PsiElement?
        get() = findChildByType(CjNodeTypes.LABEL_QUALIFIER)
}

