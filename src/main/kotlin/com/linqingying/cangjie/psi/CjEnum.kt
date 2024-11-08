package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.linqingying.cangjie.psi.stubs.CangJieEnumStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes.ENUM_BODY

class CjEnum : CjTypeStatement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieEnumStub) : super(stub, CjStubElementTypes.ENUM)

    override fun toString(): String = node.elementType.toString() + ": " + name

    override val typeName: String
        get() = "enum"

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitEnum(this, data)
    }

    override fun getBody(): CjEnumBody? {
        return getStubOrPsiChild(ENUM_BODY)
    }

    val entry: List<CjEnumEntry>
        get() {
            return body?.entrys ?: emptyList()
        }
}
