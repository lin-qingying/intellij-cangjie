package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.psi.CjTupleType
import com.huawei.cangjie.psi.CjUserType
import com.huawei.cangjie.psi.stubs.CangJieTupleTypeStub
import com.huawei.cangjie.psi.stubs.CangJieUserTypeStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement

class CangJieTupleTypeStubImpl(
    parent: StubElement<out PsiElement>?
) : CangJieStubBaseImpl<CjTupleType>(parent, CjStubElementTypes.TUPLE_TYPE), CangJieTupleTypeStub
