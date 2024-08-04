package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePropertyStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

open class CjProperty :CjNamedDeclarationStub<CangJiePropertyStub> {



    constructor(stub: CangJiePropertyStub) : super(stub, CjStubElementTypes.PROPERTY)
    constructor(node: ASTNode) : super(node)


    override fun toString(): String  = super.toString() + ": " + name
}
