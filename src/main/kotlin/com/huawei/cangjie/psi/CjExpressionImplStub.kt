package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.CjExpressionImpl.Companion.replaceExpression
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement
import com.intellij.util.IncorrectOperationException

abstract class CjExpressionImplStub<T : StubElement<*> > : CjElementImplStub<T>, CjExpression {
    constructor(stub: T, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    constructor(node: ASTNode) : super(node)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitExpression(this, data)
    }

    @Throws(IncorrectOperationException::class)
    override fun replace(newElement: PsiElement): PsiElement {
        return replaceExpression(this, newElement, true) { newElement: PsiElement -> this.rawReplace(newElement) }
    }

    fun rawReplace(newElement: PsiElement): PsiElement {
        return super.replace(newElement)
    }

    override fun getParent(): PsiElement ?{
        val stub = getStub()
        if (stub != null) {
            return stub.parentStub.psi
        }
        return super.getParent()
    }
}
