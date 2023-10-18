package com.huawei.cangjie1.psi.stubs.impl

import com.huawei.cangjie1.psi.CjImportAlias
import com.huawei.cangjie1.psi.stubs.CangJieImportAliasStub
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.PsiElement
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef



class CangJieImportAliasStubImpl(
    parent: StubElement<out PsiElement>?,
    private val name: StringRef?
) : CangJieStubBaseImpl<CjImportAlias>(parent, CjStubElementTypes.IMPORT_ALIAS), CangJieImportAliasStub {
    override fun getName(): String? = StringRef.toString(name)
}
