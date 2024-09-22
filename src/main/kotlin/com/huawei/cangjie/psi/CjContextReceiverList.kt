package com.huawei.cangjie.psi

import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.lang.ASTNode


//上下文接收器列表

class CjContextReceiverList : CjElementImplStub<CangJiePlaceHolderStub<CjContextReceiverList>> {
    constructor(node: ASTNode) : super(node)
    constructor(stub: CangJiePlaceHolderStub<CjContextReceiverList>) : super(stub, CjStubElementTypes.CONTEXT_RECEIVER_LIST)

    override fun <R : Any?, D : Any?> accept(visitor: CjVisitor<R, D>, data: D?): R  {
        return visitor.visitContextReceiverList(this, data)
    }

    fun contextReceivers(): List<CjContextReceiver> = getStubOrPsiChildrenAsList(CjStubElementTypes.CONTEXT_RECEIVER)

    fun typeReferences(): List<CjTypeReference> = contextReceivers().mapNotNull { it.typeReference() }

}
