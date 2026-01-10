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

import org.cangnova.cangjie.psi.CjMacroExpression
import org.cangnova.cangjie.psi.stubs.CangJieMacroExpressionStub
import org.cangnova.cangjie.psi.stubs.impl.CangJieMacroExpressionStubImpl
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef

class CjMacroExpressionElementType(debugName: String) : CjStubElementType<CangJieMacroExpressionStub, CjMacroExpression>(
    debugName,
    CjMacroExpression::class.java,
    CangJieMacroExpressionStub::class.java,
) {
    override fun serialize(stub: CangJieMacroExpressionStub, dataStream: StubOutputStream) {
        dataStream.writeName(stub.getShortName())
        dataStream.writeBoolean(stub.hasValueArguments())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieMacroExpressionStub {
        val text = dataStream.readName()
        val hasValueArguments = dataStream.readBoolean()
        //        int valueArgCount = dataStream.readInt();
//        Map<Name, ConstantValue<?>> args = new LinkedHashMap<>();
//        for (int i = 0; i < valueArgCount; i++) {
//            args.put(Name.identifier(Objects.requireNonNull(dataStream.readNameString())),
//                    CangJieConstantValueKt.createConstantValue(dataStream));
//        }
        return CangJieMacroExpressionStubImpl(parentStub, text, hasValueArguments)
    }

    override fun createStub(
        psi: CjMacroExpression,
        parentStub: StubElement<out PsiElement>?,
    ): CangJieMacroExpressionStub {
        val shortName = psi.shortName
        val resultName = shortName?.asString()
        val valueArgumentList = psi.valueArgumentList
        val hasValueArguments = valueArgumentList != null && valueArgumentList.arguments.isNotEmpty()
        return CangJieMacroExpressionStubImpl(parentStub, StringRef.fromString(resultName), hasValueArguments)
    }
}
