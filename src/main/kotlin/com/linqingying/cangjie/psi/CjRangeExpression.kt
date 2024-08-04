package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode


class CjRangeExpression(node: ASTNode): CjExpressionImpl(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitRangeExpression(this, data)
    }


}
