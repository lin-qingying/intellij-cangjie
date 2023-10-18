package com.huawei.cangjie1.psi.stubs.impl

import com.huawei.cangjie1.psi.CjContextReceiver
import com.huawei.cangjie1.psi.stubs.CangJieContextReceiverStub
import com.huawei.cangjie1.psi.stubs.elements.CjContextReceiverElementType
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement


class CangJieContextReceiverStubImpl(
    parent: StubElement<out PsiElement>?,
    elementType: CjContextReceiverElementType,
    private val label: String?,
) : CangJieStubBaseImpl<CjContextReceiver>(parent, elementType), CangJieContextReceiverStub {
    override fun getLabel() = label
}
