package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieModifierListStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode


class CjDeclarationModifierList : CjModifierList {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieModifierListStub) : super(stub, CjStubElementTypes.MODIFIER_LIST)
}
