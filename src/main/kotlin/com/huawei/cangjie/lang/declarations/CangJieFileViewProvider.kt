package com.huawei.cangjie.lang.declarations

import com.huawei.cangjie.lang.CangJieLanguage
import com.huawei.cangjie.psi.CjFile
import com.intellij.lang.Language
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.FileViewProvider
import com.intellij.psi.FileViewProviderFactory
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider

class CangJieFileViewProviderFactory : FileViewProviderFactory {
    override fun createFileViewProvider(
        file: VirtualFile,
        language: Language?,
        manager: PsiManager,
        eventSystemEnabled: Boolean
    ): FileViewProvider {

//        return SingleRootFileViewProvider(manager, file)
        return CangJieFileViewProvider(manager, file, eventSystemEnabled, ::CjDeclarationsFile)
    }


}


class CangJieFileViewProvider(
    manager: PsiManager,
    file: VirtualFile,
    physical: Boolean,
    private val factory: (CangJieFileViewProvider) -> CjFile?
) : SingleRootFileViewProvider(manager, file, physical, CangJieLanguage) {

//    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
//        return factory(this)
//    }

    override fun createCopy(copy: VirtualFile) = CangJieFileViewProvider(manager, copy, false, factory)


}
