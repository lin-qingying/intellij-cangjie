package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjTupleType
import com.linqingying.cangjie.psi.CjUserType
import com.linqingying.cangjie.psi.stubs.CangJieTupleTypeStub
import com.linqingying.cangjie.psi.stubs.CangJieUserTypeStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement

class CangJieTupleTypeStubImpl(
    parent: StubElement<out PsiElement>?
) : CangJieStubBaseImpl<CjTupleType>(parent, CjStubElementTypes.TUPLE_TYPE), CangJieTupleTypeStub
