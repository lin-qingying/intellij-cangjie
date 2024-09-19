package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjEnumEntryTypeEntry : CjElementImplStub<CangJiePlaceHolderStub<CjEnumEntryTypeEntry>> {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<CjEnumEntryTypeEntry>) : super(
        stub,
        CjStubElementTypes.SUPER_TYPE_ENTRY
    )

    override fun toString(): String {
        return node.elementType.toString()
    }

//    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
//        return visitor.visitSuperTypeEntry(this, data)
//    }
}
