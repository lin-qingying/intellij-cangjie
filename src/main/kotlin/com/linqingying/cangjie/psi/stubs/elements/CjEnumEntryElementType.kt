package com.linqingying.cangjie.psi.stubs.elements

import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjEnumEntry
import com.linqingying.cangjie.psi.CjNamedDeclaration
import com.linqingying.cangjie.psi.psiUtil.StubUtils.createNestedClassId
import com.linqingying.cangjie.psi.psiUtil.StubUtils.deserializeClassId
import com.linqingying.cangjie.psi.psiUtil.StubUtils.serializeClassId
import com.linqingying.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import com.linqingying.cangjie.psi.stubs.CangJieEnumEntryStub
import com.linqingying.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import com.linqingying.cangjie.psi.stubs.impl.CangJieEnumEntryStubImpl
import java.io.IOException

class CjEnumEntryElementType(debugName: String) : CjStubElementType<CangJieEnumEntryStub, CjEnumEntry>(
    debugName,
    CjEnumEntry::class.java,
    CangJieEnumEntryStub::class.java
) {

    override fun createPsi(stub: CangJieEnumEntryStub): CjEnumEntry {
        return CjEnumEntry(stub)
    }

    override fun createPsiFromAst(node: ASTNode): CjEnumEntry {
        return CjEnumEntry(node)
    }

    override fun createStub(psi: CjEnumEntry, parentStub: StubElement<*>?): CangJieEnumEntryStub {
        val fqNameByParent: FqName? = (psi as CjNamedDeclaration).safeFqNameForLazyResolve() //psi.safeFqNameForLazyResolveByParent()

        val fqNameByPackage = psi.safeFqNameForLazyResolve()
        val classId = createNestedClassId(parentStub!!, psi)
        return CangJieEnumEntryStubImpl(
            getStubType(), parentStub as StubElement<*>?,
            StringRef.fromString(fqNameByParent?.asString()),
            StringRef.fromString(fqNameByPackage?.asString()), classId,
            StringRef.fromString(psi.name),

            psi.isLocal
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieEnumEntryStub, dataStream: StubOutputStream) {

        dataStream.writeName(stub.name)


        val fqNameByParent = stub.getFqName()
        val fqNameByPacage = stub.fqNameByPackage

        dataStream.writeName(fqNameByParent?.asString())
        dataStream.writeName(fqNameByPacage?.asString())

        serializeClassId(dataStream, stub.getClassId())


        dataStream.writeBoolean(stub.isLocal())


//        val superNames = stub.getSuperNames()
//        dataStream.writeVarInt(superNames.size)
//        for (name in superNames) {
//            dataStream.writeName(name)
//        }
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieEnumEntryStub {
        val name = dataStream.readName()
        val qualifiedNameByParent = dataStream.readName()
        val qualifiedNameByPackage = dataStream.readName()

        val classId = deserializeClassId(dataStream)

        val isLocal = try{
            dataStream.readBoolean()
        }catch (e:IOException){
            false
        }


        return CangJieEnumEntryStubImpl(
            getStubType(), parentStub, qualifiedNameByParent, qualifiedNameByPackage, classId, name,
            isLocal
        )
    }

    override fun indexStub(stub: CangJieEnumEntryStub, sink: IndexSink) {
        getInstance().indexEnumEntry(stub, sink)
    }
companion object{
      fun getStubType(): CjEnumEntryElementType {
        return CjStubElementTypes.ENUM_ENTRY
    }
}

}
