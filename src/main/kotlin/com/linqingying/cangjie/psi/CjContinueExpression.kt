package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode


class CjContinueExpression(node: ASTNode) : CjExpressionWithLabel(node), CjStatementExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitContinueExpression(this, data)
    }
}

