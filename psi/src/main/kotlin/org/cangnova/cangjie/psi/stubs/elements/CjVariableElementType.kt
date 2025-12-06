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

import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import org.cangnova.cangjie.psi.stubs.CangJieVariableStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.deserialize
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.serialize
import org.cangnova.cangjie.psi.stubs.impl.CangJieVariableStubImpl
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import java.io.IOException
import org.cangnova.cangjie.name.*

//        根据单一模式返回所有绑定模式
fun CjCasePattern?.getAllBindings(): List<CjBindingPattern> {
    this ?: return emptyList()
    return when (this) {
        is CjBindingPattern -> listOf(this)
        is CjEnumPattern -> this.patterns.flatMap { it.getAllBindings() }
        is CjTuplePattern -> this.patterns.flatMap { it.getAllBindings() }
        else -> emptyList()
    }
}

class CjVariableElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieVariableStub, CjVariable>(
        debugName,
        CjVariable::class.java,
        CangJieVariableStub::class.java,
    ) {

    override fun createStub(psi: CjVariable, parentStub: StubElement<*>?): CangJieVariableStub {
//        assert !psi.isLocal() :
//                String.format("Should not store local property: %s, parent %s",
//                        psi.getText(), psi.getParent() != null ? psi.getParent().getText() : "<no parent>");

        val childPattern = psi.pattern?.getAllBindings()?.map {
//            CangJieVariableStubImpl(
//                parentStub, StringRef.fromString(it.name),
//                psi.isVar, psi.isTopLevel,
//                psi.hasInitializer(),
//                psi.receiverTypeReference != null, psi.typeReference != null,
//                psi.safeFqNameForLazyResolve(it.name),
//                emptyList(),
//                null
//            )

            CangJieVariableStub.ChildInfo(StringRef.fromString(it.name), psi.safeFqNameForLazyResolve(it.name))
        } ?: emptyList()

        return CangJieVariableStubImpl(
            parentStub, StringRef.fromString(psi.name),
            psi.isVar, psi.isTopLevel,
            psi.hasInitializer(),
            psi.receiverTypeReference != null, psi.typeReference != null,
            psi.safeFqNameForLazyResolve(),
            childPattern,
            null,

        )
    }

    companion object {
        @Throws(IOException::class)
        fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieVariableStub {
            val name = dataStream.readName()
            val isVar = dataStream.readBoolean()
            val isTopLevel = dataStream.readBoolean()
            val hasInitializer = dataStream.readBoolean()
            val hasReceiverTypeRef = dataStream.readBoolean()
            val hasReturnTypeRef = dataStream.readBoolean()

            val fqNameAsString = dataStream.readName()
            val fqName = if (fqNameAsString != null) FqName(fqNameAsString.toString()) else null

            val childSize = dataStream.readInt()
            val childVariableByPattern = mutableListOf<CangJieVariableStub.ChildInfo>()
            for (i in 0 until childSize) {
                childVariableByPattern.add(CangJieVariableStub.ChildInfo.deserialize(dataStream))
            }

            return CangJieVariableStubImpl(
                parentStub, name, isVar, isTopLevel, hasInitializer,
                hasReceiverTypeRef, hasReturnTypeRef, fqName,
                childVariableByPattern,
                deserialize(dataStream),
            )
        }

        fun serialize(stub: CangJieVariableStub, dataStream: StubOutputStream) {
            dataStream.writeName(stub.name)
            dataStream.writeBoolean(stub.isVar())
            dataStream.writeBoolean(stub.isTopLevel())

            dataStream.writeBoolean(stub.hasInitializer())
            dataStream.writeBoolean(stub.isExtension())
            dataStream.writeBoolean(stub.hasReturnTypeRef())

            val fqName = stub.getFqName()
            dataStream.writeName(fqName?.asString())

            dataStream.writeInt(stub.childNamesByPattern.size)

            stub.childNamesByPattern.forEach {
                it.serialize(dataStream)
            }

            if (stub is CangJieVariableStubImpl) {
                serialize(stub.origin, dataStream)
            }
        }
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieVariableStub, dataStream: StubOutputStream) {
        Companion.serialize(stub, dataStream)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieVariableStub {
        val stub = Companion.deserialize(dataStream, parentStub)

        return stub
    }

    override fun indexStub(stub: CangJieVariableStub, sink: IndexSink) {
        getInstance().indexVariable(stub, sink)
    }
}
