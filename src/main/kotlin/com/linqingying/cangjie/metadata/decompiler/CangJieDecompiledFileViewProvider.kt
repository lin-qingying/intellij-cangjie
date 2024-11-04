package com.linqingying.cangjie.metadata.decompiler

import com.intellij.openapi.fileTypes.FileType
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiInvalidElementAccessException
import com.intellij.psi.PsiManager
import com.intellij.psi.SingleRootFileViewProvider
import com.intellij.psi.impl.DebugUtil
import com.intellij.psi.impl.source.PsiFileImpl
import com.linqingying.cangjie.lang.CangJieFileType
import com.linqingying.cangjie.lang.CangJieLanguage
import com.linqingying.cangjie.lang.declarations.CjDeclarationsFile
import com.linqingying.cangjie.lang.declarations.CjDecompiledFile
import com.linqingying.cangjie.utils.LockedClearableLazyValue


class CangJieDecompiledFileViewProvider(
    manager: PsiManager,
    file: VirtualFile,
    physical: Boolean,
    private val factory: (Project,CangJieDecompiledFileViewProvider) -> CjDecompiledFile?
) : SingleRootFileViewProvider(manager, file, physical, CangJieLanguage ) {
    val content: LockedClearableLazyValue<String> = LockedClearableLazyValue(Any()) {
        val psiFile = createFile(manager.project, file, CangJieFileType.INSTANCE)
        val text = psiFile?.text ?: ""

        DebugUtil.performPsiModification<PsiInvalidElementAccessException>("Invalidating throw-away copy of file that was used for getting text") {
            (psiFile as? PsiFileImpl)?.markInvalidated()
        }

        text
    }

    override fun createFile(project: Project, file: VirtualFile, fileType: FileType): PsiFile? {
        return factory(project,this)
    }

    override fun createCopy(copy: VirtualFile) = CangJieDecompiledFileViewProvider(manager, copy, false, factory)

    override fun getContents() = content.get()
}
