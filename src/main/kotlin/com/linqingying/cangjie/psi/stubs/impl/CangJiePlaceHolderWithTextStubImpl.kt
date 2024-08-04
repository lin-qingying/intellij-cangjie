package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjElementImplStub
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderWithTextStub
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubElement



class CangJiePlaceHolderWithTextStubImpl<T : CjElementImplStub<out StubElement<*>>>(
    parent: StubElement<*>,
    elementType: IStubElementType<*, *>,
    private val text: String
) : CangJieStubBaseImpl<T>(parent, elementType), CangJiePlaceHolderWithTextStub<T> {
    override fun text(): String = text
}
