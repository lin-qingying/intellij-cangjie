package com.linqingying.cangjie.lang.declarations

import com.linqingying.cangjie.psi.CjFile
import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.linqingying.cangjie.metadata.decompiler.CangJieDecompiledFileViewProvider
import com.linqingying.cangjie.metadata.decompiler.DecompiledText
import com.linqingying.cangjie.utils.LockedClearableLazyValue

class CjDeclarationsFile(
    private val provider: FileViewProvider,

    ) : CjFile(
    provider

) {
    override fun toString(): String {
        return "CangJieDeclaration File: $name"
    }
    override fun getFileType(): FileType {
        return CangJieDeclarationsFileType
    }
}

open class CjDecompiledFile(
    private val provider: CangJieDecompiledFileViewProvider,
    buildDecompiledText: (VirtualFile) -> DecompiledText
) : CjFile(provider, true) {

    private val decompiledText = LockedClearableLazyValue(Any()) {
        buildDecompiledText(provider.virtualFile)
    }

    override fun getText(): String? {
        return decompiledText.get().text
    }

    override fun onContentReload() {
        super.onContentReload()

        provider.content.drop()
        decompiledText.drop()
    }

}
