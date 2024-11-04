package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor

class CjSpawnExpression(node: ASTNode) : CjCallExpression(node) {
    override fun accept(visitor: PsiElementVisitor) {
        super.accept(visitor)
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitSpawnExpression(this, data)
    }
    val lambdaExpression: CjLambdaExpression?
        get() = lambdaArgument?.getLambdaExpression()
    val lambdaArgument: CjLambdaArgument?
        get() = findChildByClass(CjLambdaArgument::class.java)
    override val calleeExpression: CjLambdaExpression?
        get() {
            return lambdaExpression
        }
}
