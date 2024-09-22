package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieForeignDirectiveStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjForeignDirective : CjElementImplStub<CangJieForeignDirectiveStub > {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieForeignDirectiveStub) : super(stub, CjStubElementTypes.FOREIGN)
}
