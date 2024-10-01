package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.CjExpressionImpl.Companion.replaceExpression
import com.huawei.cangjie.psi.stubs.CangJieConstantExpressionStub
import com.huawei.cangjie.psi.stubs.elements.CjConstantExpressionElementType.Companion.kindToConstantElementType
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.util.IncorrectOperationException

class CjConstantExpression

    : CjElementImplStub<CangJieConstantExpressionStub>, CjExpression {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieConstantExpressionStub) : super(stub, kindToConstantElementType(stub.kind()))

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitConstantExpression(this, data)
    }


    @Throws(IncorrectOperationException::class)
    override fun replace(newElement: PsiElement): PsiElement {
        return replaceExpression(this, newElement, true) { newElement: PsiElement? ->
            super.replace(
                newElement!!
            )
        }
    }
}
