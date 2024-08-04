package com.linqingying.cangjie.psi


import com.linqingying.cangjie.psi.stubs.CangJieExtendStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjExtend:CjClassOrStruct {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieExtendStub) : super(stub, CjStubElementTypes.EXTEND)

    override fun toString(): String = node.elementType.toString() + ": " + name
}
