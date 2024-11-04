package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJieFunctionStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElementVisitor
import com.intellij.psi.stubs.IStubElementType


class CjClassInit: CjFunctionImpl {

    constructor(node: ASTNode) : super(node)

//
    constructor(stub: CangJieFunctionStub): super(stub, CjStubElementTypes.CLASS_INIT)
//
    constructor(stub: CangJieFunctionStub, nodeType: IStubElementType<*, *>): super(stub, nodeType)

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R   {
        return visitor.visitClassInitFunction(this, data)

    }


}
