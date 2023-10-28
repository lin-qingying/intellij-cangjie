package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode


class CjParenthesizedExpression(node: ASTNode) : CjExpressionImpl(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R {
        return visitor.visitParenthesizedExpression(this, data)
    }

    @get:IfNotParsed
    val expression: CjExpression?
        get() = findChildByClass<CjExpression>(CjExpression::class.java)
}

