/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.stubindex.resolve

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.psi.CjFile
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.descriptors.PackageMemberDeclarationProvider
import org.cangnova.cangjie.descriptors.data.CjClassLikeInfo
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleInfo
import org.cangnova.cangjie.moduleinfo.ModuleSourceInfo
import org.cangnova.cangjie.moduleinfo.util.projectSourceModules
import org.cangnova.cangjie.resolve.caches.PerModulePackageCacheService
import org.cangnova.cangjie.resolve.lazy.declarations.AbstractDeclarationProviderFactory
import org.cangnova.cangjie.resolve.lazy.declarations.CombinedPackageMemberDeclarationProvider
import org.cangnova.cangjie.resolve.lazy.declarations.FileBasedDeclarationProviderFactory
import org.cangnova.cangjie.resolve.lazy.descriptors.ClassMemberDeclarationProvider
import org.cangnova.cangjie.resolve.lazy.descriptors.PsiBasedClassMemberDeclarationProvider
import org.cangnova.cangjie.storage.StorageManager
import org.cangnova.cangjie.cjo.CjoPackageService
import org.cangnova.cangjie.stubindex.CangJiePackageIndexUtils

class PluginDeclarationProviderFactory(
    private val project: Project,
    private val indexedFilesScope: GlobalSearchScope,
    private val storageManager: StorageManager,
    private val nonIndexedFiles: Collection<CjFile>,
    private val context: ModuleInfo

) : AbstractDeclarationProviderFactory(storageManager) {

    private val fileBasedDeclarationProviderFactory =
        FileBasedDeclarationProviderFactory(storageManager, nonIndexedFiles)

    override fun packageExists(fqName: FqName) =
        fileBasedDeclarationProviderFactory.packageExists(fqName) || stubBasedPackageExists(fqName)

    private fun stubBasedPackageExists(name: FqName): Boolean {
        // 首先尝试通过项目源码模块检查
        val sourceModuleExists = (context as? IdeaModuleInfo)?.projectSourceModules()
            ?.any { PerModulePackageCacheService.getInstance(project).packageExists(name, it) }
            ?: false

        if (sourceModuleExists) {
            return true
        }

        // 对于库模块（非 ModuleSourceInfo），使用 CjoPackageService 检查包是否存在
        // 这解决了库的 .cjo 文件不在 CangJiePartialPackageNamesIndex 中的问题
        if (context !is ModuleSourceInfo) {
            return CjoPackageService.getInstance(project).hasPackage(name)
        }

        return false
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
        val moduleSourceInfo = context as? ModuleSourceInfo

        val packageExists = CangJiePackageIndexUtils.packageExists(fqName, indexedFilesScope)
        val spiPackageExists = CangJiePackageIndexUtils.packageExists(fqName, project)
        val oldPackageExists = oldPackageExists(fqName)
        val cachedPackageExists =
            moduleSourceInfo?.let { project.service<PerModulePackageCacheService>().packageExists(fqName, it) }

        val moduleModificationCount = moduleSourceInfo?.createModificationTracker()?.modificationCount


        val common = """
                packageExists = $packageExists, cachedPackageExists = $cachedPackageExists,
                oldPackageExists = $oldPackageExists,
                SPI.packageExists = $spiPackageExists,
                context = ${context.name}

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
