package com.linqingying.cangjie.ide.stubindex.resolve

import com.linqingying.cangjie.analyzer.ModuleInfo
import com.linqingying.cangjie.ide.base.projectStructure.CangJieSourceFilterScope
import com.linqingying.cangjie.psi.CjFile
import com.linqingying.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import com.linqingying.cangjie.resolve.lazy.declarations.DeclarationProviderFactoryService
import com.linqingying.cangjie.storage.StorageManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope


class PluginDeclarationProviderFactoryService : DeclarationProviderFactoryService() {
    override fun create(
        project: Project,
        storageManager: StorageManager,
        syntheticFiles: Collection<CjFile>,
        filesScope: GlobalSearchScope,
        moduleInfo: ModuleInfo

    ): DeclarationProviderFactory {


        return PluginDeclarationProviderFactory(
            project,
            CangJieSourceFilterScope.projectSourcesAndLibraryClasses(filesScope, project),
            storageManager,
            syntheticFiles,
            moduleInfo
        )
    }
}
