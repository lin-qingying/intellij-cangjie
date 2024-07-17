package com.huawei.cangjie.lang.declarations

import com.huawei.cangjie.lang.CangJieLanguage
import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider

class CangJieDeclarationsFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language?,
        manager: PsiManager,
        eventSystemEnabled: Boolean
    ): FileViewProvider {

//        return SingleRootFileViewProvider(manager, file)
        return CangJieDeclarationsFileViewProvider(manager, file, eventSystemEnabled, ::CjDeclarationsFile)
    }


}


class CangJieDeclarationsFileViewProvider(
    manager: PsiManager,
    file: VirtualFile,
    physical: Boolean,
    private val factory: (CangJieDeclarationsFileViewProvider) -> CjDeclarationsFile?
) : SingleRootFileViewProvider(manager, file, physical, CangJieLanguage) {

//    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
//        return factory(this)
//    }

    override fun createCopy(copy: VirtualFile) = CangJieDeclarationsFileViewProvider(manager, copy, false, factory)


}
