package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjTypeConstraintList : CjElementImplStub<CangJiePlaceHolderStub<CjTypeConstraintList > > {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjTypeConstraintList >) : super(
        stub,
        CjStubElementTypes.TYPE_CONSTRAINT_LIST
    )

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitTypeConstraintList(this, data)
    }


    val constraints: List<CjTypeConstraint>
        get() = getStubOrPsiChildrenAsList(
            CjStubElementTypes.TYPE_CONSTRAINT
        )
}



