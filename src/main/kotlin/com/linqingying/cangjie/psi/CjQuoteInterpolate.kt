package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode

class CjQuoteInterpolate(node: ASTNode) : CjElementImpl(node) {

    val expression = findChildByClass<CjExpression>(CjExpression::class.java)
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitQuoteInterpolate(this, data)
    }
}
