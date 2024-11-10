package com.linqingying.cangjie.psi.stubs.elements

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.psi.CjCatchParameter
import com.linqingying.cangjie.psi.stubs.CangJieCatchParameterStub
import com.linqingying.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import com.linqingying.cangjie.psi.stubs.impl.CangJieCatchParameterStubImpl

class CjCatchParameterElementType(debugName: String) : CjStubElementType<CangJieCatchParameterStub, CjCatchParameter>(

    debugName, CjCatchParameter::class.java,
    CangJieCatchParameterStub::class.java
) {
    override fun serialize(stub: CangJieCatchParameterStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        val name = stub.getFqName()
        dataStream.writeName(name?.asString())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieCatchParameterStub {
        val name = dataStream.readName()

        val fqName = dataStream.readName()

        return CangJieCatchParameterStubImpl(fqName,name, parentStub )

    }

    override fun indexStub(stub: CangJieCatchParameterStub, sink: IndexSink) {
        getInstance().indexParameter(stub, sink)

    }

    override fun createStub(
        psi: CjCatchParameter,
        parentStub: StubElement<out PsiElement>?
    ): CangJieCatchParameterStub {

        val fqName = psi.fqName
        val fqNameRef = StringRef.fromString(fqName?.asString())
        return CangJieCatchParameterStubImpl(
            fqNameRef,
            StringRef.fromString(psi.name),
            parentStub,

        )
    }

}
