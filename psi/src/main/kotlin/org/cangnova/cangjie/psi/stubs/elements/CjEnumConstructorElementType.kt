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

import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.stubs.CangJieEnumConstructorStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieEnumConstructorStubImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import java.io.IOException

class CjEnumConstructorElementType(debugName: String) : CjStubElementType<CangJieEnumConstructorStub, CjEnumConstructor>(
    debugName,
    CjEnumConstructor::class.java,
    CangJieEnumConstructorStub::class.java,
) {

    override fun createPsi(stub: CangJieEnumConstructorStub): CjEnumConstructor {
        return CjEnumConstructor(stub)
    }

    override fun createPsiFromAst(node: ASTNode): CjEnumConstructor {
        return CjEnumConstructor(node)
    }

    override fun createStub(psi: CjEnumConstructor, parentStub: StubElement<*>?): CangJieEnumConstructorStub {
        // 提取参数类型数量
        val typeCount = psi.typeReferences.size

        // 获取所属枚举的 FqName
        val parentEnum = psi.parentEnum
        val enumFqName = parentEnum?.fqName

        return CangJieEnumConstructorStubImpl(
            getStubType(),
            parentStub as StubElement<*>?,
            StringRef.fromString(psi.name),
            typeCount,
            StringRef.fromString(enumFqName?.asString()),
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieEnumConstructorStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)
        dataStream.writeVarInt(stub.getTypeCount())
        dataStream.writeName(stub.getEnumFqName()?.asString())
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieEnumConstructorStub {
        val name = dataStream.readName()
        val typeCount = dataStream.readVarInt()
        val enumFqName = dataStream.readName()

        return CangJieEnumConstructorStubImpl(
            getStubType(),
            parentStub,
            name,
            typeCount,
            enumFqName,
        )
    }

    override fun indexStub(stub: CangJieEnumConstructorStub, sink: IndexSink) {
        getInstance().indexEnumConstructor(stub, sink)
    }

    companion object {
        fun getStubType(): CjEnumConstructorElementType {
            return CjStubElementTypes.ENUM_CONSTRUCTOR
        }
    }
}
