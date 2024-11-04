package com.linqingying.cangjie.psi

import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode

class CjConstructorDelegationCall(node: ASTNode) : CjElementImpl(node), CjCallElement {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitConstructorDelegationCall(this, data)
    }


    override val calleeExpression: CjConstructorDelegationReferenceExpression?
        get() =
            findChildByClass(CjConstructorDelegationReferenceExpression::class.java)


    override val lambdaArguments: List<CjLambdaArgument> = emptyList()

    override val typeArguments: List<CjTypeProjection> = emptyList()

    override val typeArgumentList: CjTypeArgumentList? = null

    override val valueArgumentList: CjValueArgumentList? get() = findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST)

    override val valueArguments: List<ValueArgument>
        get() {
            val list = valueArgumentList
            return list?.arguments ?: emptyList<CjValueArgument>()
        }


    val isImplicit: Boolean
        get() {
            val callee = calleeExpression
            return callee != null && callee.firstChild == null
        }

    val isCallToThis: Boolean
        get() {
            val callee = calleeExpression
            return callee != null && callee.isThis
        }
}
