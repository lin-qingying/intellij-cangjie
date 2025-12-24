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

import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.name.FqName

import org.cangnova.cangjie.psi.CjFieldVariable
import org.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import org.cangnova.cangjie.psi.stubs.CangJieFieldStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieFieldStubImpl
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.deserialize
import org.cangnova.cangjie.psi.stubs.impl.CangJieStubOrigin.Companion.serialize
import org.jetbrains.annotations.NonNls
import java.io.IOException

/**
 * 类成员字段的 Stub 元素类型
 *
 * 处理 class/struct/interface 中的 let/var/const 声明的序列化和反序列化
 */
class CjFieldElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieFieldStub, CjFieldVariable>(
        debugName,
        CjFieldVariable::class.java,
        CangJieFieldStub::class.java,
    ) {

    override fun createStub(psi: CjFieldVariable, parentStub: StubElement<out PsiElement>?): CangJieFieldStub {
        return CangJieFieldStubImpl(
            parentStub,
            StringRef.fromString(psi.name),
            psi.safeFqNameForLazyResolve(),
            psi.isVar,
            psi.isConst,
            psi.hasInitializer(),
            psi.typeReference != null,
            null,
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieFieldStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeName(stub.getFqName()?.asString())
        dataStream.writeBoolean(stub.isVar())
        dataStream.writeBoolean(stub.isConst())
        dataStream.writeBoolean(stub.hasInitializer())
        dataStream.writeBoolean(stub.hasReturnTypeRef())

        if (stub is CangJieFieldStubImpl) {
            serialize(stub.origin, dataStream)
        }
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieFieldStub {
        val name = dataStream.readName()
        val fqNameAsString = dataStream.readName()
        val fqName = if (fqNameAsString != null) FqName(fqNameAsString.toString()) else null
        val isVar = dataStream.readBoolean()
        val isConst = dataStream.readBoolean()
        val hasInitializer = dataStream.readBoolean()
        val hasReturnTypeRef = dataStream.readBoolean()

        return CangJieFieldStubImpl(
            parentStub,
            name,
            fqName,
            isVar,
            isConst,
            hasInitializer,
            hasReturnTypeRef,
            deserialize(dataStream),
        )
    }

    override fun indexStub(stub: CangJieFieldStub, sink: IndexSink) {
        getInstance().indexField(stub, sink)
    }

    override fun createPsi(stub: CangJieFieldStub): CjFieldVariable {
        return CjFieldVariable(stub)
    }

    override fun createPsiFromAst(node: ASTNode): CjFieldVariable {
        return CjFieldVariable(node)
    }
}
