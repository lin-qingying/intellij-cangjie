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

package org.cangnova.cangjie.moduleinfo.cache

import com.intellij.openapi.Disposable
import com.intellij.openapi.application.readAction
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.module.Module
import com.intellij.openapi.module.ModuleManager
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.LibraryOrderEntry
import com.intellij.openapi.roots.ModuleRootManager
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.util.Disposer
import com.intellij.openapi.util.SimpleModificationTracker
import com.intellij.platform.backend.workspace.WorkspaceModel
import com.intellij.platform.workspace.jps.entities.ModuleEntity
import com.intellij.platform.workspace.jps.entities.SourceRootEntity
import com.intellij.platform.workspace.storage.VersionedStorageChange
import com.intellij.psi.util.CachedValue
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collectIndexed
import kotlinx.coroutines.launch
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.moduleinfo.LibraryInfo
import org.cangnova.cangjie.moduleinfo.ModuleSourceInfo
import org.cangnova.cangjie.moduleinfo.checkValidity
import org.cangnova.cangjie.moduleinfo.sourceModuleInfos
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachments

/**
 * 模块信息缓存接口
 *
 * 提供对项目中所有模块信息的高效访问和缓存机制。
 */
interface IdeaModelInfosCache {
    /**
     * 获取项目中的所有模块信息
     *
     * @return 所有模块信息的列表，包括源码模块和库模块
     */
    fun allModules(): List<IdeaModuleInfo>

    /**
     * 获取指定模块的源码模块信息
     *
     * @param module IntelliJ 模块
     * @return 该模块的所有源码模块信息
     */
    fun getModuleInfosForModule(module: Module): Collection<ModuleSourceInfo>

    /**
     * 获取指定库的库信息
     *
     * @param library IntelliJ 库
     * @return 该库的所有库信息
     */
    fun getLibraryInfosForLibrary(library: Library): Collection<LibraryInfo>
}

/**
 * 细粒度的模块信息缓存实现
 *
 * 这个实现使用细粒度的缓存机制，能够：
 * 1. 监听工作空间模型变化，自动更新缓存
 * 2. 按需计算和缓存模块信息
 * 3. 支持增量更新，避免全量重建
 *
 * @param project 当前项目
 */
internal class FineGrainedIdeaModelInfosCache(private val project: Project) : IdeaModelInfosCache, Disposable {
    /**
     * 协程作用域，用于订阅工作空间模型变化
     */
    @Suppress("EXPOSED_PROPERTY_TYPE")
    val coroutineScope: CoroutineScope = CoroutineScope(SupervisorJob())

    /**
     * 模块缓存，存储模块到源码模块信息的映射
     */
    private val moduleCache = ModuleCache()

    /**
     * 所有模块信息的缓存值（包含源码模块）
     */
    private val modulesCache: CachedValue<List<IdeaModuleInfo>>

    /**
     * 所有库信息的缓存值
     */
    private val libraries: CachedValue<Collection<LibraryInfo>>

    /**
     * 修改追踪器，用于触发缓存失效
     */
    private val modificationTracker = SimpleModificationTracker()

    init {
        val cachedValuesManager = CachedValuesManager.getManager(project)

        // 初始化模块缓存
        modulesCache = cachedValuesManager.createCachedValue {
            val ideaModuleInfos = moduleCache.fetchValues().flatten().also {
                it.checkValidity { "modulesCache: modules calculation" }
            }
            CachedValueProvider.Result.create(ideaModuleInfos, modificationTracker)
        }

        // 初始化库缓存
        libraries = cachedValuesManager.createCachedValue {
            val libraryCache = LibraryInfoCache.getInstance(project)
            val collectedLibraries = mutableSetOf<LibraryInfo>()

            // 遍历所有模块，收集库信息
            for (module in ModuleManager.getInstance(project).modules) {
                ProgressManager.checkCanceled()
                for (entry in ModuleRootManager.getInstance(module).orderEntries) {
                    if (entry !is LibraryOrderEntry) continue
                    val library = entry.library ?: continue
                    collectedLibraries += libraryCache[library]
                }
            }

            collectedLibraries.checkValidity { "libraries calculation" }

            CachedValueProvider.Result.create(
                collectedLibraries,
                libraryCache.removedLibraryInfoTracker(),
                modificationTracker
            )
        }

        // 注册资源清理
        Disposer.register(this, moduleCache)
    }

    override fun dispose() {
        // 取消所有正在运行的协程
        coroutineScope.cancel()
    }

