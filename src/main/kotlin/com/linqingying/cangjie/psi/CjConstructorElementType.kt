package com.linqingying.cangjie.psi

import com.linqingying.cangjie.psi.stubs.CangJieConstructorStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import java.io.IOException



abstract class CjConstructorElementType<T : CjConstructor<T>>(
    @NonNls debugName: String,
    tClass: Class<T>,
    stubClass: Class<CangJieConstructorStub<*>>
) : CjStubElementType<CangJieConstructorStub<T>, T>(debugName, tClass, stubClass) {
    protected abstract fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isDelegatedCallToThis: Boolean,
    ): CangJieConstructorStub<T>

    protected abstract fun isDelegatedCallToThis(constructor: T): Boolean

    override fun createStub(psi: T, parentStub: StubElement<*>): CangJieConstructorStub<T> {
        val hasBody = psi.hasBody()
        val isDelegatedCallToThis = isDelegatedCallToThis(psi)
        return newStub(parentStub, StringRef.fromString(psi.name), hasBody, isDelegatedCallToThis)
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieConstructorStub<T>, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.hasBody())
        dataStream.writeBoolean(stub.isDelegatedCallToThis())
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieConstructorStub<T> {
        val name = dataStream.readName()
        val hasBody = dataStream.readBoolean()
        val isDelegatedCallToThis = dataStream.readBoolean()
        return newStub(parentStub, name, hasBody, isDelegatedCallToThis)
    }

    override fun indexStub(stub: CangJieConstructorStub<T>, sink: IndexSink) {
    }
}
