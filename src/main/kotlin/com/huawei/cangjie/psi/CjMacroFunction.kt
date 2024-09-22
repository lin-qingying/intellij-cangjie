package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJieFunctionStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjMacroFunction : CjFunctionImpl {
    constructor(node: ASTNode) : super(node)

    constructor(stub: CangJieFunctionStub) : super(stub, CjStubElementTypes.MACRO)


}
