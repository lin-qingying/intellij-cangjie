package com.linqingying.cangjie.psi

import com.intellij.lang.ASTNode
import com.linqingying.cangjie.psi.stubs.CangJieFunctionStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes

class CjMacroDeclaration : CjFunctionImpl {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieFunctionStub) : super(stub, CjStubElementTypes.MACRO)

//    override val isStatic: Boolean
//        get() = false
    override val isTopLevel: Boolean
        get() = true

    override fun <R, D> accept(visitor: CjVisitor<R, D>, data: D?): R {
        return visitor.visitMacroDeclaration(this, data)
    }
}
