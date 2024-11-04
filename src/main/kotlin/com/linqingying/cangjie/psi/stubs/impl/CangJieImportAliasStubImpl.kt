package com.linqingying.cangjie.psi.stubs.impl

import com.linqingying.cangjie.psi.CjImportAlias
import com.linqingying.cangjie.psi.stubs.CangJieImportAliasStub
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef



class CangJieImportAliasStubImpl(
    parent: StubElement<out PsiElement>?,
    private val name: StringRef?
) : CangJieStubBaseImpl<CjImportAlias>(parent, CjStubElementTypes.IMPORT_ALIAS), CangJieImportAliasStub {
    override fun getName(): String? = StringRef.toString(name)
}
