package com.huawei.cangjie.idea.stubindex.resolve

import com.huawei.cangjie.idea.base.projectStructure.CangJieSourceFilterScope
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.lazy.declarations.DeclarationProviderFactory
import com.huawei.cangjie.resolve.lazy.declarations.DeclarationProviderFactoryService
import com.huawei.cangjie.storage.StorageManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope


class PluginDeclarationProviderFactoryService : DeclarationProviderFactoryService() {
    override fun create(
        project: Project,
        storageManager: StorageManager,
        syntheticFiles: Collection<CjFile>,
        filesScope: GlobalSearchScope,

    ): DeclarationProviderFactory {


        return PluginDeclarationProviderFactory(
            project,
           CangJieSourceFilterScope.projectSources(filesScope, project),
            storageManager,
            syntheticFiles,

        )
    }
}
