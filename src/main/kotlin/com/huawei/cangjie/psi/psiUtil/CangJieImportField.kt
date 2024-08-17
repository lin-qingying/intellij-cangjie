package com.huawei.cangjie.psi.psiUtil

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjImportInfo

data class CangJieImportField(

    override val importedFqName: FqName,
    override val aliasName: String? = null,
    override val isAllUnder: Boolean,
    override val importContent: CjImportInfo.ImportContent?,
//    override val importedFqNames: MutableList<FqName>?
): CjImportInfo{

}
