package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJieInterfaceStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjInterface : CjTypeStatement {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieInterfaceStub) : super(stub, CjStubElementTypes.INTERFACE)

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R  {
        return visitor.visitInterface(this, data)
    }
    override val typeName: String
        get() = "interface"
    override fun toString(): String = node.elementType.toString() + " : $name"
}
