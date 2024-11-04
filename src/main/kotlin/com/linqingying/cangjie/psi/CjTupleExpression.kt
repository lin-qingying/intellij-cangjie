package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode

class CjTupleExpression(node: ASTNode) : CjExpressionImpl(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTupleExpression(this, data)
    }


    val expressions: List<CjExpression>
        get() {
          return  findChildrenByClass(CjExpression::class.java).toList()

        }

}
