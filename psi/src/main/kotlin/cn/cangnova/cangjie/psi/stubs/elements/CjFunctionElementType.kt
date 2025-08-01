/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.psi.stubs.elements

import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjNamedFunction
import cn.cangnova.cangjie.psi.CjNamedFunctionForExtend
import cn.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import cn.cangnova.cangjie.psi.stubs.CangJieFunctionForExtendStub
import cn.cangnova.cangjie.psi.stubs.CangJieFunctionStub
import cn.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import cn.cangnova.cangjie.psi.stubs.impl.CangJieFunctionForExtendStubImpl
import cn.cangnova.cangjie.psi.stubs.impl.CangJieFunctionStubImpl
import cn.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.deserialize
import cn.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.serialize
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import java.io.IOException
import cn.cangnova.cangjie.name.*

class CjFunctionElementType(debugName: @NonNls String) : CjStubElementType<CangJieFunctionStub, CjNamedFunction>(
    debugName,
    CjNamedFunction::class.java,
    CangJieFunctionStub::class.java,
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
            psi.hasTypeParameterListBeforeFunctionName(), //                psi.mayHaveContract(),
            null,

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
            hasTypeParameterListBeforeFunctionName, //                mayHaveContract,
            //                mayHaveContract ? CangJieFunctionStubImpl.Companion.deserializeContract(dataStream) :

            deserialize(dataStream),
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
        CangJieFunctionStub::class.java,
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

            deserialize(dataStream),
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
