package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjProperty
import com.linqingying.cangjie.psi.CjVariable
import com.linqingying.cangjie.psi.stubs.CangJiePropertyStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef

class CangJiePropertyStubImpl(
    parent: StubElement<out PsiElement>?,
    private val name: StringRef?,
    private val fqName: FqName?
) : CangJieStubBaseImpl<CjProperty>(parent, CjStubElementTypes.PROPERTY), CangJiePropertyStub {
    override fun getFqName(): FqName? = fqName

    override fun getName(): String? = StringRef.toString(name)
}
