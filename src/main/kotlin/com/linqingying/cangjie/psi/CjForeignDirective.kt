package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJieForeignDirectiveStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjForeignDirective : CjElementImplStub<CangJieForeignDirectiveStub > {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieForeignDirectiveStub) : super(stub, CjStubElementTypes.FOREIGN)
}
