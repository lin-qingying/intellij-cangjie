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

import org.cangnova.cangjie.builtins.StandardNames.MAIN
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.CjMainFunction
import org.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import org.cangnova.cangjie.psi.stubs.CangJieMainFunctionStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieMainFunctionStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.deserialize
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.serialize
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import java.io.IOException

class CjMainFunctionElementType : CjStubElementType<CangJieMainFunctionStub, CjMainFunction> {
    constructor(debugName: String, psiClass: Class<CjMainFunction>, stubClass: Class<*>) : super(
        debugName,
        psiClass,
        stubClass,
    )

    constructor(debugName: String) : super(debugName, CjMainFunction::class.java, CangJieMainFunctionStub::class.java)

    override fun createStub(psi: CjMainFunction, parentStub: StubElement<out PsiElement>): CangJieMainFunctionStubImpl {
        var fqName = psi.safeFqNameForLazyResolve()
        if (fqName != null) {
            val firstSegment = fqName.firstSegment()
            if (firstSegment != null) {
                fqName = FqName(firstSegment.asString()).child(MAIN)
            }
        }
        return CangJieMainFunctionStubImpl(
            parentStub, CjStubElementTypes.MAIN_FUNC, StringRef.fromString(psi.name), fqName,
            null,
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieMainFunctionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)

        val fqName = stub.getFqName()
        dataStream.writeName(fqName?.asString())

        if (stub is CangJieMainFunctionStubImpl) {
            serialize(stub.origin, dataStream)
        }
    }

    override fun indexStub(stub: CangJieMainFunctionStub, sink: IndexSink) {
        getInstance().indexMainFunction(stub, sink)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieMainFunctionStubImpl {
        val name = dataStream.readName()

        val fqNameAsString = dataStream.readName()
        val fqName = if (fqNameAsString != null) FqName(fqNameAsString.toString()) else null

        return CangJieMainFunctionStubImpl(
            parentStub, CjStubElementTypes.MAIN_FUNC, name, fqName,
            deserialize(dataStream),
        )
    }

    companion object {
        private const val NAME = "cangjie.MAIN_FUNCTION"
    }
}
