package com.linqingying.cangjie.psi.stubs.impl

import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjScript
import com.linqingying.cangjie.psi.stubs.CangJieScriptStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes

class CangJieScriptStubImpl(
    parent: StubElement<out PsiElement>?,
    private val _fqName: StringRef?
) : CangJieStubBaseImpl<CjScript>(parent, CjStubElementTypes.CJ_SCRIPT), CangJieScriptStub {
    override fun getName(): String = getFqName().shortName().asString()

    override fun getFqName(): FqName = FqName(StringRef.toString(_fqName)!!)
}
