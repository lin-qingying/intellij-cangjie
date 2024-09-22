package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieInterfaceStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjInterface : CjTypeStatement {

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieInterfaceStub) : super(stub, CjStubElementTypes.INTERFACE)

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R  {
        return visitor.visitInterface(this, data)
    }

    override fun toString(): String = node.elementType.toString() + " : $name"
}
