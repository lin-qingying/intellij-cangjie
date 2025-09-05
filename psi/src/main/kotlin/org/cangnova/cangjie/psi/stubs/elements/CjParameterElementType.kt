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
package org.cangnova.cangjie.psi.stubs.elements

import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.psi.stubs.CangJieParameterStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieParameterStubImpl
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjParameterElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieParameterStub, CjParameter>(
        debugName,
        CjParameter::class.java,
        CangJieParameterStub::class.java,
    ) {
    override fun createStub(psi: CjParameter, parentStub: StubElement<*>?): CangJieParameterStub {
        val fqName = psi.fqName
        val fqNameRef = StringRef.fromString(fqName?.asString())
        return CangJieParameterStubImpl(
            parentStub,
            fqNameRef,
            StringRef.fromString(psi.name),
            psi.isMutable,
            psi.hasLetOrVar(),
            psi.hasDefaultValue(),
            null,
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieParameterStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.isMutable())
        dataStream.writeBoolean(stub.hasValOrVar())
        dataStream.writeBoolean(stub.hasDefaultValue())
        val name = stub.getFqName()
        dataStream.writeName(name?.asString())
        dataStream.writeName(if (stub is CangJieParameterStubImpl) stub.functionTypeParameterName else null)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieParameterStub {
        val name = dataStream.readName()
        val isMutable = dataStream.readBoolean()
        val hasValOrValNode = dataStream.readBoolean()
        val hasDefaultValue = dataStream.readBoolean()
        val fqName = dataStream.readName()

        return CangJieParameterStubImpl(
            parentStub,
            fqName,
            name,
            isMutable,
            hasValOrValNode,
            hasDefaultValue,
            dataStream.readNameString(),
        )
    }

    override fun indexStub(stub: CangJieParameterStub, sink: IndexSink) {
        getInstance().indexParameter(stub, sink)
    }
}
