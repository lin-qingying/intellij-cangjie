package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode

class CjQuoteExpression(node: ASTNode) : CjExpressionImpl(node) {


    val quoteInterpolates: List<CjQuoteInterpolate> get() = findChildrenByClass(CjQuoteInterpolate::class.java).toList()
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitQuoteExpression(this, data)
    }
}
