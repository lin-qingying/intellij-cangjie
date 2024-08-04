package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJieClassStub
import com.linqingying.cangjie.psi.stubs.CangJieInterfaceStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjInterface:CjClassOrStruct {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieInterfaceStub) : super(stub, CjStubElementTypes.INTERFACE)


    override fun toString(): String = node.elementType.toString() + " : $name"
}
