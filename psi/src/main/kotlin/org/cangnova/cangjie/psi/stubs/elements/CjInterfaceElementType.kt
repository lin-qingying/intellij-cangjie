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

import org.cangnova.cangjie.psi.CjInterface
import org.cangnova.cangjie.psi.psiUtil.StubUtils.createNestedClassId
import org.cangnova.cangjie.psi.psiUtil.StubUtils.deserializeClassId
import org.cangnova.cangjie.psi.psiUtil.StubUtils.serializeClassId
import org.cangnova.cangjie.psi.psiUtil.getSuperNames
import org.cangnova.cangjie.psi.psiUtil.safeFqNameForLazyResolve
import org.cangnova.cangjie.psi.stubs.CangJieInterfaceStub
import org.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import org.cangnova.cangjie.psi.stubs.impl.CangJieInterfaceStubImpl
import org.cangnova.cangjie.psi.stubs.impl.Utils.wrapStrings
import com.intellij.lang.ASTNode
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjInterfaceElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieInterfaceStub, CjInterface>(
        debugName,
        CjInterface::class.java,
        CangJieInterfaceStub::class.java,
    ) {
    override fun indexStub(stub: CangJieInterfaceStub, sink: IndexSink) {
        getInstance().indexInterface(stub, sink)
    }

    override fun createPsi(stub: CangJieInterfaceStub): CjInterface {
        return CjInterface(stub)
    }

    override fun createPsiFromAst(node: ASTNode): CjInterface {
        return CjInterface(node)
    }

    override fun createStub(psi: CjInterface, parentStub: StubElement<out PsiElement?>): CangJieInterfaceStub {
        val fqName = psi.safeFqNameForLazyResolve()

        val superNames = psi.getSuperNames()
        val classId = createNestedClassId(parentStub, psi)
        return CangJieInterfaceStubImpl(
            CjStubElementTypes.INTERFACE,
            parentStub,
            StringRef.fromString(fqName?.asString()),
            classId,
            StringRef.fromString(psi.name),
            wrapStrings(superNames),
            psi.isLocal,
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieInterfaceStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.name)

        val fqName = stub.getFqName()
        dataStream.writeName(fqName?.asString())

        serializeClassId(dataStream, stub.getClassId())

        dataStream.writeBoolean(stub.isLocal())

        //        dataStream.writeBoolean(stub.isTopLevel());
        val superNames = stub.getSuperNames()
        dataStream.writeVarInt(superNames.size)
        for (name in superNames) {
            dataStream.writeName(name)
        }
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieInterfaceStub {
        val name = dataStream.readName()
        val qualifiedName = dataStream.readName()

        val classId = deserializeClassId(dataStream)

        val isLocal = dataStream.readBoolean()

        //        bool isTopLevel = dataStream.readBoolean();
        val superCount = dataStream.readVarInt()
        val superNames = StringRef.createArray(superCount)
        for (i in 0..<superCount) {
            superNames[i] = dataStream.readName()
        }

        return CangJieInterfaceStubImpl(
            CjStubElementTypes.INTERFACE,
            parentStub,
            qualifiedName,
            classId,
            name,
            superNames,
            isLocal,
        )
    }

    companion object {
        val stubType: CjInterfaceElementType
            get() = CjStubElementTypes.INTERFACE
    }
}
