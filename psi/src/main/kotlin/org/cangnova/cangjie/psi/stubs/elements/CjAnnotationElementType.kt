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

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.cangnova.cangjie.psi.CjAnnotation
import org.cangnova.cangjie.psi.stubs.CangJieAnnotationStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieAnnotationStubImpl
import org.jetbrains.annotations.NonNls
import java.io.IOException

class CjAnnotationElementType(debugName: @NonNls String) :
    CjStubElementType<CangJieAnnotationStub, CjAnnotation>(
        debugName,
        CjAnnotation::class.java,
        CangJieAnnotationStub::class.java,
    ) {
    override fun createStub(
        psi: CjAnnotation,
        parentStub: StubElement<out PsiElement?>,
    ): CangJieAnnotationStub {
        val shortName = psi.shortName
        val resultName = shortName?.asString()
        val valueArgumentList = psi.valueArgumentList
        val hasValueArguments = valueArgumentList != null && !valueArgumentList.arguments.isEmpty()
        return CangJieAnnotationStubImpl(parentStub, StringRef.fromString(resultName), hasValueArguments)
    }

    @Throws(IOException::class)
    override fun serialize(stub: CangJieAnnotationStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getShortName())
        dataStream.writeBoolean(stub.hasValueArguments())
        if (stub is CangJieAnnotationStubImpl) {
//            Map<Name, ConstantValue<?>> arguments = ((CangJieAnnotationStubImpl) stub).getValueArguments();
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
    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>): CangJieAnnotationStub {
        val text = dataStream.readName()
        val hasValueArguments = dataStream.readBoolean()
        //        int valueArgCount = dataStream.readInt();
//        Map<Name, ConstantValue<?>> args = new LinkedHashMap<>();
//        for (int i = 0; i < valueArgCount; i++) {
//            args.put(Name.identifier(Objects.requireNonNull(dataStream.readNameString())),
//                    CangJieConstantValueKt.createConstantValue(dataStream));
//        }
        return CangJieAnnotationStubImpl(parentStub, text, hasValueArguments)
    }
}
