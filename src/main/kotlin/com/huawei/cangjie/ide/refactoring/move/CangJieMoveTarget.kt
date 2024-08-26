package com.huawei.cangjie.ide.refactoring.move

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjElement
import com.huawei.cangjie.psi.CjFile
import com.intellij.openapi.module.ModuleUtilCore
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiElement
fun CangJieMoveTarget.getTargetModule(project: Project) = targetFileOrDir?.let { com.intellij.openapi.module.ModuleUtilCore.findModuleForFile(it, project) }


sealed interface CangJieMoveTarget{
    val targetFileOrDir: VirtualFile?

    val targetContainerFqName: FqName?
    fun getTargetPsiIfExists(originalPsi: PsiElement): CjElement?
    fun getOrCreateTargetPsi(originalPsi: PsiElement): CjElement
    object Empty : CangJieMoveTarget {
        override val targetContainerFqName: FqName? = null

        override val targetFileOrDir: VirtualFile? = null

        override fun getOrCreateTargetPsi(originalPsi: PsiElement): CjElement = throw UnsupportedOperationException()

        override fun getTargetPsiIfExists(originalPsi: PsiElement): CjElement? = null
    }

    class Directory(targetPackageFqName: FqName, override val targetFileOrDir: VirtualFile) : CangJieMoveTarget {
        override val targetContainerFqName = targetPackageFqName

        override fun getOrCreateTargetPsi(originalPsi: PsiElement): CjFile {
            val file = originalPsi.containingFile ?: error("PSI element in not contained in any file: $originalPsi")
            return file as CjFile
        }

        override fun getTargetPsiIfExists(originalPsi: PsiElement): CjElement? = null
    }
}
