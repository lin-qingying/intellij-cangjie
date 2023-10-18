package com.huawei.cangjie1.psi.stubs.elements

import com.huawei.cangjie1.psi.CjConstructorElementType
import com.huawei.cangjie1.psi.CjPrimaryConstructor
import com.huawei.cangjie1.psi.stubs.CangJieConstructorStub
import com.huawei.cangjie1.psi.stubs.impl.CangJieConstructorStubImpl
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


class CjPrimaryConstructorElementType(debugName: String) :
    CjConstructorElementType<CjPrimaryConstructor>(debugName, CjPrimaryConstructor::class.java, CangJieConstructorStub::class.java) {
    override fun newStub(
        parentStub: StubElement<*>,
        nameRef: StringRef?,
        hasBody: Boolean,
        isDelegatedCallToThis: Boolean,
    ): CangJieConstructorStub<CjPrimaryConstructor> {
        return CangJieConstructorStubImpl(
            parentStub, CjStubElementTypes.PRIMARY_CONSTRUCTOR, nameRef, hasBody, isDelegatedCallToThis
        )
    }

    override fun isDelegatedCallToThis(constructor: CjPrimaryConstructor) = false
}
