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

package org.cangnova.cangjie.cjo

import com.intellij.openapi.Disposable
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ModuleRootEvent
import com.intellij.openapi.roots.ModuleRootListener
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.roots.libraries.LibraryTablesRegistrar
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import kotlinx.coroutines.CoroutineScope
import org.cangnova.cangjie.metadata.model.wrapper.PackageWrapper
import org.cangnova.cangjie.name.FqName
import java.util.concurrent.ConcurrentHashMap

/**
 * CJO 包元数据服务实现类
 *
 * 内部实现类，不应直接使用。请使用 [CjoPackageService] 接口。
 *
 * @see CjoPackageService
 */
internal class CjoPackageServiceImpl(
    private val project: Project,
    @Suppress("UNUSED_PARAMETER") private val cs: CoroutineScope
) : CjoPackageService, Disposable, LibraryChangeCallback {

    /**
     * 包名到 VirtualFile 的映射
     */
    private val packageFileMap = ConcurrentHashMap<FqName, VirtualFile>()

    /**
     * 已加载的包缓存（非文件绑定的包）
     */
    private val directPackageCache = ConcurrentHashMap<FqName, PackageWrapper>()

    /**
     * 库名到包名列表的映射（用于库移除时的高效清理）
     */
    private val libraryPackagesMap = ConcurrentHashMap<String, MutableSet<FqName>>()

    /**
     * 库扫描器
     */
    private val libraryScanner: CjoLibraryScanner = DefaultCjoLibraryScanner.INSTANCE

    init {
        // 扫描当前所有已存在的库（初始化索引）
        buildInitialIndex()

        // 订阅模块根变更事件（监听所有库的变化，包括模块级库）
        project.messageBus.connect(this).subscribe(
            com.intellij.openapi.roots.ModuleRootListener.TOPIC,
            object : ModuleRootListener {
                override fun rootsChanged(event: ModuleRootEvent) {
                    // 当模块根变更时，重新构建索引
                    // 这会捕获所有库的添加、移除、修改事件（包括模块级库）
                    LOG.debug("Module roots changed, rebuilding CJO index")
                    rebuildIndex()
                }
            }
        )
    }

    /**
     * 构建初始索引
     *
     * 在服务启动时扫描所有已存在的库
     */
    private fun buildInitialIndex() {
        // 扫描所有模块的库（包括模块级库）
        val moduleManager = ModuleManager.getInstance(project)
        for (module in moduleManager.modules) {
            val moduleRootManager = ModuleRootManager.getInstance(module)
            for (orderEntry in moduleRootManager.orderEntries) {
                if (orderEntry is com.intellij.openapi.roots.LibraryOrderEntry) {
                    val library = orderEntry.library
                    if (library != null) {
                        LOG.debug("Initial scan (module ${module.name}): ${library.name}")
                        scanAndRegisterLibrary(library)
                    }
                }
            }
        }

        // 扫描项目级库
        val projectLibraryTable = LibraryTablesRegistrar.getInstance().getLibraryTable(project)
        for (library in projectLibraryTable.libraries) {
            LOG.debug("Initial scan (project): ${library.name}")
            scanAndRegisterLibrary(library)
        }

        // 扫描应用级库
        val applicationLibraryTable = LibraryTablesRegistrar.getInstance().libraryTable
        for (library in applicationLibraryTable.libraries) {
            LOG.debug("Initial scan (application): ${library.name}")
            scanAndRegisterLibrary(library)
        }

        LOG.info("Initial CJO index built, ${packageFileMap.size} packages registered")
    }

    /**
     * 重建索引
     *
     * 当模块根变更时调用，完全清除旧索引并重新构建
     */
    private fun rebuildIndex() {
        // 清除所有缓存
        packageFileMap.clear()
        directPackageCache.clear()  // 也清除 directPackageCache，因为会重新填充
        libraryPackagesMap.clear()

        // 重新构建
        buildInitialIndex()
    }

    override fun getPackage(packageFqName: FqName): PackageWrapper? {
        // 1. 检查直接缓存
        directPackageCache[packageFqName]?.let { return it }

        // 2. 检查已注册的文件映射
        packageFileMap[packageFqName]?.let { file ->
            return getPackageFromFile(file)
        }

        // 3. 查询扩展点提供者
        return getPackageFromProviders(packageFqName)
    }

    /**
     * 从扩展点提供者获取包
     */
    private fun getPackageFromProviders(packageFqName: FqName): PackageWrapper? {
        val providers = CjoProvider.EP_NAME.extensionList
            .filter { it.isApplicable(project) }
            .sortedByDescending { it.priority }

        for (provider in providers) {
            val file = provider.getPackageFile(project, packageFqName)
            if (file != null && file.isValid) {
                LOG.debug("Found package $packageFqName from provider: ${provider.displayName}")
                return getPackageFromFile(file)
            }
        }

        return null
    }

    override fun getPackageFromFile(virtualFile: VirtualFile): PackageWrapper? {
        if (!virtualFile.isValid) return null

        return CachedValuesManager.getManager(project).getCachedValue(
            virtualFile,
            PACKAGE_CACHE_KEY,
            {
                val wrapper = CjoFileLoader.loadFromFile(virtualFile)
                CachedValueProvider.Result.create(
                    wrapper,
                    virtualFile,
                    ProjectRootModificationTracker.getInstance(project)
                )
            },
            false
        )
    }

    override fun registerPackageFile(packageFqName: FqName, virtualFile: VirtualFile) {
        packageFileMap[packageFqName] = virtualFile
    }

    override fun registerPackage(packageWrapper: PackageWrapper) {
        directPackageCache[packageWrapper.packageName] = packageWrapper
    }

    override fun getAllPackageNames(): Set<FqName> {
        val result = mutableSetOf<FqName>()
        result.addAll(directPackageCache.keys)
        result.addAll(packageFileMap.keys)

        // 从扩展点获取
        CjoProvider.EP_NAME.extensionList
            .filter { it.isApplicable(project) }
            .forEach { provider ->
                result.addAll(provider.getAvailablePackages(project))
            }

        return result
    }

    override fun hasPackage(packageFqName: FqName): Boolean {
        if (directPackageCache.containsKey(packageFqName)) return true
        if (packageFileMap.containsKey(packageFqName)) return true

        // 检查扩展点提供者
        return CjoProvider.EP_NAME.extensionList
            .filter { it.isApplicable(project) }
            .sortedByDescending { it.priority }
            .any { it.getPackageFile(project, packageFqName) != null }
    }

    override fun invalidateCache(packageFqName: FqName) {
        directPackageCache.remove(packageFqName)
    }

    override fun clearCache() {
        directPackageCache.clear()
        packageFileMap.clear()
        libraryPackagesMap.clear()
    }

    override fun resolveFullId(
        fullId: org.cangnova.cangjie.metadata.model.fb.FbFullId,
        currentPackage: PackageWrapper
    ): NameResolveResult {
        val resolver = CjoFullIdResolver.getInstance(project)
        return resolver.resolveName(fullId, currentPackage)
    }

    // ========== 库缓存清理方法 ==========

    /**
     * 通过库名移除相关的缓存（用于库已被删除的情况）
     */
    private fun onLibraryRemovedByName(libraryName: String) {
        // 使用 libraryPackagesMap 精确清理
        val packages = libraryPackagesMap.remove(libraryName) ?: return
        packages.forEach { packageFqName ->
            packageFileMap.remove(packageFqName)
            directPackageCache.remove(packageFqName)
        }
        LOG.debug("Removed ${packages.size} CJO entries for library $libraryName")
    }

    // ========== LibraryChangeCallback 实现 ==========

    /**
     * 扫描库并注册其中的 CJO 文件
     *
     * 扫描器会解析 CJO 文件获取真正的包名，并缓存 PackageWrapper 对象
     */
    private fun scanAndRegisterLibrary(library: Library) {
        val libraryName = library.name ?: return
        val cjoFiles = libraryScanner.scanLibrary(library)

        // 记录该库包含的所有包名
        val packages = libraryPackagesMap.getOrPut(libraryName) {
            java.util.concurrent.ConcurrentHashMap.newKeySet()
        }

        for (info in cjoFiles) {
            // 包名已经由扫描器从 CJO 文件中解析获取
            registerPackageFile(info.packageFqName, info.file)
            // 同时缓存 PackageWrapper 对象
            directPackageCache[info.packageFqName] = info.packageWrapper
            packages.add(info.packageFqName)
        }
        LOG.debug("Scanned library $libraryName, found ${cjoFiles.size} CJO packages")
    }

    override fun onLibraryAdded(library: Library) {
        scanAndRegisterLibrary(library)
    }

    override fun onLibraryRemoved(library: Library) {
        val libraryName = library.name ?: return
        onLibraryRemovedByName(libraryName)
    }

    override fun onLibraryUpdated(library: Library) {
        val libraryName = library.name ?: return
        onLibraryRemovedByName(libraryName)
        scanAndRegisterLibrary(library)
    }

    override fun dispose() {
        clearCache()
    }

    companion object {
        private val LOG = Logger.getInstance(CjoPackageServiceImpl::class.java)

        private val PACKAGE_CACHE_KEY = Key.create<CachedValue<PackageWrapper?>>(
            "CjoPackageService.PackageWrapper"
        )
    }
}