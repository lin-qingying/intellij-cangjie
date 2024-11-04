package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjPropertyAccessor
import com.linqingying.cangjie.psi.stubs.CangJiePropertyAccessorStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement


class CangJiePropertyAccessorStubImpl(
    parent: StubElement<*>?,
    private val isGetter: Boolean,
    private val hasBody: Boolean,
    private val hasBlockBody: Boolean
) : CangJieStubBaseImpl<CjPropertyAccessor>(
    parent,
    CjStubElementTypes.PROPERTY_ACCESSOR
), CangJiePropertyAccessorStub {
    override fun isGetter(): Boolean  = isGetter


    override fun hasBody(): Boolean = hasBody

    override fun hasBlockBody(): Boolean = hasBlockBody
}

