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

import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.psiUtil.StubUtils.createNestedClassId
import org.cangnova.cangjie.psi.psiUtil.StubUtils.deserializeClassId
import org.cangnova.cangjie.psi.psiUtil.StubUtils.serializeClassId
import org.cangnova.cangjie.psi.psiUtil.getSuperNames
import org.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import org.cangnova.cangjie.psi.stubs.CangJieEnumStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieEnumStubImpl
import org.cangnova.cangjie.psi.stubs.impl.Utils.wrapStrings
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

class CjEnumElementType(debugName: String) : CjStubElementType<CangJieEnumStub, CjEnum>(
    debugName,
    CjEnum::class.java,
    CangJieEnumStub::class.java,
) {
    override fun serialize(stub: CangJieEnumStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)

        val fqName = stub.getFqName()
        dataStream.writeName(fqName?.asString())

        serializeClassId(dataStream, stub.getClassId())

        dataStream.writeBoolean(stub.isLocal())
        dataStream.writeBoolean(stub.isNonExhaustive())
//        dataStream.writeBoolean(stub.isTopLevel())

        val superNames = stub.getSuperNames()
        dataStream.writeVarInt(superNames.size)
        for (name in superNames) {
            dataStream.writeName(name)
        }
    }
    companion object {
        fun getStubType(): CjEnumElementType {
            return CjStubElementTypes.ENUM
        }
    }
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieEnumStub {
        val name = dataStream.readName()
        val qualifiedName = dataStream.readName()

        val classId = deserializeClassId(dataStream)


        val isLocal = dataStream.readBoolean()
 val isNonExhaustive = dataStream.readBoolean()
        val superCount = dataStream.readVarInt()
        val superNames = StringRef.createArray(superCount)
        for (i in 0 until superCount) {
            superNames[i] = dataStream.readName()
        }

        return CangJieEnumStubImpl(
            CjStubElementTypes.ENUM,
            parentStub,
            qualifiedName,
            classId,
            name,
            superNames,
            isLocal,
            isNonExhaustive
        )
    }

    override fun createStub(psi: CjEnum, parentStub: StubElement<out PsiElement>?): CangJieEnumStub {
        val fqName = psi.safeFqNameForLazyResolve()

        val superNames = psi.getSuperNames()
        val classId = createNestedClassId(parentStub!!, psi)
        val isNonExhaustive = psi.isNonExhaustive
        return CangJieEnumStubImpl(
            CjStubElementTypes.ENUM,
            parentStub as StubElement<*>?,
            StringRef.fromString(fqName?.asString()),
            classId,
            StringRef.fromString(psi.name),
            wrapStrings(superNames),
            psi.isLocal,
            isNonExhaustive
        )
    }

    override fun createPsi(stub: CangJieEnumStub): CjEnum {
        return CjEnum(stub)
    }

    override fun indexStub(stub: CangJieEnumStub, sink: IndexSink) {
        getInstance().indexEnum(stub, sink)
    }

    override fun createPsiFromAst(node: ASTNode): CjEnum {
        return CjEnum(node)
    }
}
