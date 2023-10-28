package com.huawei.cangjie.psi

import com.huawei.cangjie.CjNodeTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement



class CjSuperExpression(node: ASTNode) : CjInstanceExpressionWithLabel(node), CjStatementExpression {
    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D): R {
        return visitor.visitSuperExpression(this, data)
    }

    val superTypeQualifier: CjTypeReference?
        /**
         * class A : B, C {
         * override fun foo() {
         * super<B>.foo()
         * super<C>.foo()
         * }
         * }
        </C></B> */
        get() = findChildByType<PsiElement>(CjNodeTypes.TYPE_REFERENCE) as CjTypeReference?
}

