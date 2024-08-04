package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJieClassStub
import com.linqingying.cangjie.psi.stubs.CangJieStructStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjStruct :CjClassOrStruct{

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieStructStub) : super(stub, CjStubElementTypes.STRUCT)

    override fun toString(): String = node.elementType.toString() + " : $name"
}
