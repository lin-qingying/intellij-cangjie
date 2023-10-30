package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjTypeList: CjElementImplStub<CangJiePlaceHolderStub<CjTypeList>> {

    constructor(node:ASTNode) : super(node)

    constructor(stub:CangJiePlaceHolderStub<CjTypeList>) : super(stub,CjStubElementTypes.TYPE_LIST)
}
