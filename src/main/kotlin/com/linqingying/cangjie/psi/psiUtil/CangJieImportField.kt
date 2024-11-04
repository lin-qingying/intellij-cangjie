package com.linqingying.cangjie.psi.psiUtil

import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.psi.CjImportInfo

data class CangJieImportField(

    override val importedFqName: FqName,
    override val aliasName: String? = null,
    override val isAllUnder: Boolean,
    override val importContent: CjImportInfo.ImportContent?,
//    override val importedFqNames: MutableList<FqName>?
): CjImportInfo
