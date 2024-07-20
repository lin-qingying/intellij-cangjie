package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.name.ClassId
import com.huawei.cangjie.psi.CjProjectionKind
import com.huawei.cangjie.psi.CjUserType
import com.huawei.cangjie.psi.stubs.CangJieUserTypeStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes


import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement


class CangJieUserTypeStubImpl(
    parent: StubElement<out PsiElement>?
) : CangJieStubBaseImpl<CjUserType>(parent, CjStubElementTypes.USER_TYPE), CangJieUserTypeStub
