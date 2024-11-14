package com.linqingying.cangjie.psi.stubs.elements

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.psi.CjNamedFunction
import com.linqingying.cangjie.psi.CjNamedFunctionForExtend
import com.linqingying.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import com.linqingying.cangjie.psi.stubs.CangJieFunctionForExtendStub
import com.linqingying.cangjie.psi.stubs.CangJieFunctionStub
import com.linqingying.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import com.linqingying.cangjie.psi.stubs.impl.CangJieFunctionForExtendStubImpl
import com.linqingying.cangjie.psi.stubs.impl.CangJieFunctionStubImpl
import com.linqingying.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.deserialize
import com.linqingying.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.serialize
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjFunctionElementType(debugName: @NonNls String) : CjStubElementType<CangJieFunctionStub, CjNamedFunction>(
    debugName,
    CjNamedFunction::class.java,
    CangJieFunctionStub::class.java
) {


    override fun createStub(psi: CjNamedFunction, parentStub: StubElement<*>): CangJieFunctionStub {
        val isTopLevel = psi.parent is CjFile
        val isExtension = psi.receiverTypeReference != null
        val fqName = psi.safeFqNameForLazyResolve()
        val hasBlockBody = psi.hasBlockBody()
        val hasBody = psi.hasBody()
        return CangJieFunctionStubImpl(
            parentStub,
            CjStubElementTypes.FUNCTION,
            StringRef.fromString(psi.name),
            isTopLevel,
            fqName,
            isExtension,
            hasBlockBody,
            hasBody,
            psi.hasTypeParameterListBeforeFunctionName(),  //                psi.mayHaveContract(),
            null,
            null
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieFunctionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.isTopLevel())

        val fqName = stub.getFqName()
        dataStream.writeName(fqName?.asString())

        dataStream.writeBoolean(stub.isExtension())
        dataStream.writeBoolean(stub.hasBlockBody())
        dataStream.writeBoolean(stub.hasBody())
        dataStream.writeBoolean(stub.hasTypeParameterListBeforeFunctionName())


        if (stub is CangJieFunctionStubImpl) {


            serialize(stub.origin, dataStream)
        }
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieFunctionStub {
        val name = dataStream.readName()
        val isTopLevel = dataStream.readBoolean()

        val fqNameAsString = dataStream.readName()
        val fqName = if (fqNameAsString != null) FqName(fqNameAsString.toString()) else null

        val isExtension = dataStream.readBoolean()
        val hasBlockBody = dataStream.readBoolean()
        val hasBody = dataStream.readBoolean()
        val hasTypeParameterListBeforeFunctionName = dataStream.readBoolean()
        //        bool mayHaveContract = dataStream.readBoolean();
        return CangJieFunctionStubImpl(
            parentStub, CjStubElementTypes.FUNCTION, name, isTopLevel, fqName, isExtension, hasBlockBody, hasBody,
            hasTypeParameterListBeforeFunctionName,  //                mayHaveContract,
            //                mayHaveContract ? CangJieFunctionStubImpl.Companion.deserializeContract(dataStream) :
            null,


            deserialize(dataStream)
        )
    }

    override fun indexStub(stub: CangJieFunctionStub, sink: IndexSink) {
        getInstance().indexFunction(stub, sink)
    }

    override fun getExternalId(): String {
        return NAME
    }

    companion object {
        private const val NAME = "cangjie.FUNCTION"
    }
}

class CjFunctionForExtendElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieFunctionStub, CjNamedFunctionForExtend>(
        debugName,
        CjNamedFunctionForExtend::class.java,
        CangJieFunctionStub::class.java
    ) {


    override fun createStub(psi: CjNamedFunctionForExtend, parentStub: StubElement<*>): CangJieFunctionStub {
        val isTopLevel = psi.parent is CjFile
        val isExtension = psi.receiverTypeReference != null
        val fqName = psi.safeFqNameForLazyResolve()
        val hasBlockBody = psi.hasBlockBody()
        val hasBody = psi.hasBody()
        return CangJieFunctionForExtendStubImpl(
            parentStub,
            CjStubElementTypes.FUNCTION_EXTEND,
            StringRef.fromString(psi.name),
            isTopLevel,
            fqName,
            isExtension,
            hasBlockBody,
            hasBody,
            psi.hasTypeParameterListBeforeFunctionName(),
            null,
            null
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieFunctionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.isTopLevel())

        val fqName = stub.getFqName()
        dataStream.writeName(fqName?.asString())

        dataStream.writeBoolean(stub.isExtension())
        dataStream.writeBoolean(stub.hasBlockBody())
        dataStream.writeBoolean(stub.hasBody())
        dataStream.writeBoolean(stub.hasTypeParameterListBeforeFunctionName())


        if (stub is CangJieFunctionForExtendStubImpl) {

            serialize(stub.origin, dataStream)
        }
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieFunctionForExtendStub {
        val name = dataStream.readName()
        val isTopLevel = dataStream.readBoolean()

        val fqNameAsString = dataStream.readName()
        val fqName = if (fqNameAsString != null) FqName(fqNameAsString.toString()) else null

        val isExtension = dataStream.readBoolean()
        val hasBlockBody = dataStream.readBoolean()
        val hasBody = dataStream.readBoolean()
        val hasTypeParameterListBeforeFunctionName = dataStream.readBoolean()

        return CangJieFunctionForExtendStubImpl(
            parentStub, CjStubElementTypes.FUNCTION_EXTEND, name, isTopLevel, fqName, isExtension, hasBlockBody, hasBody,
            hasTypeParameterListBeforeFunctionName,

            null,


            deserialize(dataStream)
        )
    }

    override fun indexStub(stub: CangJieFunctionStub, sink: IndexSink) {
        getInstance().indexFunction(stub, sink)
    }

    override fun getExternalId(): String {
        return NAME
    }

    companion object {
        private const val NAME = "cangjie.FUNCTION_EXTEND"
    }
}
