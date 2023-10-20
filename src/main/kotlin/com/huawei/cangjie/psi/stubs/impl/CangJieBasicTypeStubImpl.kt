package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.psi.CjBasicType
import com.huawei.cangjie.psi.stubs.CangJieBasicTypeStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement

class CangJieBasicTypeStubImpl(
    parent: StubElement<out PsiElement>?
) :CangJieStubBaseImpl<CjBasicType>(parent, CjStubElementTypes.BASIC_TYPE),
    CangJieBasicTypeStub
