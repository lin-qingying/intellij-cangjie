/*
 * Copyright 2026 LinQingYing. and contributors.
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

package org.cangnova.cangjie.psi.stubs.elements

import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjNamedFunction
import org.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import org.cangnova.cangjie.psi.stubs.CangJieNamedFunctionStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieNamedFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.deserialize
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.serialize
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import java.io.IOException
import org.cangnova.cangjie.name.*
import org.cangnova.cangjie.psi.psiUtil.isExtensionDeclaration

class CjFunctionElementType(debugName: @NonNls String) : CjStubElementType<CangJieNamedFunctionStub, CjNamedFunction>(
    debugName,
    CjNamedFunction::class.java,
    CangJieNamedFunctionStub::class.java,
) {

    override fun createStub(psi: CjNamedFunction, parentStub: StubElement<*>): CangJieNamedFunctionStub {
        val isTopLevel = psi.parent is CjFile
        val isExtension = psi.isExtensionDeclaration()
        val fqName = psi.safeFqNameForLazyResolve()
        val hasBlockBody = psi.hasBlockBody()
        val hasBody = psi.hasBody()
        return CangJieNamedFunctionStubImpl(
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
    override fun serialize(stub: CangJieNamedFunctionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.isTopLevel())

        val fqName = stub.getFqName()
        dataStream.writeName(fqName?.asString())

        dataStream.writeBoolean(stub.isExtension())
        dataStream.writeBoolean(stub.hasBlockBody())
        dataStream.writeBoolean(stub.hasBody())
        dataStream.writeBoolean(stub.hasTypeParameterListBeforeFunctionName())

        if (stub is CangJieNamedFunctionStubImpl) {
            serialize(stub.origin, dataStream)
        }
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieNamedFunctionStub {
        val name = dataStream.readName()
        val isTopLevel = dataStream.readBoolean()

        val fqNameAsString = dataStream.readName()
        val fqName = if (fqNameAsString != null) FqName(fqNameAsString.toString()) else null

        val isExtension = dataStream.readBoolean()
        val hasBlockBody = dataStream.readBoolean()
        val hasBody = dataStream.readBoolean()
        val hasTypeParameterListBeforeFunctionName = dataStream.readBoolean()
        //        bool mayHaveContract = dataStream.readBoolean();
        return CangJieNamedFunctionStubImpl(
            parentStub, CjStubElementTypes.FUNCTION, name, isTopLevel, fqName, isExtension, hasBlockBody, hasBody,
            hasTypeParameterListBeforeFunctionName, //                mayHaveContract,
            //                mayHaveContract ? CangJieNamedFunctionStubImpl.Companion.deserializeContract(dataStream) :

            deserialize(dataStream),
        )
    }

    override fun indexStub(stub: CangJieNamedFunctionStub, sink: IndexSink) {
        getInstance().indexFunction(stub, sink)
    }

    override fun getExternalId(): String {
        return NAME
    }

    companion object {
        private const val NAME = "cangjie.FUNCTION"
    }
}
