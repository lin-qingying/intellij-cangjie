package com.huawei.cangjie.ide.stubindex.resolve

import com.huawei.cangjie.analyzer.CangJieModuleInfo
import com.huawei.cangjie.analyzer.ModuleInfo
import com.huawei.cangjie.ide.cache.PerModulePackageCacheService
import com.huawei.cangjie.ide.cache.trackers.CangJieCodeBlockModificationListener
import com.huawei.cangjie.ide.indices.CangJiePackageIndexUtils
import com.huawei.cangjie.name.FqName
import com.huawei.cangjie.psi.CjFile
import com.huawei.cangjie.resolve.lazy.data.CjClassLikeInfo
import com.huawei.cangjie.resolve.lazy.declarations.AbstractDeclarationProviderFactory
import com.huawei.cangjie.resolve.lazy.declarations.CombinedPackageMemberDeclarationProvider
import com.huawei.cangjie.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import com.huawei.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider
import com.huawei.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import com.huawei.cangjie.resolve.lazy.descriptors.PsiBasedClassMemberDeclarationProvider
import com.huawei.cangjie.storage.StorageManager
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope

class PluginDeclarationProviderFactory(
    private val project: Project,
    private val indexedFilesScope: GlobalSearchScope,
    private val storageManager: StorageManager,
    private val nonIndexedFiles: Collection<CjFile>,
    private val moduleInfo: ModuleInfo

) : AbstractDeclarationProviderFactory(storageManager) {

    private val fileBasedDeclarationProviderFactory =
        FileBasedDeclarationProviderFactory(storageManager, nonIndexedFiles)

    override fun packageExists(fqName: FqName) =
        fileBasedDeclarationProviderFactory.packageExists(fqName) || stubBasedPackageExists(fqName)

    private fun stubBasedPackageExists(name: FqName): Boolean {
//return true
        // We're only looking for source-based declarations
        return PerModulePackageCacheService.getInstance(project).packageExists(name, moduleInfo)


    }

    private fun diagnoseMissingPackageFragmentPartialPackageIndexCorruption(message: String): Nothing {
        PerModulePackageCacheService.getInstance(project).onTooComplexChange() // force cache clean up
        throw InconsistencyIndexException("CangJiePartialPackageNamesIndex inconsistency.\n$message")
    }

    private fun diagnoseMissingPackageFragmentPerModulePackageCacheMiss(message: String): Nothing {
        PerModulePackageCacheService.getInstance(project).onTooComplexChange() // Postpone cache rebuild
        throw InconsistencyIndexException("PerModulePackageCache miss.\n$message")
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

    private fun diagnoseMissingPackageFragmentUnknownReason(message: String): Nothing {
        throw IllegalStateException(message)
    }
    private fun oldPackageExists(packageFqName: FqName): Boolean =
       CangJiePackageIndexUtils.packageExists(packageFqName, indexedFilesScope)

    private val onCreationDebugInfo = debugInfo()
    override fun diagnoseMissingPackageFragment(fqName: FqName, file: CjFile?) {
        val moduleSourceInfo = moduleInfo
        val packageExists = CangJiePackageIndexUtils.packageExists(fqName, indexedFilesScope)
        val spiPackageExists = CangJiePackageIndexUtils.packageExists(fqName, project)
        val oldPackageExists = oldPackageExists(fqName)
        val cachedPackageExists =
            moduleSourceInfo.let { project.service<PerModulePackageCacheService>().packageExists(fqName, it) }
//        val moduleModificationCount = moduleSourceInfo?.createModificationTracker()?.modificationCount

        val common = """
                packageExists = $packageExists, cachedPackageExists = $cachedPackageExists,
                oldPackageExists = $oldPackageExists,
                SPI.packageExists = $spiPackageExists,
                OOCB count = ${CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker.modificationCount}
             
            """.trimIndent()
//        moduleModificationCount = $moduleModificationCount

        val message = if (file != null) {
            val virtualFile = file.virtualFile
            val inScope = virtualFile in indexedFilesScope
            val packageFqName = file.packageFqName
            """
                |Cannot find package fragment '$fqName' for file ${file.name}, file package = '$packageFqName':
                |vFile: $virtualFile,
                |nonIndexedFiles = $nonIndexedFiles, isNonIndexed = ${file in nonIndexedFiles},
                |scope = $indexedFilesScope, isInScope = $inScope,
                |$common,
                |packageFqNameByTree = '${file.packageFqNameByTree}', packageDirectiveText = '${file.packageDirective?.text}'
            """.trimMargin()
        } else {
            """
                |Cannot find package fragment '$fqName' for unspecified file:
                |nonIndexedFiles = $nonIndexedFiles,
                |scope = $indexedFilesScope,
                |$common
            """.trimMargin()
        }

        val scopeNotEmptyAndContainsFile =
            !GlobalSearchScope.isEmptyScope(indexedFilesScope) && (file == null || file.virtualFile in indexedFilesScope)

        when {
            scopeNotEmptyAndContainsFile
                    && !packageExists && !oldPackageExists -> diagnoseMissingPackageFragmentPartialPackageIndexCorruption(
                message
            )

            scopeNotEmptyAndContainsFile
                    && packageExists && cachedPackageExists == false -> diagnoseMissingPackageFragmentPerModulePackageCacheMiss(
                message
            )

            else -> diagnoseMissingPackageFragmentUnknownReason(message)
        }
    }

    override fun getClassMemberDeclarationProvider(classLikeInfo: CjClassLikeInfo): ClassMemberDeclarationProvider {
        return PsiBasedClassMemberDeclarationProvider(storageManager, classLikeInfo)

    }

    fun debugToString(): String {
        return arrayOf(
            "PluginDeclarationProviderFactory", "On failure:", debugInfo(), "On creation:", onCreationDebugInfo
        ).joinToString("\n")
    }
}

private class InconsistencyIndexException(message: String) : ProcessCanceledException(message)
