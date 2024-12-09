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
package com.linqingying.cangjie.psi.stubs.elements

import com.intellij.psi.stubs.IndexSink
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.descriptors.DescriptorVisibilities
import com.linqingying.cangjie.descriptors.DescriptorVisibility
import com.linqingying.cangjie.psi.CjImportDirective
import com.linqingying.cangjie.psi.CjMultiImportDirective
import com.linqingying.cangjie.psi.stubs.CangJieImportDirectiveStub
import com.linqingying.cangjie.psi.stubs.CangJieMultiImportDirectiveStub
import com.linqingying.cangjie.psi.stubs.elements.StubIndexService.Companion.getInstance
import com.linqingying.cangjie.psi.stubs.impl.CangJieImportDirectiveStubImpl
import com.linqingying.cangjie.psi.stubs.impl.CangJieMulitImportDirectiveStubImpl
import org.jetbrains.annotations.NonNls
import java.io.IOException


class CjImportDirectiveElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieImportDirectiveStub, CjImportDirective>(
        debugName,
        CjImportDirective::class.java,
        CangJieImportDirectiveStub::class.java
    ) {
    override fun createStub(psi: CjImportDirective, parentStub: StubElement<*>?): CangJieImportDirectiveStub {
        val importedFqName = psi.importedFqName
        val fqName = StringRef.fromString(importedFqName?.asString())
        return CangJieImportDirectiveStubImpl(
            parentStub!!,
            psi.isAllUnder,
            fqName,
            psi.isValidImport,
            psi.modifierVisibility
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieImportDirectiveStub, dataStream: StubOutputStream) {
        dataStream.writeBoolean(stub.isAllUnder())
        val importedFqName = stub.getImportedFqName()
        dataStream.writeName(importedFqName?.asString())
        dataStream.writeBoolean(stub.isValid())
        dataStream.writeName(stub.getModifierVisibility().name)
    }

    override fun indexStub(stub: CangJieImportDirectiveStub, sink: IndexSink) {
        getInstance().indexImports(stub, sink)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieImportDirectiveStub {
        val isAllUnder = dataStream.readBoolean()
        val importedName = dataStream.readName()
        val isValid = dataStream.readBoolean()
        val modifierVisibility = dataStream.readName()
        val visibility: DescriptorVisibility? = if (modifierVisibility != null) {
            DescriptorVisibilities.formName(modifierVisibility.string)
        } else {
            DescriptorVisibilities.PRIVATE
        }
        return CangJieImportDirectiveStubImpl(
            parentStub, isAllUnder, importedName, isValid,
            visibility!!
        )
    }
}

class CjMultiImportDirectiveElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieMultiImportDirectiveStub, CjMultiImportDirective>(
        debugName,
        CjMultiImportDirective::class.java,
        CangJieMultiImportDirectiveStub::class.java
    ) {
    override fun createStub(psi: CjMultiImportDirective, parentStub: StubElement<*>): CangJieMultiImportDirectiveStub {

        return CangJieMulitImportDirectiveStubImpl(
            parentStub,

            psi.modifierVisibility
        )
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieMultiImportDirectiveStub, dataStream: StubOutputStream) {


        dataStream.writeName(stub.getModifierVisibility().name)
    }

    override fun indexStub(stub: CangJieMultiImportDirectiveStub, sink: IndexSink) {
        getInstance().indexImports(stub, sink)
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieMultiImportDirectiveStub {

        val modifierVisibility = dataStream.readName()
        val visibility: DescriptorVisibility = if (modifierVisibility != null) {
            DescriptorVisibilities.formName(modifierVisibility.string)
        } else {
            DescriptorVisibilities.PRIVATE
        }
        return CangJieMulitImportDirectiveStubImpl(
            parentStub,
            visibility
        )
    }
}
