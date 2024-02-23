package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode


class CjCallExpression(node: ASTNode) : CjExpressionImpl(node), CjCallElement, CjReferenceExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitCallExpression(this, data)
    }

    override fun getCalleeExpression(): CjExpression? {
        return findChildByClass(CjExpression::class.java)
    }


    override fun toString(): String {
        return node.elementType.toString()
    }
}
