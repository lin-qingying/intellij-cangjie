package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJieEnumStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjEnum : CjTypeStatement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieEnumStub) : super(stub, CjStubElementTypes.ENUM)

    override fun toString(): String = node.elementType.toString() + ": " + name

    override val typeName: String
        get() = "enum"
    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R  {
        return visitor.visitEnum(this, data)
    }
}
