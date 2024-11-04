package com.linqingying.cangjie.psi.stubs.elements

import com.linqingying.cangjie.psi.CjTypeAlias
import com.linqingying.cangjie.psi.psiUtil.StubUtils
import com.linqingying.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import com.linqingying.cangjie.psi.stubs.CangJieTypeAliasStub
import com.linqingying.cangjie.psi.stubs.impl.CangJieTypeAliasStubImpl
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

class CjTypeAliasElementType(debugName: String) :
    CjStubElementType<CangJieTypeAliasStub, CjTypeAlias>(
        debugName,
        CjTypeAlias::class.java,
        CangJieTypeAliasStub::class.java
    ) {
    override fun serialize(stub: CangJieTypeAliasStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeName(stub.getFqName()?.asString())
        StubUtils.serializeClassId(dataStream, stub.getClassId())

    }

    override fun indexStub(stub: CangJieTypeAliasStub, sink: IndexSink) {
        StubIndexService.getInstance().indexTypeAlias(stub, sink)
    }
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieTypeAliasStub {
        val name = dataStream.readName()
        val fqName = dataStream.readName()
        val classId = StubUtils.deserializeClassId(dataStream)

        return CangJieTypeAliasStubImpl(parentStub, name, fqName, classId)
    }

    override fun createStub(psi: CjTypeAlias, parentStub: StubElement<*>): CangJieTypeAliasStub {
        val name = StringRef.fromString(psi.name)
        val fqName = StringRef.fromString(psi.safeFqNameForLazyResolve()?.asString())
        val classId = StubUtils.createNestedClassId(parentStub, psi)

        return CangJieTypeAliasStubImpl(parentStub, name, fqName, classId)
    }

}
