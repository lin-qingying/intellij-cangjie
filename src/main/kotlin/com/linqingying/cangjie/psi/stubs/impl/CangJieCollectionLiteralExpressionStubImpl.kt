package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjCollectionLiteralExpression
import com.linqingying.cangjie.psi.stubs.CangJieCollectionLiteralExpressionStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement


class CangJieCollectionLiteralExpressionStubImpl(
    parent: StubElement<out PsiElement>?
) : CangJieStubBaseImpl<CjCollectionLiteralExpression>(parent, CjStubElementTypes.COLLECTION_LITERAL_EXPRESSION),
    CangJieCollectionLiteralExpressionStub
