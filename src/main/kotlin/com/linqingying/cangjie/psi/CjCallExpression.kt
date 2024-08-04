package com.linqingying.cangjie.psi

import com.google.common.collect.Lists
import com.linqingying.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


class CjCallExpression(node: ASTNode) : CjExpressionImpl(node), CjCallElement, CjReferenceExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitCallExpression(this, data)
    }

    override fun getLambdaArguments(): List<CjLambdaArgument> {
        return findChildrenByType<CjLambdaArgument>(CjNodeTypes.LAMBDA_ARGUMENT)
    }

    override fun getTypeArguments(): List<CjTypeProjection> {

        return typeArgumentList?.arguments ?: emptyList()
    }


    override fun getTypeArgumentList(): CjTypeArgumentList? {
        return findChildByType<PsiElement>(CjNodeTypes.TYPE_ARGUMENT_LIST) as CjTypeArgumentList?
    }

    override fun getCalleeExpression(): CjExpression? {
        return findChildByClass(CjExpression::class.java)
    }

    override fun getValueArgumentList(): CjValueArgumentList? {
        return findChildByType(CjNodeTypes.VALUE_ARGUMENT_LIST) as CjValueArgumentList?

    }

    override fun getValueArguments(): List<CjValueArgument> {

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
