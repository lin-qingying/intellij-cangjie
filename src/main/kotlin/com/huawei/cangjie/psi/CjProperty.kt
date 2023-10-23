package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePropertyStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

open class CjProperty :CjNamedDeclarationStub<CangJiePropertyStub> {



    constructor(stub: CangJiePropertyStub) : super(stub, CjStubElementTypes.PROPERTY)
    constructor(node: ASTNode) : super(node)


}
