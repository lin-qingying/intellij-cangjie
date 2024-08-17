package com.huawei.cangjie.psi.stubs.impl

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjImportDirective
import com.huawei.cangjie.psi.stubs.CangJieImportDirectiveStub
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes
import com.intellij.psi.stubs.StubElement
import com.intellij.util.io.StringRef


class CangJieImportDirectiveStubImpl(
    parent: StubElement<*>,
    private val isAllUnder: Boolean,
    private val importedFqName: StringRef? = null,
//    private val importedFqNames: List<StringRef> = emptyList(),
    private val isValid: Boolean
) : CangJieStubBaseImpl<CjImportDirective>(parent, CjStubElementTypes.IMPORT_DIRECTIVE), CangJieImportDirectiveStub {
    override fun isAllUnder(): Boolean = isAllUnder

    override fun getImportedFqName(): FqName? {
        val fqNameString = StringRef.toString(importedFqName)
        return if (fqNameString != null) FqName(fqNameString) else null
    }

//    override fun getImportedFqNames(): List<FqName> {
//     return   importedFqNames.map {
//         StringRef.toString(it)?.let { FqName(it) }!!
//        }
//    }

    override fun isValid(): Boolean = isValid
}
