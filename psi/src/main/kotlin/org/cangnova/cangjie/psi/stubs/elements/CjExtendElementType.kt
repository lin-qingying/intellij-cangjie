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

import org.cangnova.cangjie.psi.CjExtend
import org.cangnova.cangjie.psi.psiUtil.StubUtils
import org.cangnova.cangjie.psi.psiUtil.getSuperNames
import org.cangnova.cangjie.psi.stubs.CangJieExtendStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieExtendStubImpl
import org.cangnova.cangjie.psi.stubs.impl.Utils
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

class CjExtendElementType(debugName: String) : CjStubElementType<CangJieExtendStub, CjExtend>(
    debugName,
    CjExtend::class.java,
    CangJieExtendStub::class.java,
) {

    override fun indexStub(stub: CangJieExtendStub, sink: IndexSink) {
        getInstance().indexExtend(stub, sink)
    }

    override fun serialize(stub: CangJieExtendStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)

        val fqName = stub.getFqName()
        dataStream.writeName(fqName?.asString())

        StubUtils.serializeClassId(dataStream, stub.getClassId())
        // 序列化被扩展类型名称
        dataStream.writeName(stub.receiverTypeName)
        val superNames = stub.getSuperNames()
        dataStream.writeVarInt(superNames.size)
        for (name in superNames) {
            dataStream.writeName(name)
        }

    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieExtendStub {
        val name = dataStream.readName()
        val qualifiedName = dataStream.readName()

        val classId = StubUtils.deserializeClassId(dataStream)
        // 反序列化被扩展类型名称
        val receiverTypeName = dataStream.readName()
        val superCount = dataStream.readVarInt()
        val superNames = StringRef.createArray(superCount)
        for (i in 0 until superCount) {
            superNames[i] = dataStream.readName()
        }



        return CangJieExtendStubImpl(
            CjStubElementTypes.EXTEND,
            parentStub,
            qualifiedName,
            classId,
            name,
            superNames,
            receiverTypeName = StringRef.toString(receiverTypeName) ?: ""
        )
    }

    override fun createStub(psi: CjExtend, parentStub: StubElement<out PsiElement>?): CangJieExtendStub {
        // 获取被扩展类型的名称
        val receiverTypeName = psi.nameAsName.asString()

        val fqName = psi.fqName

        val superNames = psi.getSuperNames()
        val classId = StubUtils.createNestedClassId(parentStub!!, psi)
        return CangJieExtendStubImpl(
            CjStubElementTypes.EXTEND,
            parentStub as StubElement<*>?,
            StringRef.fromString(fqName?.asString()),
            classId,
            StringRef.fromString(psi.name),
            Utils.wrapStrings(superNames),
            receiverTypeName = receiverTypeName
        )
    }

    override fun createPsi(stub: CangJieExtendStub): CjExtend {
        return CjExtend(stub)
    }

    override fun createPsiFromAst(node: ASTNode): CjExtend {
        return CjExtend(node)
    }
}
