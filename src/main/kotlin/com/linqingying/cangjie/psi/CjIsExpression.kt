package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode

class CjIsExpression(node: ASTNode) : CjExpressionImpl(node), CjOperationExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitIsExpression(this, data)
    }

    val leftHandSide: CjExpression
        get() = findChildByClass(CjExpression::class.java)!!

    @get:IfNotParsed
    val typeReference: CjTypeReference?
        get() = findChildByType(CjNodeTypes.TYPE_REFERENCE)

    override val operationReference : CjSimpleNameExpression get()   {
        return findChildByType(CjNodeTypes.OPERATION_REFERENCE)!!
    }
}
