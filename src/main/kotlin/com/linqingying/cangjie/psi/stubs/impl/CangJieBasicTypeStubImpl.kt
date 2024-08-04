package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjBasicType
import com.linqingying.cangjie.psi.stubs.CangJieBasicTypeStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement

class CangJieBasicTypeStubImpl(
    parent: StubElement<out PsiElement>?
) :CangJieStubBaseImpl<CjBasicType>(parent, CjStubElementTypes.BASIC_TYPE),
    CangJieBasicTypeStub
