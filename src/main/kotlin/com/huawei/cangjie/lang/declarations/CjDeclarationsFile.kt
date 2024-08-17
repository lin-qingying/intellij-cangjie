package com.huawei.cangjie.lang.declarations

import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.fileTypes.FileType
import com.intellij.psi.FileViewProvider

class CjDeclarationsFile(
    private val provider: FileViewProvider,

    ) : CjFile(
    provider

) {
    override fun toString(): String {
        return "CangJieDeclaration File: $name"
    }
    override fun getFileType(): FileType {
        return CangJieBuiltInFileType
    }
}
