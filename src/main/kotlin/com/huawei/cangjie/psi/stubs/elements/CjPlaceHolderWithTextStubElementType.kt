package com.huawei.cangjie.psi.stubs.elements

import com.huawei.cangjie.psi.CjElementImplStub
import com.huawei.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub
import com.huawei.cangjie.psi.stubs.impl.CangJiePlaceHolderWithTextStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls



class CjPlaceHolderWithTextStubElementType<T : CjElementImplStub<out StubElement<*>>>(@NonNls debugName: String, psiClass: Class<T>) :
    CjStubElementType<CangJiePlaceHolderWithTextStub<T>, T>(debugName, psiClass, CangJiePlaceHolderWithTextStub::class.java) {

    override fun createStub(psi: T, parentStub: StubElement<*>): CangJiePlaceHolderWithTextStub<T> {
        return CangJiePlaceHolderWithTextStubImpl(parentStub, this, psi.text)
    }

    override fun serialize(stub: CangJiePlaceHolderWithTextStub<T>, dataStream: StubOutputStream) {
        dataStream.writeUTFFast(stub.text())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJiePlaceHolderWithTextStub<T> {
        val text = dataStream.readUTFFast()
        return CangJiePlaceHolderWithTextStubImpl(parentStub, this, text)
    }
}
