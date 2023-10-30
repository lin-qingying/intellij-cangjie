package com.huawei.cangjie.psi.stubs.elements

import com.huawei.cangjie.psi.CjConstantExpression
import com.huawei.cangjie.psi.stubs.CangJieConstantExpressionStub
import com.huawei.cangjie.psi.stubs.ConstantValueKind
import com.huawei.cangjie.psi.stubs.ConstantValueKind.*
import com.huawei.cangjie.psi.stubs.impl.CangJieConstantExpressionStubImpl
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
        CangJieConstantExpressionStub::class.java
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
            StringRef.fromString(value)
        )
    }

    override fun serialize(stub: CangJieConstantExpressionStub, dataStream: StubOutputStream) {
        dataStream.writeInt(stub.kind().ordinal)
        dataStream.writeName(stub.value())
    }

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieConstantExpressionStub {
        val kindOrdinal = dataStream.readInt()
        val value = dataStream.readName() ?: StringRef.fromString("")

        val valueKind = ConstantValueKind.values()[kindOrdinal]

        return CangJieConstantExpressionStubImpl(
            parentStub,
            kindToConstantElementType(valueKind),
            valueKind,
            value
        )
    }

    companion object {
        fun kindToConstantElementType(kind: ConstantValueKind): CjConstantExpressionElementType {
            return when (kind) {

                BOOLEAN_CONSTANT -> CjStubElementTypes.BOOLEAN_CONSTANT
                FLOAT_CONSTANT -> CjStubElementTypes.FLOAT_CONSTANT
                CHARACTER_CONSTANT -> CjStubElementTypes.CHARACTER_CONSTANT
                INTEGER_CONSTANT -> CjStubElementTypes.INTEGER_CONSTANT
                UNIT_CONSTANT -> CjStubElementTypes.UNIT_CONSTANT

            }
        }

        private fun constantElementTypeToKind(elementType: CjConstantExpressionElementType): ConstantValueKind {
            return when (elementType) {

                CjStubElementTypes.BOOLEAN_CONSTANT -> BOOLEAN_CONSTANT
                CjStubElementTypes.INTEGER_CONSTANT -> INTEGER_CONSTANT
                CjStubElementTypes.FLOAT_CONSTANT -> FLOAT_CONSTANT
                CjStubElementTypes.CHARACTER_CONSTANT -> CHARACTER_CONSTANT
                CjStubElementTypes.UNIT_CONSTANT -> UNIT_CONSTANT
                else -> throw IllegalStateException("Unknown constant node type: $elementType")
            }
        }
    }
}
