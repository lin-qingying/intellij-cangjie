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

import org.cangnova.cangjie.psi.CjCatchParameter
import org.cangnova.cangjie.psi.stubs.CangJieCatchParameterStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieCatchParameterStubImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

class CjCatchParameterElementType(debugName: String) : CjStubElementType<CangJieCatchParameterStub, CjCatchParameter>(

    debugName,
    CjCatchParameter::class.java,
    CangJieCatchParameterStub::class.java,
) {
    override fun serialize(stub: CangJieCatchParameterStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        val name = stub.getFqName()
        dataStream.writeName(name?.asString())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieCatchParameterStub {
        val name = dataStream.readName()

        val fqName = dataStream.readName()

        return CangJieCatchParameterStubImpl(fqName, name, parentStub)
    }

    override fun indexStub(stub: CangJieCatchParameterStub, sink: IndexSink) {
        getInstance().indexParameter(stub, sink)
    }

    override fun createStub(
        psi: CjCatchParameter,
        parentStub: StubElement<out PsiElement>?,
    ): CangJieCatchParameterStub {
        val fqName = psi.fqName
        val fqNameRef = StringRef.fromString(fqName?.asString())
        return CangJieCatchParameterStubImpl(
            fqNameRef,
            StringRef.fromString(psi.name),
            parentStub,

        )
    }
}
