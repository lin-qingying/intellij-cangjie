package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement


class CjCallExpression(node: ASTNode) : CjExpressionImpl(node), CjCallElement, CjReferenceExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitCallExpression(this, data)
    }
    override fun getLambdaArguments(): List<CjLambdaArgument> {
        return findChildrenByType<CjLambdaArgument>(CjNodeTypes.LAMBDA_ARGUMENT)
    }


    override fun getTypeArgumentList(): CjTypeArgumentList? {
        return findChildByType<PsiElement>(CjNodeTypes.TYPE_ARGUMENT_LIST) as CjTypeArgumentList?
    }

    override fun getCalleeExpression(): CjExpression? {
        return findChildByClass(CjExpression::class.java)
    }

    override fun getValueArgumentList(): CjValueArgumentList? {
        return findChildByType (CjNodeTypes.VALUE_ARGUMENT_LIST) as CjValueArgumentList?

    }


    override fun toString(): String {
        return node.elementType.toString()
    }
}
