package com.linqingying.cangjie.psi.stubs.elements

import com.linqingying.cangjie.psi.CjContextReceiver
import com.linqingying.cangjie.psi.stubs.CangJieContextReceiverStub
import com.linqingying.cangjie.psi.stubs.impl.CangJieContextReceiverStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream


class CjContextReceiverElementType(debugName: String) : CjStubElementType<CangJieContextReceiverStub, CjContextReceiver>(
    debugName,
    CjContextReceiver::class.java,
    CangJieContextReceiverStub::class.java
) {
    override fun createStub(
        element: CjContextReceiver,
        parentStub: StubElement<*>?
    ): CangJieContextReceiverStub = CangJieContextReceiverStubImpl(parentStub, this, element.labelName())

    override fun serialize(stub: CangJieContextReceiverStub, dataStream: StubOutputStream) =
        dataStream.writeName(stub.getLabel())

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?) =
        CangJieContextReceiverStubImpl(parentStub, this, dataStream.readNameString())
}
