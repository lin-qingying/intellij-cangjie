package com.huawei.cangjie.idea.stubindex.resolve

import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.lazy.declarations.AbstractDeclarationProviderFactory
import com.huawei.cangjie.resolve.lazy.declarations.CombinedPackageMemberDeclarationProvider
import com.huawei.cangjie.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import com.huawei.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider
import com.huawei.cangjie.storage.StorageManager
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

class PluginDeclarationProviderFactory(
    private val project: Project,
    private val indexedFilesScope: GlobalSearchScope,
    private val storageManager: StorageManager,
    private val nonIndexedFiles: Collection<CjFile>,

    ) : AbstractDeclarationProviderFactory(storageManager) {

    private val fileBasedDeclarationProviderFactory =
        FileBasedDeclarationProviderFactory(storageManager, nonIndexedFiles)

    override fun packageExists(fqName: FqName) =
        fileBasedDeclarationProviderFactory.packageExists(fqName) || stubBasedPackageExists(fqName)

    private fun stubBasedPackageExists(name: FqName): Boolean {
        return true
        // We're only looking for source-based declarations
//        return (moduleInfo as? IdeaModuleInfo)?.projectSourceModules()
//            ?.any { PerModulePackageCacheService.getInstance(project).packageExists(name, it) }
//            ?: false
    }

    private fun getStubBasedPackageMemberDeclarationProvider(name: FqName): PackageMemberDeclarationProvider {

        return StubBasedPackageMemberDeclarationProvider(name, project, indexedFilesScope)
    }

    public override fun createPackageMemberDeclarationProvider(name: FqName): PackageMemberDeclarationProvider? {
        val fileBasedProvider = fileBasedDeclarationProviderFactory.getPackageMemberDeclarationProvider(name)
        val stubBasedProvider = getStubBasedPackageMemberDeclarationProvider(name)
        return when {
            fileBasedProvider == null && stubBasedProvider == null -> null
            fileBasedProvider == null -> stubBasedProvider
            stubBasedProvider == null -> fileBasedProvider
            else -> CombinedPackageMemberDeclarationProvider(listOf(stubBasedProvider, fileBasedProvider))
        }
    }
    private fun debugInfo(): String {
        if (nonIndexedFiles.isEmpty()) return "-no synthetic files-\n"

        return buildString {
            nonIndexedFiles.forEach {
                append(it.name)
                append(" isPhysical=${it.isPhysical}")
                append(" modStamp=${it.modificationStamp}")
                appendLine()
            }
        }
    }

    private val onCreationDebugInfo = debugInfo()
    override fun diagnoseMissingPackageFragment(fqName: FqName, file: CjFile?) {
//        TODO("Not yet implemented")
    }
    fun debugToString(): String {
        return arrayOf("PluginDeclarationProviderFactory", "On failure:", debugInfo(), "On creation:", onCreationDebugInfo
            ).joinToString("\n")
    }
}