package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.psi.CjCollectionLiteralExpression
import com.huawei.cangjie.psi.stubs.CangJieCollectionLiteralExpressionStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement


class CangJieCollectionLiteralExpressionStubImpl(
    parent: StubElement<out PsiElement>?
) : CangJieStubBaseImpl<CjCollectionLiteralExpression>(parent, CjStubElementTypes.COLLECTION_LITERAL_EXPRESSION),
    CangJieCollectionLiteralExpressionStub
