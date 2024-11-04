package com.linqingying.cangjie.metadata.decompiler

import com.intellij.psi.stubs.StubElement
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.psi.CjContextReceiverList
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.linqingying.cangjie.psi.stubs.impl.CangJieContextReceiverStubImpl
import com.linqingying.cangjie.psi.stubs.impl.CangJiePlaceHolderStubImpl

internal class ContextReceiversListStubBuilder(c: ClsStubBuilderContext) {
    private val typeStubBuilder = TypeClsStubBuilder(c)

    fun createContextReceiverStubs(parent: StubElement<*>, contextReceiverTypes: List<ProtoBuf.Type>) {
        if (contextReceiverTypes.isEmpty()) return
        val contextReceiverListStub =
            CangJiePlaceHolderStubImpl<CjContextReceiverList>(parent, CjStubElementTypes.CONTEXT_RECEIVER_LIST)
        for (contextReceiverType in contextReceiverTypes) {
            val contextReceiverStub =
                CangJieContextReceiverStubImpl(contextReceiverListStub, CjStubElementTypes.CONTEXT_RECEIVER, label = null)
            typeStubBuilder.createTypeReferenceStub(contextReceiverStub, contextReceiverType)
        }
    }
}
