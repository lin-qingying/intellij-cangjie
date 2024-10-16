package com.huawei.cangjie.psi.stubs.elements

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjEnumEntry
import com.huawei.cangjie.psi.CjNamedDeclaration
import com.huawei.cangjie.psi.psiUtil.StubUtils.createNestedClassId
import com.huawei.cangjie.psi.psiUtil.StubUtils.deserializeClassId
import com.huawei.cangjie.psi.psiUtil.StubUtils.serializeClassId
import com.huawei.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import com.huawei.cangjie.psi.stubs.CangJieEnumEntryStub
import com.huawei.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import com.huawei.cangjie.psi.stubs.impl.CangJieEnumEntryStubImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
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

    private fun getStubType(): CjEnumEntryElementType {
        return CjStubElementTypes.ENUM_ENTRY
    }
}
