package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode

class CjTypeList: CjElementImplStub<CangJiePlaceHolderStub<CjTypeList>> {

    constructor(node:ASTNode) : super(node)

    constructor(stub:CangJiePlaceHolderStub<CjTypeList>) : super(stub,CjStubElementTypes.TYPE_LIST)
}
