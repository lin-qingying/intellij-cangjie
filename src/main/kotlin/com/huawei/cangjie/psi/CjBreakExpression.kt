package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode



class CjBreakExpression(node: ASTNode) : CjExpressionWithLabel(node), CjStatementExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R {
        return visitor.visitBreakExpression(this, data)
    }
}

