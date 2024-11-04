package com.linqingying.cangjie.psi.stubs.elements

import com.linqingying.cangjie.psi.CjTupleType

import com.linqingying.cangjie.psi.stubs.CangJieTupleTypeStub
import com.linqingying.cangjie.psi.stubs.impl.CangJieTupleTypeStubImpl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CjTupleTypeElementType(debugName:String) :CjStubElementType<CangJieTupleTypeStub,CjTupleType>(debugName,
    CjTupleType::class.java,
    CangJieTupleTypeStub::class.java) {
    override fun serialize(stub: CangJieTupleTypeStub, dataStream: StubOutputStream) {
//        dataStream.writeName(debugName)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieTupleTypeStub {
        return CangJieTupleTypeStubImpl(parentStub)
    }

    override fun createStub(psi: CjTupleType, parentStub: StubElement<out PsiElement>?): CangJieTupleTypeStub {
       return CangJieTupleTypeStubImpl(parentStub)
    }
}
