package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieEnumEntryStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

class CjEnumEntry : CjTypeStatement {
    constructor(node: ASTNode) : super(node)

    constructor(
        stub: CangJieEnumEntryStub

    ) : super(stub,  CjStubElementTypes.ENUM_ENTRY)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitEnumEntry(this, data)
    }

    override fun toString(): String {
        return node.elementType.toString()
    }

    val typeReference: CjTypeReference?
        get() = getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE)
}
