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

import cn.cangnova.cangjie.builtins.StandardNames.MAIN
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.psi.CjFile
import cn.cangnova.cangjie.psi.CjMainFunction
import cn.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import cn.cangnova.cangjie.psi.stubs.CangJieFunctionStub
import cn.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import cn.cangnova.cangjie.psi.stubs.impl.CangJieFunctionStubImpl
import cn.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.deserialize
import cn.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.serialize
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import java.io.IOException

class CjMainFunctionElementType : CjStubElementType<CangJieFunctionStub, CjMainFunction> {
    constructor(debugName: String, psiClass: Class<CjMainFunction>, stubClass: Class<*>) : super(
        debugName,
        psiClass,
        stubClass,
    )

    constructor(debugName: String) : super(debugName, CjMainFunction::class.java, CangJieFunctionStub::class.java)

    override fun createStub(psi: CjMainFunction, parentStub: StubElement<out PsiElement>): CangJieFunctionStubImpl {
        val isTopLevel = psi.parent is CjFile
        val isExtension = psi.receiverTypeReference != null
        var fqName = psi.safeFqNameForLazyResolve()
        if (fqName != null) {
            fqName = FqName(fqName.moduleName.asString()).child(MAIN)
        }
        val hasBlockBody = psi.hasBlockBody()
        val hasBody = psi.hasBody()
        return CangJieFunctionStubImpl(
            parentStub, CjStubElementTypes.MAIN_FUNC, StringRef.fromString(psi.name), isTopLevel, fqName,
            isExtension, hasBlockBody, hasBody, psi.hasTypeParameterListBeforeFunctionName(),

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
        //        bool haveContract = stub.mayHaveContract();
//        dataStream.writeBoolean(haveContract);
        if (stub is CangJieFunctionStubImpl) {
            serialize(stub.origin, dataStream)
        }
    }

    override fun indexStub(stub: CangJieFunctionStub, sink: IndexSink) {
        getInstance().indexMainFunction(stub, sink)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieFunctionStubImpl {
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
            parentStub, CjStubElementTypes.MAIN_FUNC, name, isTopLevel, fqName, isExtension, hasBlockBody, hasBody,
            hasTypeParameterListBeforeFunctionName,
            deserialize(dataStream),
        )
    }

    companion object {
        private const val NAME = "cangjie.MAIN_FUNCTION"
    }
}
