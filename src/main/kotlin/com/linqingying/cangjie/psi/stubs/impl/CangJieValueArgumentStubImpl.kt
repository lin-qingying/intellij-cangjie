package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjValueArgument
import com.linqingying.cangjie.psi.stubs.CangJieValueArgumentStub
import com.linqingying.cangjie.psi.stubs.elements.CjValueArgumentElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement


class CangJieValueArgumentStubImpl<T : CjValueArgument>(
    parent: StubElement<out PsiElement>?,
    elementType: CjValueArgumentElementType<T>,
    private val isSpread: Boolean
) : CangJiePlaceHolderStubImpl<T>(parent, elementType), CangJieValueArgumentStub<T> {
    override fun isSpread(): Boolean = isSpread
}
