package com.linqingying.cangjie.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjCatchParameter
import com.linqingying.cangjie.psi.stubs.CangJieCatchParameterStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes

class CangJieCatchParameterStubImpl(
    private val fqName: StringRef?,

    private val name: StringRef?,
    parent: StubElement<out PsiElement>?,

    ) : CangJieStubBaseImpl<CjCatchParameter>(
    parent, CjStubElementTypes.CATCH_PARAMETER
), CangJieCatchParameterStub {
    override fun getName(): String? {
        return StringRef.toString(name)
    }

    override fun getFqName(): FqName? {
        return if (fqName != null) FqName(fqName.string) else null
    }
}
