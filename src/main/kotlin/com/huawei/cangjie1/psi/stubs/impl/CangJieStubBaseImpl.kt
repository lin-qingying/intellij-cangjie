package com.huawei.cangjie1.psi.stubs.impl

import com.huawei.cangjie1.psi.CjElementImplStub
import com.intellij.psi.stubs.IStubElementType
import com.intellij.psi.stubs.StubBase
import com.intellij.psi.stubs.StubElement

open class CangJieStubBaseImpl<T : CjElementImplStub<*>>(parent: StubElement<*>?, elementType: IStubElementType<*, *>) :
    StubBase<T>(parent, elementType)
