package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem
import org.cangnova.cangjie.analysis.api.projectStructure.CaModule

/**
 * 对位 Kotlin `ModuleCandidate`。
 *
 * 当前 IDE 实现没有 Workspace Model entity 级别的模块候选，
 * 但仍保留“候选收集 -> 候选选择 -> 模块构造”的三段职责。
 */
internal sealed class CaModuleCandidate {
    data class FixedModule(
        val module: CaModule,
    ) : CaModuleCandidate()

    data class SourceRoot(
        val root: VirtualFile,
    ) : CaModuleCandidate()

    data class LibrarySourceFile(
        val file: VirtualFile,
    ) : CaModuleCandidate()

    data class LibraryBinaryFile(
        val file: VirtualFile,
    ) : CaModuleCandidate()

    data class NotUnderContentRoot(
        val item: PsiFileSystemItem,
    ) : CaModuleCandidate()
}
