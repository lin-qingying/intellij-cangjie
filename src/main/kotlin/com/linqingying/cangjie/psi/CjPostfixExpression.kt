package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil


class  CjPostfixExpression(node:ASTNode):CjUnaryExpression(node){


    override val baseExpression: CjExpression?
        get() = PsiTreeUtil.getPrevSiblingOfType(operationReference, CjExpression::class.java)
    override fun <R,D> accept(visitor: CjVisitor<R, D>, data: D?): R  {
        return visitor.visitPostfixExpression(this, data)
    }
}
