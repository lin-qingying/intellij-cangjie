package org.cangnova.cangjie.ide.base.analysisApiPlatform.projectStructure.provider

import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFileSystemItem

/**
 * 对位 Kotlin `CandidateCollector`。
 *
 * 仓颉 IDE 当前不依赖 Kotlin 那套 Workspace Model entity 候选，
 * 但仍显式保留候选收集层，避免 provider 直接把文件分类逻辑写死。
 */
internal object CaIdeCandidateCollector {
    fun collectCandidates(
        item: PsiFileSystemItem,
        state: CaIdeProjectStructureState,
    ): Sequence<CaModuleCandidate> {
        val virtualFile = item.virtualFile
            ?: return sequenceOf(CaModuleCandidate.NotUnderContentRoot(item))

        return sequence {
            yieldAll(collectBuiltinsCandidates(virtualFile, state))
            yieldAll(collectCandidatesByVirtualFile(item, virtualFile, state))
        }
    }

    private fun collectBuiltinsCandidates(
        virtualFile: VirtualFile,
        state: CaIdeProjectStructureState,
    ): Collection<CaModuleCandidate> {
        return state.getBuiltinsModules()
            .filter { builtinsModule -> virtualFile in builtinsModule.contentScope }
            .map(CaModuleCandidate::FixedModule)
    }

    private fun collectCandidatesByVirtualFile(
        item: PsiFileSystemItem,
        virtualFile: VirtualFile,
        state: CaIdeProjectStructureState,
    ): Collection<CaModuleCandidate> {
        if (state.isInLibrarySource(virtualFile)) {
            return listOf(CaModuleCandidate.LibrarySourceFile(virtualFile))
        }

        if (state.isInLibraryClasses(virtualFile)) {
            return listOf(CaModuleCandidate.LibraryBinaryFile(virtualFile))
        }

        val sourceRoot = state.getSourceRootForFile(virtualFile)
        if (sourceRoot != null && state.isInSource(virtualFile)) {
            return listOf(CaModuleCandidate.SourceRoot(sourceRoot))
        }

        return listOf(CaModuleCandidate.NotUnderContentRoot(item))
    }
}
