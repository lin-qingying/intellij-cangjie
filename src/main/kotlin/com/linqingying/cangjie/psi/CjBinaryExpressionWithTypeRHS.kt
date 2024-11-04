package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode
import java.util.*

class CjBinaryExpressionWithTypeRHS(node: ASTNode) : CjExpressionImpl(node), CjOperationExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitBinaryWithTypeRHSExpression(this, data)
    }

    val left: CjExpression
        get() {
            val left = checkNotNull(
                findChildByClass(
                    CjExpression::class.java
                )
            )
            return left
        }

    @get:IfNotParsed
    val right: CjTypeReference?
        get() {
            var node = operationReference.node
            while (node != null) {
                val psi = node.psi
                if (psi is CjTypeReference) {
                    return psi
                }
                node = node.treeNext
            }

            return null
        }

    override val operationReference : CjSimpleNameExpression get() =
           findChildByType(CjNodeTypes.OPERATION_REFERENCE)!!

}
