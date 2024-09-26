package com.huawei.cangjie.psi

import com.intellij.lang.ASTNode

class CjUnsafeExpression(node: ASTNode) : CjCallExpression(node) {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitUnsafeExpression(this, data)
    }

    val lambdaExpression: CjLambdaExpression?
        get() = lambdaArgument?.getLambdaExpression()
    val lambdaArgument: CjLambdaArgument?
        get() = findChildByClass(CjLambdaArgument::class.java)
}
