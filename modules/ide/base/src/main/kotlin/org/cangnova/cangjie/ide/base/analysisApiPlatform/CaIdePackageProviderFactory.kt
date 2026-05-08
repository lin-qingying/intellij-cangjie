@file:OptIn(org.cangnova.cangjie.analysis.api.CaPlatformInterface::class)

package org.cangnova.cangjie.ide.base.analysisApiPlatform

import com.intellij.openapi.project.Project
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.psi.PsiDirectory
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiFileSystemItem
import com.intellij.psi.PsiManager
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.analysis.api.decompiled.CaDecompiledBinaryIndex
import org.cangnova.cangjie.analysis.api.platform.packages.CangJieCompositePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJieEmptyPackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProvider
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderFactory
import org.cangnova.cangjie.analysis.api.platform.packages.CangJiePackageProviderMerger
import org.cangnova.cangjie.analysis.api.platform.projectStructure.CaModuleProvider
import org.cangnova.cangjie.analysis.api.projectStructure.CaBuiltinsModule
import org.cangnova.cangjie.analysis.api.projectStructure.CaLibraryModule
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.utils.isCangJieFileType

/**
 * IDE 平台的主包 provider 工厂。
 *
 * 组织方式参考 Kotlin `IdeKotlinPackageProviderFactory`：
 * - source 包名直接来自作用域内源码文件；
 * - compiled/builtins 包名直接来自 binary header；
 * - package provider 只消费 package facts，不先物化 decompiled PSI。
 */
class CaIdePackageProviderFactory(
    private val project: Project,
) : CangJiePackageProviderFactory {
    private val psiManager: PsiManager = PsiManager.getInstance(project)
    private val projectStructureProvider: CaModuleProvider = CaModuleProvider.getInstance(project)
    private val decompiledBinaryIndex: CaDecompiledBinaryIndex
        get() = CaDecompiledBinaryIndex.getInstance(project)

    override fun createPackageProvider(searchScope: GlobalSearchScope): CangJiePackageProvider {
        val matchingPackageNames = linkedSetOf<FqName>()
        collectSourcePackageNames(searchScope, matchingPackageNames)
        collectCompiledPackageNames(searchScope, matchingPackageNames)
        if (matchingPackageNames.isEmpty()) return CangJieEmptyPackageProvider

        return CaIdeScopePackageProvider(matchingPackageNames)
    }

    /**
     * source 侧继续按 project structure 暴露的文件系统项收集真实包名。
     */
    private fun collectSourcePackageNames(
        scope: GlobalSearchScope,
        destination: MutableSet<FqName>,
    ) {
        projectStructureProvider.allSourceFiles.forEach { item ->
            collectSourcePackageNames(item, scope, destination)
        }
    }

    private fun collectSourcePackageNames(
        item: PsiFileSystemItem,
        scope: GlobalSearchScope,
        destination: MutableSet<FqName>,
    ) {
        when (item) {
            is CjFile -> collectSourcePackageName(item, scope, destination)
            is PsiFile -> (item as? CjFile)?.let { collectSourcePackageName(it, scope, destination) }
            is PsiDirectory -> {
                VfsUtilCore.iterateChildrenRecursively(item.virtualFile, null) { virtualFile ->
                    if (virtualFile.isDirectory || !virtualFile.isCangJieFileType()) {
                        return@iterateChildrenRecursively true
                    }
                    if (!scope.contains(virtualFile)) {
                        return@iterateChildrenRecursively true
                    }
                    (psiManager.findFile(virtualFile) as? CjFile)?.let { file ->
                        destination += file.packageFqName
                    }
                    true
                }
            }
        }
    }

    private fun collectSourcePackageName(
        file: CjFile,
        scope: GlobalSearchScope,
        destination: MutableSet<FqName>,
    ) {
        val virtualFile = file.virtualFile
        if (virtualFile == null || scope.contains(virtualFile)) {
            destination += file.packageFqName
        }
    }

    /**
     * compiled/builtins 侧必须直接从 binary header 读取包名，
     * 不能先恢复 decompiled `CjFile` 再取 `packageFqName`。
     */
    private fun collectCompiledPackageNames(
        scope: GlobalSearchScope,
        destination: MutableSet<FqName>,
    ) {
        projectStructureProvider.allModules.forEach { module ->
            when (module) {
                is CaLibraryModule -> {
                    decompiledBinaryIndex.getBinaryFiles(module)
                        .asSequence()
                        .filter(scope::contains)
                        .mapNotNull(decompiledBinaryIndex::readPackageFqName)
                        .forEach(destination::add)
                }

                is CaBuiltinsModule -> {
                    decompiledBinaryIndex.getBinaryFiles(module)
                        .asSequence()
                        .filter(scope::contains)
                        .mapNotNull(decompiledBinaryIndex::readPackageFqName)
                        .forEach(destination::add)
                }
            }
        }
    }
}

/**
 * IDE 平台的包 provider merger。
 */
class CaIdePackageProviderMerger : CangJiePackageProviderMerger {
    override fun merge(providers: List<CangJiePackageProvider>): CangJiePackageProvider {
        return CangJieCompositePackageProvider.create(providers)
    }
}

/**
 * 当前作用域下的包视图。
 *
 * 只接受已经归一化好的 package facts，
 * 并把父包链显式补齐，保证 `doesPackageExist` 与 `getSubpackageNames` 的判断一致。
 */
private class CaIdeScopePackageProvider(
    matchingPackageNames: Set<FqName>,
) : CangJiePackageProvider {
    private val kotlinPackageToSubpackages: Map<FqName, Set<Name>> = run {
        val packages = linkedMapOf<FqName, MutableSet<Name>>()
        for (packageName in matchingPackageNames) {
            var currentPackage = FqName.ROOT
            for (subpackage in packageName.pathSegments()) {
                packages.getOrPut(currentPackage, ::linkedSetOf).add(subpackage)
                currentPackage = currentPackage.child(subpackage)
            }
            packages.computeIfAbsent(currentPackage) { linkedSetOf() }
        }
        packages
    }

    override fun doesPackageExist(packageFqName: FqName): Boolean {
        return packageFqName.isRoot || packageFqName in kotlinPackageToSubpackages
    }

    override fun getSubpackageNames(packageFqName: FqName): Set<Name> {
        return kotlinPackageToSubpackages[packageFqName].orEmpty()
    }
}
