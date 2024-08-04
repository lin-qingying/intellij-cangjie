package com.linqingying.cangjie.psi.stubs.elements

import com.linqingying.cangjie.psi.CjConstructorElementType
import com.linqingying.cangjie.psi.CjSecondaryConstructor
import com.linqingying.cangjie.psi.stubs.CangJieConstructorStub
import com.linqingying.cangjie.psi.stubs.impl.CangJieConstructorStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


class CjSecondaryConstructorElementType(debugName: String) :
    CjConstructorElementType<CjSecondaryConstructor>(debugName, CjSecondaryConstructor::class.java, CangJieConstructorStub::class.java) {
    override fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isDelegatedCallToThis: Boolean,
    ): CangJieConstructorStub<CjSecondaryConstructor> {
        return CangJieConstructorStubImpl(
            parentStub, CjStubElementTypes.SECONDARY_CONSTRUCTOR, nameRef, hasBody, isDelegatedCallToThis
        )
    }

    override fun isDelegatedCallToThis(constructor: CjSecondaryConstructor) = constructor.getDelegationCallOrNull()?.isCallToThis ?: true
}
