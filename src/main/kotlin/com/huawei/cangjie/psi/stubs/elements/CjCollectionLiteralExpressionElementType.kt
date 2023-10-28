package com.huawei.cangjie.psi.stubs.elements

import com.huawei.cangjie.psi.CjCollectionLiteralExpression
import com.huawei.cangjie.psi.stubs.CangJieCollectionLiteralExpressionStub
import com.huawei.cangjie.psi.stubs.impl.CangJieCollectionLiteralExpressionStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.psi.stubs.StubInputStream
import com.intellij.psi.stubs.StubOutputStream
import org.jetbrains.annotations.NonNls


class CjCollectionLiteralExpressionElementType(@NonNls debugName: String) :
    CjStubElementType<CangJieCollectionLiteralExpressionStub, CjCollectionLiteralExpression>(
        debugName,
        CjCollectionLiteralExpression::class.java,
        CangJieCollectionLiteralExpressionStub::class.java
    ) {
    override fun serialize(stub: CangJieCollectionLiteralExpressionStub, dataStream: StubOutputStream) {}

    override fun deserialize(dataStream: StubInputStream, parentStub: StubElement<*>?): CangJieCollectionLiteralExpressionStub {
        return CangJieCollectionLiteralExpressionStubImpl(parentStub)
    }

    override fun createStub(psi: CjCollectionLiteralExpression, parentStub: StubElement<*>?): CangJieCollectionLiteralExpressionStub {
        return CangJieCollectionLiteralExpressionStubImpl(parentStub)
    }
}
