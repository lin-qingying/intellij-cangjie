package com.huawei.cangjie.psi

import com.google.common.collect.Lists
import com.huawei.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode


open class CjCallExpression(node: ASTNode) : CjExpressionImpl(node), CjCallElement, CjReferenceExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitCallExpression(this, data)
    }

    override val lambdaArguments: List<CjLambdaArgument>
        get() {
            return findChildrenByType<CjLambdaArgument>(CjNodeTypes.LAMBDA_ARGUMENT)
        }
    val referenceExpression: CjNameReferenceExpression? get() = calleeExpression as? CjNameReferenceExpression

    override val typeArguments: List<CjTypeProjection>
        get() {
            return referenceExpression?.typeArguments ?: emptyList()

        }


    override val typeArgumentList: CjTypeArgumentList?
        get() {
            return referenceExpression?.typeArgumentList

        }

    fun getBasicTypeExpr(): CjExpression? {

        val typeElement = findChildByType<CjTypeReference>(CjNodeTypes.TYPE_REFERENCE) ?: return null

        return typeElement.node.findChildByType(CjNodeTypes.BASIC_TYPE)?.psi as? CjExpression

    }

    override val calleeExpression: CjExpression?
        get() {
            return findChildByClass(CjExpression::class.java) ?: getBasicTypeExpr()
        }


    override val valueArgumentList: CjValueArgumentList?
        get() {

            return findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST) as CjValueArgumentList?

        }

    override val valueArguments: List<CjValueArgument>
        get() {

            val valueArgumentsInParentheses =
                valueArgumentList?.arguments ?: emptyList<CjValueArgument>()
            val functionLiteralArguments: List<CjLambdaArgument> =
                lambdaArguments
            if (functionLiteralArguments.isEmpty()) {
                return valueArgumentsInParentheses
            }
            val allValueArguments: MutableList<CjValueArgument> =
                Lists.newArrayList<CjValueArgument>()
            allValueArguments.addAll(valueArgumentsInParentheses)
            allValueArguments.addAll(functionLiteralArguments)
            return allValueArguments
        }


    override fun toString(): String {
        return node.elementType.toString()
    }
}
