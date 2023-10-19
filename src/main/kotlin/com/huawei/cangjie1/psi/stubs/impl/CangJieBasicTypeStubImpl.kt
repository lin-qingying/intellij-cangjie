package com.huawei.cangjie1.psi.stubs.impl

import com.huawei.cangjie1.psi.CjBasicType
import com.huawei.cangjie1.psi.CjUserType
import com.huawei.cangjie1.psi.stubs.CangJieBasicTypeStub
import com.huawei.cangjie1.psi.stubs.CangJieUserTypeStub
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement

class CangJieBasicTypeStubImpl(
    parent: StubElement<out PsiElement>?
) :CangJieStubBaseImpl<CjBasicType>(parent, CjStubElementTypes.BASIC_TYPE),
    CangJieBasicTypeStub
