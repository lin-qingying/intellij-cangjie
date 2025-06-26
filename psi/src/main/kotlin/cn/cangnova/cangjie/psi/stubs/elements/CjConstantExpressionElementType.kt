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

import cn.cangnova.cangjie.psi.CjConstantExpression
import cn.cangnova.cangjie.psi.stubs.CangJieConstantExpressionStub
import cn.cangnova.cangjie.psi.stubs.ConstantValueKind
import cn.cangnova.cangjie.psi.stubs.ConstantValueKind.*
import cn.cangnova.cangjie.psi.stubs.impl.CangJieConstantExpressionStubImpl
import com.intellij.lang.ASTNode
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import com.intellij.util.io.StringRef
import org.jetbrains.annotations.NonNls

class CjConstantExpressionElementType(@NonNls debugName: String) :
    CjStubElementType<CangJieConstantExpressionStub, CjConstantExpression>(
        debugName,
        CjConstantExpression::class.java,
        CangJieConstantExpressionStub::class.java,
    ) {

    override fun shouldCreateStub(node: ASTNode): Boolean {
        val parent = node.treeParent ?: return false
        if (parent.elementType != CjStubElementTypes.VALUE_ARGUMENT) return false

        return super.shouldCreateStub(node)
    }

    override fun createStub(psi: CjConstantExpression, parentStub: StubElement<*>?): CangJieConstantExpressionStub {
        val elementType = psi.node.elementType as? CjConstantExpressionElementType
            ?: throw IllegalStateException("Stub element type is expected for constant")

        val value = psi.text ?: ""

        return CangJieConstantExpressionStubImpl(
            parentStub,
            elementType,
            constantElementTypeToKind(elementType),
            StringRef.fromString(value),
        )
    }

    override fun serialize(stub: CangJieConstantExpressionStub, dataStream: StubOutputStream) {
        dataStream.writeInt(stub.kind().ordinal)
        dataStream.writeName(stub.value())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieConstantExpressionStub {
        val kindOrdinal = dataStream.readInt()
        val value = dataStream.readName() ?: StringRef.fromString("")

        val valueKind = entries[kindOrdinal]

        return CangJieConstantExpressionStubImpl(
            parentStub,
            kindToConstantElementType(valueKind),
            valueKind,
            value,
        )
    }

    companion object {
        fun kindToConstantElementType(kind: ConstantValueKind): CjConstantExpressionElementType {
            return when (kind) {
                BOOLEAN_CONSTANT -> CjStubElementTypes.BOOLEAN_CONSTANT
                FLOAT_CONSTANT -> CjStubElementTypes.FLOAT_CONSTANT
                RUNE_CONSTANT -> CjStubElementTypes.RUNE_CONSTANT
                CHARACTER_BYTE_CONSTANT -> CjStubElementTypes.CHARACTER_BYTE_CONSTANT
                INTEGER_CONSTANT -> CjStubElementTypes.INTEGER_CONSTANT
                UNIT_CONSTANT -> CjStubElementTypes.UNIT_CONSTANT
            }
        }

        private fun constantElementTypeToKind(elementType: CjConstantExpressionElementType): ConstantValueKind {
            return when (elementType) {
                CjStubElementTypes.BOOLEAN_CONSTANT -> BOOLEAN_CONSTANT
                CjStubElementTypes.INTEGER_CONSTANT -> INTEGER_CONSTANT
                CjStubElementTypes.FLOAT_CONSTANT -> FLOAT_CONSTANT
                CjStubElementTypes.CHARACTER_BYTE_CONSTANT -> CHARACTER_BYTE_CONSTANT
                CjStubElementTypes.RUNE_CONSTANT -> RUNE_CONSTANT
                CjStubElementTypes.UNIT_CONSTANT -> UNIT_CONSTANT
                else -> throw IllegalStateException("Unknown constant node type: $elementType")
            }
        }
    }
}
