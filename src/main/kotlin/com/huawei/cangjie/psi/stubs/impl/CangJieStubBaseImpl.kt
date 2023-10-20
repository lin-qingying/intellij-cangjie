package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.psi.CjElementImplStub
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

open class CangJieStubBaseImpl<T : CjElementImplStub<*>>(parent: StubElement<*>?, elementType: IStubElementType<*, *>) :
    StubBase<T>(parent, elementType)
