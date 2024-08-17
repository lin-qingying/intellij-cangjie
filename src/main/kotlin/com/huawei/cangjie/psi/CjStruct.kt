package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieStructStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjStruct :CjTypeStatement{

    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieStructStub) : super(stub, CjStubElementTypes.STRUCT)

    override fun toString(): String = node.elementType.toString() + " : $name"

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitStruct(this, data)
    }
}
