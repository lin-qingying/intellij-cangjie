package com.huawei.cangjie.resolve.lazy.declarations

import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.storage.StorageManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.DelegatingGlobalSearchScope
import com.intellij.psi.search.GlobalSearchScope

abstract class DeclarationProviderFactoryService{
    abstract fun create(
        project: Project,
        storageManager: StorageManager,
        syntheticFiles: Collection<CjFile>,
        filesScope: GlobalSearchScope,
        moduleInfo: ModuleInfo

    ): DeclarationProviderFactory
    companion object {
        @JvmStatic
        fun createDeclarationProviderFactory(
            project: Project,
            storageManager: StorageManager,
            syntheticFiles: Collection<CjFile>,
            moduleContentScope: GlobalSearchScope,
            moduleInfo: ModuleInfo

        ): DeclarationProviderFactory {
            return project.getService(DeclarationProviderFactoryService::class.java)!!
                .create(project, storageManager, syntheticFiles, filteringScope(syntheticFiles, moduleContentScope),moduleInfo)
        }

        private fun filteringScope(syntheticFiles: Collection<CjFile>, baseScope: GlobalSearchScope): GlobalSearchScope {
            if (syntheticFiles.isEmpty() || baseScope == GlobalSearchScope.EMPTY_SCOPE) {
                return baseScope
            }
            return SyntheticFilesFilteringScope(syntheticFiles, baseScope)
        }
    }

    private class SyntheticFilesFilteringScope(syntheticFiles: Collection<CjFile>, baseScope: GlobalSearchScope) :
        DelegatingGlobalSearchScope(baseScope) {

        private val originals = syntheticFiles.mapNotNullTo(HashSet<VirtualFile>()) { it.originalFile.virtualFile }

        override fun contains(file: VirtualFile) = super.contains(file) && file !in originals

        override fun toString() = "SyntheticFilesFilteringScope($myBaseScope)"
    }
}
