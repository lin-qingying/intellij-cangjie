package com.huawei.cangjie.psi.stubs.elements

import com.huawei.cangjie.psi.CjValueArgument
import com.huawei.cangjie.psi.stubs.CangJieValueArgumentStub
import com.huawei.cangjie.psi.stubs.impl.CangJieValueArgumentStubImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream


class CjValueArgumentElementType<T : CjValueArgument>(debugName: String, psiClass: Class<T>) :
    CjStubElementType<CangJieValueArgumentStub<T>, T>(debugName, psiClass, CangJieValueArgumentStub::class.java) {

    override fun createStub(psi: T, parentStub: StubElement<out PsiElement>?): CangJieValueArgumentStub<T> {
        return CangJieValueArgumentStubImpl(parentStub, this, psi.isSpread)
    }

    override fun serialize(stub: CangJieValueArgumentStub<T>, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isSpread())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<PsiElement>?): CangJieValueArgumentStub<T> {
        val isSpread = dataStream.readBoolean()
        return CangJieValueArgumentStubImpl(parentStub, this, isSpread)
    }
}
