package com.huawei.cangjie.utils

import com.huawei.cangjie.psi.CjFile
import com.intellij.injected.editor.VirtualFileWindow
import com.intellij.openapi.module.Module
import com.intellij.openapi.roots.ProjectRootManager
import com.intellij.openapi.vfs.NonPhysicalFileSystem
import com.intellij.psi.PsiElement


fun PsiElement.isUnderCangJieSourceRootTypes(): Boolean {
    val cjFile = this.containingFile.safeAs<CjFile>() ?: return false
    val file = cjFile.virtualFile?.takeIf { it !is VirtualFileWindow && it.fileSystem !is NonPhysicalFileSystem } ?: return false
    val projectFileIndex = ProjectRootManager.getInstance(cjFile.project).fileIndex
    return projectFileIndex.isInTestSourceContent(file )
}
//fun Module.asSourceInfo(sourceRootType: CangJieSourceRootType?): ModuleSourceInfoWithExpectedBy? =
//    when (sourceRootType) {
//        SourceCangJieRootType -> ModuleProductionSourceInfo(this)
//        TestSourceCangJieRootType -> ModuleTestSourceInfo(this)
//        else -> null
//    }
