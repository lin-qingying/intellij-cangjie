package com.linqingying.cangjie.psi.stubs.elements

import com.linqingying.cangjie.psi.CjBasicType
import com.linqingying.cangjie.psi.stubs.CangJieBasicTypeStub
import com.linqingying.cangjie.psi.stubs.impl.CangJieBasicTypeStubImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream

class CjBasicTypeElementType(
    debugString: String
) : CjStubElementType<CangJieBasicTypeStub, CjBasicType>(
    debugString, CjBasicType::class.java, CangJieBasicTypeStub::class.java
) {
    override fun serialize(stub: CangJieBasicTypeStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.basicType)

    }

    override fun createPsi(stub: CangJieBasicTypeStub): CjBasicType {
        return CjBasicType(stub)
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieBasicTypeStub {

        val name = dataStream.readName()
        return CangJieBasicTypeStubImpl(parentStub, name!!.string)
    }

    override fun createStub(psi: CjBasicType, parentStub: StubElement<out PsiElement>?): CangJieBasicTypeStub {
        return CangJieBasicTypeStubImpl(parentStub, psi.name)
    }
}
