package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.tree.IElementType

open class CjBinaryExpression(node: ASTNode) : CjExpressionImpl(node), CjOperationExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitBinaryExpression(this, data)
    }

    @get:IfNotParsed
    val left: CjExpression?
        get() {
            var node = operationReference.node.treePrev
            while (node != null) {
                val psi = node.psi
                if (psi is CjExpression) {
                    return psi
                }
                node = node.treePrev
            }

            return null
        }

    @get:IfNotParsed
    val right: CjExpression?
        get() {
            var node = operationReference.node.treeNext
            while (node != null) {
                val psi = node.psi
                if (psi is CjExpression) {
                    return psi
                }
                node = node.treeNext
            }

            return null
        }

    override val operationReference : CjOperationReferenceExpression get()  {
        val operationReference = findChildByType<PsiElement>(CjNodeTypes.OPERATION_REFERENCE)
            ?: throw NullPointerException("No operation reference for binary expression: " + children.contentToString())

        return operationReference as CjOperationReferenceExpression
    }

    val operationToken: IElementType
        get() = operationReference.getReferencedNameElementType()
}
