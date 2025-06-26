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

import cn.cangnova.cangjie.psi.CjCollectionLiteralExpression
import cn.cangnova.cangjie.psi.stubs.CangJieCollectionLiteralExpressionStub
import cn.cangnova.cangjie.psi.stubs.impl.CangJieCollectionLiteralExpressionStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls

class CjCollectionLiteralExpressionElementType(@NonNls debugName: String) :
    CjStubElementType<CangJieCollectionLiteralExpressionStub, CjCollectionLiteralExpression>(
        debugName,
        CjCollectionLiteralExpression::class.java,
        CangJieCollectionLiteralExpressionStub::class.java,
    ) {
    override fun serialize(stub: CangJieCollectionLiteralExpressionStub, dataStream: StubOutputStream) {}

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieCollectionLiteralExpressionStub {
        return CangJieCollectionLiteralExpressionStubImpl(parentStub)
    }

    override fun createStub(psi: CjCollectionLiteralExpression, parentStub: StubElement<*>?): CangJieCollectionLiteralExpressionStub {
        return CangJieCollectionLiteralExpressionStubImpl(parentStub)
    }
}
