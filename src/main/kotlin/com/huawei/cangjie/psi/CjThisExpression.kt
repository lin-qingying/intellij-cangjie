package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode



class CjThisExpression(node: ASTNode) : CjInstanceExpressionWithLabel(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R {
        return visitor.visitThisExpression(this, data)
    }
}

