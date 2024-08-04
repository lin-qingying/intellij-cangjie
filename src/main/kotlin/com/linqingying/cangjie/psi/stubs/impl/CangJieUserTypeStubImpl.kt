package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.name.ClassId
import com.linqingying.cangjie.psi.CjProjectionKind
import com.linqingying.cangjie.psi.CjUserType
import com.linqingying.cangjie.psi.stubs.CangJieUserTypeStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes


import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement


class CangJieUserTypeStubImpl(
    parent: StubElement<out PsiElement>?
) : CangJieStubBaseImpl<CjUserType>(parent, CjStubElementTypes.USER_TYPE), CangJieUserTypeStub
