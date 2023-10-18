package com.huawei.cangjie1.psi.stubs.impl

import com.huawei.cangjie1.name.FqName
import com.huawei.cangjie1.psi.CjImportDirective
import com.huawei.cangjie1.psi.stubs.CangJieImportDirectiveStub
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


class CangJieImportDirectiveStubImpl(
    parent: StubElement<*>,
    private val isAllUnder: Boolean,
    private val importedFqName: StringRef?,
    private val isValid: Boolean
) : CangJieStubBaseImpl<CjImportDirective>(parent, CjStubElementTypes.IMPORT_DIRECTIVE), CangJieImportDirectiveStub {
    override fun isAllUnder(): Boolean = isAllUnder

    override fun getImportedFqName(): FqName? {
        val fqNameString = StringRef.toString(importedFqName)
        return if (fqNameString != null) FqName(fqNameString) else null
    }

    override fun isValid(): Boolean = isValid
}
