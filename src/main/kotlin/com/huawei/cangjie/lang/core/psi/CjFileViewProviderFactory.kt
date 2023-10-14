package com.huawei.cangjie.lang.core.psi

import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider

class CjFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language,
        manager: PsiManager,
        eventSystemEnabled: Boolean
    ): FileViewProvider {
        val shouldAdjustFileLimit = SingleRootFileViewProvider.isTooLargeForIntelligence(file)
                && file.length <= CANGJIE_FILE_SIZE_LIMIT_FOR_INTELLISENSE

        if (shouldAdjustFileLimit) {
            SingleRootFileViewProvider.doNotCheckFileSizeLimit(file)
        }

        return SingleRootFileViewProvider(manager, file, eventSystemEnabled)
    }
}
private const val CANGJIE_FILE_SIZE_LIMIT_FOR_INTELLISENSE: Int = 8 * 1024 * 1024
