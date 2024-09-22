package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieEnumStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjEnum : CjTypeStatement {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieEnumStub) : super(stub, CjStubElementTypes.ENUM)

    override fun toString(): String = node.elementType.toString() + ": " + name


    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R  {
        return visitor.visitEnum(this, data)
    }
}
