@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledPsiProvider
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.lang.declarations.CangJieBuiltInFileType
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.isCangJieFileType

/**
 * 按作用域收集 IDE 可见的仓颉文件。
 *
 * 组织方式对齐 Kotlin Analysis API 的平台层：
 * - declaration/package/annotation provider 不各自发明文件发现逻辑；
 * - source 侧只读取 project-structure 暴露的 source items；
 * - compiled 侧只走 decompiled index + decompiled PSI。
 */
internal class CaIdeScopeCangJieFileCollector(
    private val project: Project,
) {
    private val psiManager: PsiManager = PsiManager.getInstance(project)
    private val projectStructureProvider: CaModuleProvider = CaModuleProvider.getInstance(project)
    private val decompiledBinaryIndex: CaDecompiledBinaryIndex? = runCatching { CaDecompiledBinaryIndex.getInstance(project) }.getOrNull()
    private val decompiledPsiProvider: CaDecompiledPsiProvider? = runCatching { CaDecompiledPsiProvider.getInstance(project) }.getOrNull()

    fun collect(scope: GlobalSearchScope): List<CjFile> {
        val files = linkedSetOf<CjFile>()
        collectSourceFiles(scope, files)
        collectCompiledFiles(scope, files)
        return files.toList()
    }

    private fun collectSourceFiles(scope: GlobalSearchScope, destination: MutableSet<CjFile>) {
        projectStructureProvider.allSourceFiles.forEach { item ->
            collectFromFileSystemItem(item, scope, destination)
        }
    }

    private fun collectCompiledFiles(scope: GlobalSearchScope, destination: MutableSet<CjFile>) {
        val binaryIndex = decompiledBinaryIndex ?: return
        val psiProvider = decompiledPsiProvider ?: return

        projectStructureProvider.allModules.forEach { module ->
            when (module) {
                is CaLibraryModule -> {
                    binaryIndex.getBinaryFiles(module)
                        .asSequence()
                        .filter(scope::contains)
                        .mapNotNull(psiProvider::getDecompiledFile)
                        .forEach(destination::add)
                }

                is CaBuiltinsModule -> {
                    binaryIndex.getBinaryFiles(module)
                        .asSequence()
                        .filter(scope::contains)
                        .mapNotNull(psiProvider::getDecompiledFile)
                        .forEach(destination::add)
                }
            }
        }
    }

    private fun collectFromFileSystemItem(
        item: PsiFileSystemItem,
        scope: GlobalSearchScope,
        destination: MutableSet<CjFile>,
    ) {
        when (item) {
            is CjFile -> collectFile(item, scope, destination)
            is PsiFile -> (item as? CjFile)?.let { collectFile(it, scope, destination) }
            is PsiDirectory -> {
                VfsUtilCore.iterateChildrenRecursively(item.virtualFile, null) { virtualFile ->
                    if (virtualFile.isDirectory || !virtualFile.isCangJieScopeCandidate()) {
                        return@iterateChildrenRecursively true
                    }
                    if (!scope.contains(virtualFile)) {
                        return@iterateChildrenRecursively true
                    }
                    (psiManager.findFile(virtualFile) as? CjFile)?.let(destination::add)
                    true
                }
            }
        }
    }

    private fun collectFile(file: CjFile, scope: GlobalSearchScope, destination: MutableSet<CjFile>) {
        val virtualFile = file.virtualFile
        if (virtualFile == null || scope.contains(virtualFile)) {
            destination += file
        }
    }

    private fun VirtualFile.isCangJieScopeCandidate(): Boolean {
        return fileType == CangJieBuiltInFileType ||
            extension.equals("cjo", ignoreCase = true) ||
            isCangJieFileType()
    }
}
