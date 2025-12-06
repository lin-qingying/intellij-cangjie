/*
 * Copyright 2025 LinQingYing. and contributors.
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

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjClassInit
import org.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import org.cangnova.cangjie.psi.stubs.CangJieFunctionStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.deserialize
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.serialize
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

class CjClassInitElementType(debugName: String) :
    CjStubElementType<CangJieFunctionStub, CjClassInit>(debugName, CjClassInit::class.java, CangJieFunctionStub::class.java) {
    override fun serialize(stub: CangJieFunctionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)

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

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieFunctionStub {
        val name = dataStream.readName()

        val fqNameAsString = dataStream.readName()
        val fqName = if (fqNameAsString != null) FqName(fqNameAsString.toString()) else null

        val isExtension = dataStream.readBoolean()
        val hasBlockBody = dataStream.readBoolean()
        val hasBody = dataStream.readBoolean()
        val hasTypeParameterListBeforeFunctionName = dataStream.readBoolean()

        return CangJieFunctionStubImpl(
            parentStub, CjStubElementTypes.CLASS_INIT, name, false, fqName, isExtension, hasBlockBody, hasBody,
            hasTypeParameterListBeforeFunctionName,
            /*contract*/
            deserialize(dataStream),
        )
    }

    override fun createStub(psi: CjClassInit, parentStub: StubElement<out PsiElement>?): CangJieFunctionStub {
        val isExtension = psi.receiverTypeReference != null
        val fqName = psi.safeFqNameForLazyResolve()
        val hasBlockBody = psi.hasBlockBody()
        val hasBody = psi.hasBody()
        return CangJieFunctionStubImpl(
            parentStub, CjStubElementTypes.CLASS_INIT, StringRef.fromString(psi.getName()), false, fqName,
            isExtension, hasBlockBody, hasBody, psi.hasTypeParameterListBeforeFunctionName(),

            null,
        )
    }
}
