package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJieModifierListStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode


class CjDeclarationModifierList : CjModifierList {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieModifierListStub) : super(stub, CjStubElementTypes.MODIFIER_LIST)
}
