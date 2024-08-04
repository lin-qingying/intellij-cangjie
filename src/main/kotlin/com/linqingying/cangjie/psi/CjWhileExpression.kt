package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode


class CjWhileExpression(node: ASTNode) : CjWhileExpressionBase(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitWhileExpression(this, data)
    }
}

