package com.huawei.cangjie.lang.declarations

import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.fileTypes.FileType

class CjDeclarationsFile(
    private val provider: CangJieDeclarationsFileViewProvider,

    ) : CjFile(
    provider

) {


    override fun getFileType(): FileType {
        return CangJieBuiltInFileType
    }
}
