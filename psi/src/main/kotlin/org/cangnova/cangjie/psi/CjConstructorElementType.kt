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

package org.cangnova.cangjie.psi

import org.cangnova.cangjie.psi.stubs.CangJieConstructorStub
import org.cangnova.cangjie.psi.stubs.elements.CjStubElementType
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import java.io.IOException

abstract class CjConstructorElementType<T : CjConstructor<T>>(
    @NonNls debugName: String,
    tClass: Class<T>,
    stubClass: Class<CangJieConstructorStub<*>>,
) : CjStubElementType<CangJieConstructorStub<T>, T>(debugName, tClass, stubClass) {
    protected abstract fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isPrimary: Boolean,
        isDelegatedCallToThis: Boolean,
    ): CangJieConstructorStub<T>

    open val isPrimary: Boolean get() = false

    protected abstract fun isDelegatedCallToThis(constructor: T): Boolean

    override fun createStub(psi: T, parentStub: StubElement<*>): CangJieConstructorStub<T> {
        val hasBody = psi.hasBody()
        val isDelegatedCallToThis = isDelegatedCallToThis(psi)
        return newStub(
            parentStub,
            StringRef.fromString(psi.name),
            hasBody,
            isPrimary,
            isDelegatedCallToThis
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieConstructorStub<T>, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeBoolean(stub.hasBody())
        dataStream.writeBoolean(stub.isPrimary)
        dataStream.writeBoolean(stub.isDelegatedCallToThis())
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieConstructorStub<T> {
        val name = dataStream.readName()
        val hasBody = dataStream.readBoolean()
        val isPrimary = dataStream.readBoolean()
        val isDelegatedCallToThis = dataStream.readBoolean()
        return newStub(parentStub, name, hasBody, isPrimary, isDelegatedCallToThis)
    }

    override fun indexStub(stub: CangJieConstructorStub<T>, sink: IndexSink) {
    }
}
