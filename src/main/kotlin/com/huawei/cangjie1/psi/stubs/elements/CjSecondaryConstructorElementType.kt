package com.huawei.cangjie1.psi.stubs.elements

import com.huawei.cangjie1.psi.CjConstructorElementType
import com.huawei.cangjie1.psi.CjSecondaryConstructor
import com.huawei.cangjie1.psi.stubs.CangJieConstructorStub
import com.huawei.cangjie1.psi.stubs.impl.CangJieConstructorStubImpl
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
