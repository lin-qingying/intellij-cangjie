package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode


class CjThrowExpression(node: ASTNode) : CjExpressionImpl(node), CjStatementExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R {
        return visitor.visitThrowExpression(this, data)
    }

    @get:IfNotParsed
    val thrownExpression: CjExpression?
        get() = findChildByClass<CjExpression>(CjExpression::class.java)
}

