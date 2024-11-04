package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJieFunctionStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IStubElementType

class CjMainFunction : CjFunctionImpl {
    constructor(stub: CangJieFunctionStub) : super(stub, CjStubElementTypes.MAIN_FUNC)
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJieFunctionStub, nodeType: IStubElementType<*, *>) : super(stub, nodeType)

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitMainFunction(this, data)
    }

    override fun getName(): String {
        return "main"
    }
}
