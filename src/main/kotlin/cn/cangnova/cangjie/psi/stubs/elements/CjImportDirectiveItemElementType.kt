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

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import cn.cangnova.cangjie.descriptors.DescriptorVisibilities
import cn.cangnova.cangjie.descriptors.DescriptorVisibility
import cn.cangnova.cangjie.psi.CjImportDirective
import cn.cangnova.cangjie.psi.CjImportDirectiveItem

import cn.cangnova.cangjie.psi.stubs.CangJieImportDirectiveItemStub
import cn.cangnova.cangjie.psi.stubs.CangJieImportDirectiveStub

import cn.cangnova.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import cn.cangnova.cangjie.psi.stubs.impl.CangJieImportDirectiveItemStubImpl
import cn.cangnova.cangjie.psi.stubs.impl.CangJieImportDirectiveStubImpl

import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjImportDirectiveElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieImportDirectiveStub, CjImportDirective>(
        debugName,
        CjImportDirective::class.java,
        CangJieImportDirectiveStub::class.java
    ) {
    override fun createStub(psi: CjImportDirective, parentStub: StubElement<*>?): CangJieImportDirectiveStub {


        return CangJieImportDirectiveStubImpl(
            parentStub!!,

            psi.modifierVisibility
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieImportDirectiveStub, dataStream: StubOutputStream) {

        dataStream.writeName(stub.getModifierVisibility().name)
    }

    override fun indexStub(stub: CangJieImportDirectiveStub, sink: IndexSink) {
        getInstance().indexImports(stub, sink)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieImportDirectiveStub {

        val modifierVisibility = dataStream.readName()
        val visibility: DescriptorVisibility? = if (modifierVisibility != null) {
            DescriptorVisibilities.formName(modifierVisibility.string)
        } else {
            DescriptorVisibilities.PRIVATE
        }
        return CangJieImportDirectiveStubImpl(
            parentStub,
            visibility!!
        )
    }
}


class CjImportDirectiveItemElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieImportDirectiveItemStub, CjImportDirectiveItem>(
        debugName,
        CjImportDirectiveItem::class.java,
        CangJieImportDirectiveItemStub::class.java
    ) {
    override fun createStub(psi: CjImportDirectiveItem, parentStub: StubElement<*>?): CangJieImportDirectiveItemStub {
        val importedFqName = psi.importedFqName
        val fqName = StringRef.fromString(importedFqName?.asString())
        return CangJieImportDirectiveItemStubImpl(
            parentStub!!,
            psi.isAllUnder,
            fqName,
            psi.isValidImport,
            psi.modifierVisibility
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieImportDirectiveItemStub, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isAllUnder())
        val importedFqName = stub.getImportedFqName()
        dataStream.writeName(importedFqName?.asString())
        dataStream.writeBoolean(stub.isValid())
        dataStream.writeName(stub.getModifierVisibility().name)
    }

    override fun indexStub(stub: CangJieImportDirectiveItemStub, sink: IndexSink) {
        getInstance().indexImports(stub, sink)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieImportDirectiveItemStub {
        val isAllUnder = dataStream.readBoolean()
        val importedName = dataStream.readName()
        val isValid = dataStream.readBoolean()
        val modifierVisibility = dataStream.readName()
        val visibility: DescriptorVisibility? = if (modifierVisibility != null) {
            DescriptorVisibilities.formName(modifierVisibility.string)
        } else {
            DescriptorVisibilities.PRIVATE
        }
        return CangJieImportDirectiveItemStubImpl(
            parentStub, isAllUnder, importedName, isValid,
            visibility!!
        )
    }
}
