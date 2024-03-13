package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode

class CjUnsafeExpression(node: ASTNode) : CjExpressionImpl(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitUnsafeExpression(this, data)
    }
}