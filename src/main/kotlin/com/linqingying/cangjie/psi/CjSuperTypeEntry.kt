package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjSuperTypeEntry : CjSuperTypeListEntry {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJiePlaceHolderStub<out CjSuperTypeListEntry >) : super(
        stub,
        CjStubElementTypes.SUPER_TYPE_ENTRY
    )

    override fun toString(): String {
        return node.elementType.toString()
    }

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitSuperTypeEntry(this, data)
    }
}
