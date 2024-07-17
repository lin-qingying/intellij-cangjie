package com.huawei.cangjie.psi.stubs.elements

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjClassInit
import com.huawei.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import com.huawei.cangjie.psi.stubs.CangJieFunctionStub
import com.huawei.cangjie.psi.stubs.impl.CangJieFunctionStubImpl
import com.huawei.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.deserialize
import com.huawei.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.serialize
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

class CjClassInitElementType(debugName: String) :
    CjStubElementType<CangJieFunctionStub, CjClassInit>(debugName, CjClassInit::class.java,CangJieFunctionStub::class.java){
    override fun serialize(stub: CangJieFunctionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(false)

        val fqName = stub.getFqName()
        dataStream.writeName(fqName?.asString())

        dataStream.writeBoolean(stub.isExtension())
        dataStream.writeBoolean(stub.hasBlockBody())
        dataStream.writeBoolean(stub.hasBody())
        dataStream.writeBoolean(stub.hasTypeParameterListBeforeFunctionName())
//        bool haveContract = stub.mayHaveContract();
//        dataStream.writeBoolean(haveContract);
        //        bool haveContract = stub.mayHaveContract();
//        dataStream.writeBoolean(haveContract);
        if (stub is CangJieFunctionStubImpl) {
            serialize(stub.origin, dataStream)
        }
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieFunctionStub {
        val name = dataStream.readName()


        val fqNameAsString = dataStream.readName()
        val fqName = if (fqNameAsString != null) FqName(fqNameAsString.toString()) else null

        val isExtension = dataStream.readBoolean()
        val hasBlockBody = dataStream.readBoolean()
        val hasBody = dataStream.readBoolean()
        val hasTypeParameterListBeforeFunctionName = dataStream.readBoolean()
//        bool mayHaveContract = dataStream.readBoolean();
        //        bool mayHaveContract = dataStream.readBoolean();
        return CangJieFunctionStubImpl(
            parentStub, CjStubElementTypes.CLASS_INIT, name, false, fqName, isExtension, hasBlockBody, hasBody,
            hasTypeParameterListBeforeFunctionName,
            null,/*contract*/
            deserialize(dataStream)
        )
    }

    override fun createStub(psi: CjClassInit, parentStub: StubElement<out PsiElement>?): CangJieFunctionStub {

        val isExtension = psi.getReceiverTypeReference() != null
        val fqName = psi.safeFqNameForLazyResolve()
        val hasBlockBody = psi.hasBlockBody()
        val hasBody = psi.hasBody()
        return CangJieFunctionStubImpl(
            parentStub, CjStubElementTypes.CLASS_INIT, StringRef.fromString(psi.getName()), false, fqName,
            isExtension, hasBlockBody, hasBody, psi.hasTypeParameterListBeforeFunctionName(),
            null,
            null
        )
    }

}
