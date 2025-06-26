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

import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.psi.CjEnumEntry
import cn.cangnova.cangjie.psi.CjNamedDeclaration
import cn.cangnova.cangjie.psi.psiUtil.StubUtils.createNestedClassId
import cn.cangnova.cangjie.psi.psiUtil.StubUtils.deserializeClassId
import cn.cangnova.cangjie.psi.psiUtil.StubUtils.serializeClassId
import cn.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import cn.cangnova.cangjie.psi.stubs.CangJieEnumEntryStub
import cn.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import cn.cangnova.cangjie.psi.stubs.impl.CangJieEnumEntryStubImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import java.io.IOException

class CjEnumEntryElementType(debugName: String) : CjStubElementType<CangJieEnumEntryStub, CjEnumEntry>(
    debugName,
    CjEnumEntry::class.java,
    CangJieEnumEntryStub::class.java,
) {

    override fun createPsi(stub: CangJieEnumEntryStub): CjEnumEntry {
        return CjEnumEntry(stub)
    }

    override fun createPsiFromAst(node: ASTNode): CjEnumEntry {
        return CjEnumEntry(node)
    }

    override fun createStub(psi: CjEnumEntry, parentStub: StubElement<*>?): CangJieEnumEntryStub {
        val fqNameByParent: FqName? = (psi as CjNamedDeclaration).safeFqNameForLazyResolve() // psi.safeFqNameForLazyResolveByParent()

        val fqNameByPackage = psi.safeFqNameForLazyResolve()
        val classId = createNestedClassId(parentStub!!, psi)
        return CangJieEnumEntryStubImpl(
            getStubType(),
            parentStub as StubElement<*>?,
            StringRef.fromString(fqNameByParent?.asString()),
            StringRef.fromString(fqNameByPackage?.asString()),
            classId,
            StringRef.fromString(psi.name),

            psi.isLocal,
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieEnumEntryStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)

        val fqNameByParent = stub.getFqName()
        val fqNameByPacage = stub.fqNameByPackage

        dataStream.writeName(fqNameByParent?.asString())
        dataStream.writeName(fqNameByPacage?.asString())

        serializeClassId(dataStream, stub.getClassId())

        dataStream.writeBoolean(stub.isLocal())

//        val superNames = stub.getSuperNames()
//        dataStream.writeVarInt(superNames.size)
//        for (name in superNames) {
//            dataStream.writeName(name)
//        }
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieEnumEntryStub {
        val name = dataStream.readName()
        val qualifiedNameByParent = dataStream.readName()
        val qualifiedNameByPackage = dataStream.readName()

        val classId = deserializeClassId(dataStream)

        val isLocal = try {
            dataStream.readBoolean()
        } catch (e: IOException) {
            false
        }

        return CangJieEnumEntryStubImpl(
            getStubType(),
            parentStub,
            qualifiedNameByParent,
            qualifiedNameByPackage,
            classId,
            name,
            isLocal,
        )
    }

    override fun indexStub(stub: CangJieEnumEntryStub, sink: IndexSink) {
        getInstance().indexEnumEntry(stub, sink)
    }
    companion object {
        fun getStubType(): CjEnumEntryElementType {
            return CjStubElementTypes.ENUM_ENTRY
        }
    }
}
