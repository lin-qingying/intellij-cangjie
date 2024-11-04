package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjContextReceiver
import com.linqingying.cangjie.psi.stubs.CangJieContextReceiverStub
import com.linqingying.cangjie.psi.stubs.elements.CjContextReceiverElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement


class CangJieContextReceiverStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: CjContextReceiverElementType,
    private val label: String?,
) : CangJieStubBaseImpl<CjContextReceiver>(parent, elementType), CangJieContextReceiverStub {
    override fun getLabel() = label
}
