package com.huawei.cangjie.lang.declarations

import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.vfs.VirtualFile

class CjDeclarationsFile (
    private val provider: CangJieDeclarationsFileViewProvider,

): CjFile(
    provider

) {
}
