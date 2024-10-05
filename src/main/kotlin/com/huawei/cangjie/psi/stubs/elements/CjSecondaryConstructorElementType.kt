package com.huawei.cangjie.psi.stubs.elements

import com.huawei.cangjie.psi.CjConstructorElementType
import com.huawei.cangjie.psi.CjEndSecondaryConstructor
import com.huawei.cangjie.psi.CjSecondaryConstructor
import com.huawei.cangjie.psi.stubs.CangJieConstructorStub
import com.huawei.cangjie.psi.stubs.impl.CangJieConstructorStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

class CjEndSecondaryConstructorElementType(debugName: String) :
    CjConstructorElementType<CjEndSecondaryConstructor>(debugName, CjEndSecondaryConstructor::class.java, CangJieConstructorStub::class.java) {
    override fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isDelegatedCallToThis: Boolean,
    ): CangJieConstructorStub<CjEndSecondaryConstructor> {
        return CangJieConstructorStubImpl(
            parentStub, CjStubElementTypes.END_SECONDARY_CONSTRUCTOR, nameRef, hasBody, isDelegatedCallToThis
        )
    }

    override fun isDelegatedCallToThis(constructor: CjEndSecondaryConstructor) = constructor.getDelegationCallOrNull()?.isCallToThis ?: true
}

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
