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

package org.cangnova.cangjie.psi.stubs.elements

import org.cangnova.cangjie.psi.CjAnnotationEntry
import org.cangnova.cangjie.psi.stubs.CangJieAnnotationEntryStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieAnnotationEntryStubImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjAnnotationEntryElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieAnnotationEntryStub, CjAnnotationEntry>(
        debugName,
        CjAnnotationEntry::class.java,
        CangJieAnnotationEntryStub::class.java,
    ) {
    override fun createStub(
        psi: CjAnnotationEntry,
        parentStub: StubElement<out PsiElement?>,
    ): CangJieAnnotationEntryStub {
        val shortName = psi.shortName
        val resultName = shortName?.asString()
        val valueArgumentList = psi.valueArgumentList
        val hasValueArguments = valueArgumentList != null && !valueArgumentList.arguments.isEmpty()
        return CangJieAnnotationEntryStubImpl(parentStub, StringRef.fromString(resultName), hasValueArguments)
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieAnnotationEntryStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getShortName())
        dataStream.writeBoolean(stub.hasValueArguments())
        if (stub is CangJieAnnotationEntryStubImpl) {
//            Map<Name, ConstantValue<?>> arguments = ((CangJieAnnotationEntryStubImpl) stub).getValueArguments();
//            dataStream.writeInt(arguments != null ? arguments.size() : 0);
//            if (arguments != null) {
//                for (Map.Entry<Name, ConstantValue<?>> valueEntry : arguments.entrySet()) {
//                    dataStream.writeName(valueEntry.getKey().asString());
//                    ConstantValue<?> value = valueEntry.getValue();
//                    CangJieConstantValueKt.serialize(value, dataStream);
//                }
//            }
        }
    }

    @Throws(IOException::class)
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieAnnotationEntryStub {
        val text = dataStream.readName()
        val hasValueArguments = dataStream.readBoolean()
        //        int valueArgCount = dataStream.readInt();
//        Map<Name, ConstantValue<?>> args = new LinkedHashMap<>();
//        for (int i = 0; i < valueArgCount; i++) {
//            args.put(Name.identifier(Objects.requireNonNull(dataStream.readNameString())),
//                    CangJieConstantValueKt.createConstantValue(dataStream));
//        }
        return CangJieAnnotationEntryStubImpl(parentStub, text, hasValueArguments)
    }
}
