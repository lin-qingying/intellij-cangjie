package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode

class CjSafeQualifiedExpression(node: ASTNode) : CjExpressionImpl(node), CjQualifiedExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitSafeQualifiedExpression(this, data)
    }
}
