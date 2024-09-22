package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.util.PsiTreeUtil

class CjPrefixExpression(node: ASTNode) : CjUnaryExpression(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitPrefixExpression(this, data)
    }


    @get:IfNotParsed
    override val baseExpression: CjExpression?
        get() = PsiTreeUtil.getNextSiblingOfType(
            operationReference,
            CjExpression::class.java
        )
}