    /**
     * 抽象缓存基类
     *
     * 提供了通用的缓存机制，包括：
     * 1. 延迟初始化
     * 2. 工作空间模型变化监听（通过 Kotlin Flow）
     * 3. 增量更新支持
     *
     * @param Key 缓存键类型
     * @param Value 缓存值类型
     * @param initializer 初始化函数，用于预加载缓存数据
     */
    abstract inner class AbstractCache<Key : Any, Value : Any>(initializer: (AbstractCache<Key, Value>) -> Unit) :
        SynchronizedFineGrainedEntityCache<Key, Value>(project) {

        @Volatile
        private var initializerRef: ((AbstractCache<Key, Value>) -> Unit)? = initializer
        private val initializerLock = Any()

        init {
            initialize()
        }

        /**
         * 订阅工作空间模型变化事件
         *
         * 使用推荐的 WorkspaceModel.eventLog + Kotlin Flow API 来监听模块变化，
         * 这是 IntelliJ Platform 文档推荐的公共 API 方式。
         *
         * @see <a href="https://plugins.jetbrains.com/docs/intellij/workspace-model-event-listening.html">Workspace Model Event Listening</a>
         */
        override fun subscribe() {
            val workspaceModel = WorkspaceModel.getInstance(project)

            this@FineGrainedIdeaModelInfosCache.coroutineScope.launch {
                workspaceModel.eventLog.collectIndexed { index, event ->
                    if (index == 0) {
                        // 首次事件：初始化缓存（在 read action 中执行）
                        readAction {
                            initializeCache()
                        }
                    } else {
                        // 后续事件：增量更新缓存（在 read action 中执行）
                        readAction {
                            handleWorkspaceChange(event)
                        }
                    }
                }
            }
        }

        /**
         * 初始化缓存
         *
         * 在第一次接收到工作空间事件时调用，用于预加载所有模块信息
         */
        private fun initializeCache() {
            if (initializerRef != null) {
                synchronized(initializerLock) {
                    initializerRef?.let { it(this) }
                    initializerRef = null
                }
            }
        }

        /**
         * 处理工作空间模型变化
         *
         * 当工作空间模型发生变化时，清空缓存并触发修改计数，
         * 让缓存在下次访问时重新计算。
         *
         * @param event 工作空间变化事件
         */
        private fun handleWorkspaceChange(event: VersionedStorageChange) {
            if (initializerRef != null) return // 缓存尚未初始化，忽略变化

            val moduleChanges = event.getChanges(ModuleEntity::class.java)
            val sourceRootChanges = event.getChanges(SourceRootEntity::class.java)
            if (moduleChanges.isEmpty() && sourceRootChanges.isEmpty()) return

            // 清空缓存（不需要 write action）
            invalidate()

            // 通知依赖此缓存的其他组件刷新
            incModificationCount()
        }

        /**
         * 根据模块名查找模块实例
         *
         * 使用公共 API ModuleManager.findModuleByName()
         *
         * @param moduleName 模块名称
         * @return 模块实例，如果不存在则返回 null
         */
        private fun findModuleByName(moduleName: String): Module? {
            return ModuleManager.getInstance(project).findModuleByName(moduleName)
        }

        /**
         * 获取所有缓存值
         *
         * 如果缓存尚未初始化，会先执行初始化函数
         */
        fun fetchValues(): Collection<Value> {
            initializeCache()
            return values()
        }
    }

    /**
     * 模块缓存实现
     *
     * 缓存模块到源码模块信息的映射，监听模块根变化
     */
    inner class ModuleCache : AbstractCache<Module, List<ModuleSourceInfo>>(
        initializer = {
            // 预加载所有模块的信息
            project.ideaModules().forEach(it::get)
        }) {

        /**
         * 计算给定模块的源码模块信息
         */
        override fun calculate(key: Module): List<ModuleSourceInfo> = key.sourceModuleInfos

        /**
         * 检查模块键的有效性
         */
        override fun checkKeyValidity(key: Module) {
            key.checkValidity()
        }
    }

    /**
     * 增加修改计数，触发缓存失效
     */
    private fun incModificationCount() {
        modificationTracker.incModificationCount()
    }

    /**
     * 获取所有模块信息（包括源码模块和库模块）
     */
    override fun allModules(): List<IdeaModuleInfo> = (modulesCache.value + libraries.value).also {
        it.checkValidity { "allModules" }
    }

    /**
     * 获取指定模块的源码模块信息
     */
    override fun getModuleInfosForModule(module: Module): Collection<ModuleSourceInfo> = moduleCache[module]

    /**
     * 获取指定库的库信息
     */
    override fun getLibraryInfosForLibrary(library: Library): Collection<LibraryInfo> =
        LibraryInfoCache.getInstance(project)[library]
}

fun Project.ideaModules(): Array<out Module> = runReadAction { ModuleManager.getInstance(this).modules }

fun Collection<IdeaModuleInfo>.checkValidity(lazyMessage: () -> String) {
    val disposed = filter {
        when (it) {
            is ModuleSourceInfo -> it.module.isDisposed
            is LibraryInfo -> it.library.isDisposed
            else -> false
        }
    }
    if (disposed.isNotEmpty()) {
        throw CangJieExceptionWithAttachments(lazyMessage())
            .withAttachment("disposedInfos.txt", disposed.joinToString("\n") { it.name.asString() })
    }
}