package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode

class CjQuoteExpression(node: ASTNode) : CjExpressionImpl(node) {



    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitQuoteExpression(this, data)
    }
}
