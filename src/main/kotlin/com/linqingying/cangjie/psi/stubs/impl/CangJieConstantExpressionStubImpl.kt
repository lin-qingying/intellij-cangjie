package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjConstantExpression
import com.linqingying.cangjie.psi.stubs.CangJieConstantExpressionStub
import com.linqingying.cangjie.psi.stubs.ConstantValueKind
import com.linqingying.cangjie.psi.stubs.elements.CjConstantExpressionElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


class CangJieConstantExpressionStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: CjConstantExpressionElementType,
    private val kind: ConstantValueKind,
    private val value: StringRef
) : CangJieStubBaseImpl<CjConstantExpression>(parent, elementType), CangJieConstantExpressionStub {
    override fun kind(): ConstantValueKind = kind
    override fun value(): String = StringRef.toString(value)
}
