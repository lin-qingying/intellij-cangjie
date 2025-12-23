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
import org.cangnova.cangjie.name.*

import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjNamedDeclaration
import org.cangnova.cangjie.psi.psiUtil.StubUtils.createNestedClassId
import org.cangnova.cangjie.psi.psiUtil.StubUtils.deserializeClassId
import org.cangnova.cangjie.psi.psiUtil.StubUtils.serializeClassId
import org.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import org.cangnova.cangjie.psi.psiUtil.getStrictParentOfType
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
        // 获取枚举条目的完全限定名
        val fqName: FqName? = (psi as CjNamedDeclaration).safeFqNameForLazyResolve()

        // 获取父枚举的完全限定名
        val parentEnum = psi.getStrictParentOfType<CjEnum>()
        val parentEnumFqName = parentEnum?.safeFqNameForLazyResolve()

        // 枚举条目不是类，classId 使用父枚举的 classId
        val classId = parentEnum?.let { createNestedClassId(parentStub!!, it) }

        // 提取参数信息
        val typeReferences = psi.typeReferences
        val parameterCount = typeReferences.size
        val parameterTypeNames = typeReferences.mapNotNull { it.text }

        // 判断是否为本地枚举条目
        val isLocal = parentEnum?.isLocal ?: false

        return CangJieEnumConstructorStubImpl(
            getStubType(),
            parentStub as StubElement<*>?,
            StringRef.fromString(fqName?.asString()),
            StringRef.fromString(parentEnumFqName?.asString()),
            classId,
            StringRef.fromString(psi.name),
            parameterCount,
            parameterTypeNames,
            isLocal,
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieEnumConstructorStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)

        val fqName = stub.getFqName()
        val parentEnumFqName = stub.getParentEnumFqName()

        dataStream.writeName(fqName?.asString())
        dataStream.writeName(parentEnumFqName?.asString())

        serializeClassId(dataStream, stub.getClassId())

        dataStream.writeBoolean(stub.isLocal())

        // 序列化参数信息
        dataStream.writeVarInt(stub.getParameterCount())
        val paramTypeNames = stub.getParameterTypeNames()
        dataStream.writeVarInt(paramTypeNames.size)
        for (typeName in paramTypeNames) {
            dataStream.writeName(typeName)
        }
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieEnumConstructorStub {
        val name = dataStream.readName()
        val fqName = dataStream.readName()
        val parentEnumFqName = dataStream.readName()

        val classId = deserializeClassId(dataStream)

        val isLocal = try {
            dataStream.readBoolean()
        } catch (e: IOException) {
            false
        }

        // 反序列化参数信息
        val parameterCount = try {
            dataStream.readVarInt()
        } catch (e: IOException) {
            0
        }

        val parameterTypeNames = try {
            val count = dataStream.readVarInt()
            (0 until count).map {
                StringRef.toString(dataStream.readName()) ?: ""
            }
        } catch (e: IOException) {
            emptyList()
        }

        return CangJieEnumConstructorStubImpl(
            getStubType(),
            parentStub,
            fqName,
            parentEnumFqName,
            classId,
            name,
            parameterCount,
            parameterTypeNames,
            isLocal,
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
