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
package com.linqingying.cangjie.psi.stubs.elements

import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.psi.CjNameBasicReferenceExpression
import com.linqingying.cangjie.psi.CjNameReferenceExpression
import com.linqingying.cangjie.psi.stubs.CangJieNameBasicReferenceExpressionStub
import com.linqingying.cangjie.psi.stubs.CangJieNameReferenceExpressionStub
import com.linqingying.cangjie.psi.stubs.impl.CangJieNameBasicReferenceExpressionStubImpl
import com.linqingying.cangjie.psi.stubs.impl.CangJieNameReferenceExpressionStubImpl
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjNameReferenceExpressionElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieNameReferenceExpressionStub, CjNameReferenceExpression>(
        debugName,
        CjNameReferenceExpression::class.java,
        CangJieNameReferenceExpressionStub::class.java
    ) {
    override fun createStub(
        psi: CjNameReferenceExpression,
        parentStub: StubElement<*>?
    ): CangJieNameReferenceExpressionStub {
        return CangJieNameReferenceExpressionStubImpl(parentStub, StringRef.fromString(psi.referencedName))
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieNameReferenceExpressionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getReferencedName())
        dataStream.writeBoolean(
            stub is CangJieNameReferenceExpressionStubImpl && stub.isClassRef
        )
    }

    @Throws(IOException::class)
    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>
    ): CangJieNameReferenceExpressionStub {
        val referencedName = dataStream.readName()
        val isClassRef = dataStream.readBoolean()
        return CangJieNameReferenceExpressionStubImpl(parentStub, referencedName!!, isClassRef)
    }
}

class CjNameBasicReferenceExpressionElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieNameBasicReferenceExpressionStub, CjNameBasicReferenceExpression>(
        debugName,
        CjNameBasicReferenceExpression::class.java,
        CangJieNameBasicReferenceExpressionStub::class.java
    ) {
    override fun createStub(
        psi: CjNameBasicReferenceExpression,
        parentStub: StubElement<*>?
    ): CangJieNameBasicReferenceExpressionStub {
        return CangJieNameBasicReferenceExpressionStubImpl(parentStub, StringRef.fromString(psi.referencedName))
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieNameBasicReferenceExpressionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getReferencedName())
        dataStream.writeBoolean(
            stub is CangJieNameBasicReferenceExpressionStubImpl && stub.isClassRef
        )
    }

    @Throws(IOException::class)
    override fun deserialize(
        dataStream: StubInputStream,
        parentStub: StubElement<*>
    ): CangJieNameBasicReferenceExpressionStub {
        val referencedName = dataStream.readName()
        val isClassRef = dataStream.readBoolean()
        return CangJieNameBasicReferenceExpressionStubImpl(parentStub, referencedName!!, isClassRef)
    }
}
