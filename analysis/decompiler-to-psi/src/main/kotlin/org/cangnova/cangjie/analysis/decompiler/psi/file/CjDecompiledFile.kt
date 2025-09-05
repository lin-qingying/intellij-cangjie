package org.cangnova.cangjie.analysis.decompiler.psi.file

import com.intellij.openapi.vfs.VirtualFile
import org.cangnova.cangjie.analysis.decompiler.psi.CangJieDecompiledFileViewProvider
import org.cangnova.cangjie.analysis.decompiler.psi.text.DecompiledText
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.LockedClearableLazyValue

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
