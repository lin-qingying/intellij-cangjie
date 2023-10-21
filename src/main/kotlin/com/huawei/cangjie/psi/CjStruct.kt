package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieClassStub
import com.huawei.cangjie.psi.stubs.CangJieStructStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjStruct :CjClassOrStruct{

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieStructStub) : super(stub, CjStubElementTypes.STRUCT)

    override fun toString(): String = node.elementType.toString() + " : $name"
}
