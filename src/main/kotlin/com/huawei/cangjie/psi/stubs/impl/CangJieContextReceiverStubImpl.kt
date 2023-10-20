package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.psi.CjContextReceiver
import com.huawei.cangjie.psi.stubs.CangJieContextReceiverStub
import com.huawei.cangjie.psi.stubs.elements.CjContextReceiverElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement


class CangJieContextReceiverStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: CjContextReceiverElementType,
    private val label: String?,
) : CangJieStubBaseImpl<CjContextReceiver>(parent, elementType), CangJieContextReceiverStub {
    override fun getLabel() = label
}
