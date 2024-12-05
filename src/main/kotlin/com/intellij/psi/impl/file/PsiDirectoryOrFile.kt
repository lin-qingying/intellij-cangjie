package com.intellij.openapi.psi.impl.file

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.impl.PsiManagerImpl
import com.intellij.psi.impl.file.PsiDirectoryImpl

class PsiDirectoryOrFile(
    val psiFile: PsiFileSystemItem , psiManager: PsiManagerImpl, file: VirtualFile
) : PsiDirectoryImpl(psiManager, file){


    override fun isDirectory(): Boolean {
        return psiFile.isDirectory == true
    }
}
