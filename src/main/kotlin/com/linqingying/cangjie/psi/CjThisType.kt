package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor


class CjThisType : CjElementImplStub<CangJiePlaceHolderStub<CjThisType>>, CjTypeElement {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJiePlaceHolderStub<CjThisType>) : super(stub, CjStubElementTypes.THIS_TYPE)





    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitThisType(this, data)
    }
}
