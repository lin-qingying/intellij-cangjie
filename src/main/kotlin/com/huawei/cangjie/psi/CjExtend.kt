package com.huawei.cangjie.psi


import com.huawei.cangjie.psi.stubs.CangJieExtendStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjExtend:CjTypeStatement {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieExtendStub) : super(stub, CjStubElementTypes.EXTEND)

    override fun toString(): String = node.elementType.toString() + ": " + name
}
