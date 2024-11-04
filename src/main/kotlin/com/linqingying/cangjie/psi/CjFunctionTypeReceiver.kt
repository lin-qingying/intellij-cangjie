package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode


class CjFunctionTypeReceiver :
    CjElementImplStub<CangJiePlaceHolderStub<CjFunctionTypeReceiver>> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJiePlaceHolderStub<CjFunctionTypeReceiver>) : super(
        stub,
        CjStubElementTypes.FUNCTION_TYPE_RECEIVER
    )

    val typeReference: CjTypeReference
        get() = getRequiredStubOrPsiChild<CangJiePlaceHolderStub<CjTypeReference>, CjTypeReference>(CjStubElementTypes.TYPE_REFERENCE)
}

