package com.linqingying.cangjie.psi.stubs.elements

import com.linqingying.cangjie.psi.CjEnum
import com.linqingying.cangjie.psi.CjExtend
import com.linqingying.cangjie.psi.psiUtil.StubUtils
import com.linqingying.cangjie.psi.psiUtil.getSuperNames
import com.linqingying.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import com.linqingying.cangjie.psi.stubs.CangJieEnumStub
import com.linqingying.cangjie.psi.stubs.CangJieExtendStub
import com.linqingying.cangjie.psi.stubs.impl.CangJieEnumStubImpl
import com.linqingying.cangjie.psi.stubs.impl.CangJieExtendStubImpl
import com.linqingying.cangjie.psi.stubs.impl.Utils
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

class CjExtendElementType(debugName:String):CjStubElementType<CangJieExtendStub, CjExtend>(debugName,
    CjExtend::class.java, CangJieExtendStub::class.java)  {


    override fun serialize(stub: CangJieExtendStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)

        val fqName = stub.getFqName()
        dataStream.writeName(fqName?.asString())

        StubUtils.serializeClassId(dataStream, stub.getClassId())

        dataStream.writeBoolean(stub.isLocal())


        val superNames = stub.getSuperNames()
        dataStream.writeVarInt(superNames.size)
        for (name in superNames) {
            dataStream.writeName(name)
        }
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieExtendStub {
        val name = dataStream.readName()
        val qualifiedName = dataStream.readName()

        val classId = StubUtils.deserializeClassId(dataStream)

        val isLocal = dataStream.readBoolean()


        val superCount = dataStream.readVarInt()
        val superNames = StringRef.createArray(superCount)
        for (i in 0 until superCount) {
            superNames[i] = dataStream.readName()
        }

        return CangJieExtendStubImpl(
            CjStubElementTypes.EXTEND, parentStub, qualifiedName, classId, name, superNames,
            isLocal
        )
    }


    override fun createStub(psi: CjExtend, parentStub: StubElement<out PsiElement>?): CangJieExtendStub {
        val fqName = psi.safeFqNameForLazyResolve()

        val superNames = psi.getSuperNames()
        val classId = StubUtils.createNestedClassId(parentStub!!, psi)
        return CangJieExtendStubImpl(
            CjStubElementTypes.EXTEND, parentStub as StubElement<*>?,
            StringRef.fromString(fqName?.asString()), classId,
            StringRef.fromString(psi.getName()),
            Utils.wrapStrings(superNames),
            psi.isLocal()
        )
    }

    override fun createPsi(stub: CangJieExtendStub): CjExtend {
        return CjExtend(stub)
    }

    override fun createPsiFromAst(node: ASTNode): CjExtend {
        return CjExtend(node)
    }
}
