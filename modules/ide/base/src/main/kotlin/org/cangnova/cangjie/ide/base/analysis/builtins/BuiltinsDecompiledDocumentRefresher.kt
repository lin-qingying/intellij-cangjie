package org.cangnova.cangjie.ide.base.analysis.builtins

import com.intellij.openapi.fileEditor.FileDocumentManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiManager
import com.intellij.util.FileContentUtilCore
import org.cangnova.cangjie.analysis.decompiled.psi.BuiltinsVirtualFileProvider

/**
 * 刷新当前项目 builtins `.cjo` 的 PSI 与 editor document。
 *
 * 标准库 toolchain 从“不可用/不可反编译”切到“可用”时，
 * binary decompiler 可能已经把旧的失败占位文本缓存进 document。
 * 这里只走真实项目刷新链统一重建：
 * 1. 让 `.cjo` 文件重新走 filetype decompiler / view provider；
 * 2. 若 document 已存在，再强制从当前 file 文本重载，避免继续保留旧占位文本。
 */
object BuiltinsDecompiledDocumentRefresher {
    fun refresh(project: Project) {
        if (project.isDisposed) return

        val builtinsFiles = BuiltinsVirtualFileProvider.getInstance().getBuiltinVirtualFiles(project)
        if (builtinsFiles.isEmpty()) return

        PsiManager.getInstance(project).dropPsiCaches()
        FileContentUtilCore.reparseFiles(builtinsFiles)

        val fileDocumentManager = FileDocumentManager.getInstance()
        builtinsFiles.forEach { virtualFile ->
            reloadCachedDocument(project, virtualFile, fileDocumentManager)
        }
    }

    private fun reloadCachedDocument(
        project: Project,
        virtualFile: VirtualFile,
        fileDocumentManager: FileDocumentManager,
    ) {
        val document = fileDocumentManager.getCachedDocument(virtualFile) ?: return
        fileDocumentManager.reloadFromDisk(document, project)
    }
}
