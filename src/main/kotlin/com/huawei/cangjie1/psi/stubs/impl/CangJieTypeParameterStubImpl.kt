package com.huawei.cangjie1.psi.stubs.impl

import com.huawei.cangjie1.psi.CjTypeParameter
import com.huawei.cangjie1.psi.stubs.CangJieTypeParameterStub
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef



class CangJieTypeParameterStubImpl(
    parent: StubElement<out PsiElement>?,
    private val name: StringRef?,
    private val isInVariance: Boolean,

) : CangJieStubBaseImpl<CjTypeParameter>(parent, CjStubElementTypes.TYPE_PARAMETER), CangJieTypeParameterStub {
    override fun isInVariance() = isInVariance

    override fun getName() = StringRef.toString(name)
    // type parameters don't have FqNames
    override fun getFqName() = null
}
