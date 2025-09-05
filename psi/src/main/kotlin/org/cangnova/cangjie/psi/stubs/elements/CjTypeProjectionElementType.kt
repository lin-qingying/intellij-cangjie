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

import org.cangnova.cangjie.psi.CjTypeProjection
import org.cangnova.cangjie.psi.stubs.CangJieTypeProjectionStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieTypeProjectionStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjTypeProjectionElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieTypeProjectionStub, CjTypeProjection>(
        debugName,
        CjTypeProjection::class.java,
        CangJieTypeProjectionStub::class.java,
    ) {
    override fun createStub(psi: CjTypeProjection, parentStub: StubElement<*>?): CangJieTypeProjectionStub {
        return CangJieTypeProjectionStubImpl(parentStub, psi.projectionKind.ordinal)
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieTypeProjectionStub, dataStream: StubOutputStream) {
        dataStream.writeVarInt(stub.getProjectionKind().ordinal)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieTypeProjectionStub {
        val projectionKindOrdinal = dataStream.readVarInt()
        return CangJieTypeProjectionStubImpl(parentStub, projectionKindOrdinal)
    }
}
