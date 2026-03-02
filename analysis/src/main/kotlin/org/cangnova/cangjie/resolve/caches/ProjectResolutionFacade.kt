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

package org.cangnova.cangjie.resolve.caches

import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.cangnova.cangjie.cache.trackers.CangJieCodeBlockModificationListener
import org.cangnova.cangjie.context.GlobalContextImpl
import org.cangnova.cangjie.context.withProject
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.CjProjectDescriptorService
import org.cangnova.cangjie.diagnostics.DiagnosticSink
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.moduleinfo.NotUnderContentRootModuleInfo
import org.cangnova.cangjie.moduleinfo.cache.checkValidity
import org.cangnova.cangjie.moduleinfo.provider.ModuleInfoProvider
import org.cangnova.cangjie.moduleinfo.provider.moduleInfo
import org.cangnova.cangjie.moduleinfo.util.getModuleInfosFromIdeaModel
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.AnalysisResult
import org.cangnova.cangjie.resolve.CangJieModificationTrackerService
import org.cangnova.cangjie.resolve.CompositeBindingContext
import org.cangnova.cangjie.resolve.EmptyResolverForProject
import org.cangnova.cangjie.resolve.ResolverForModule
import org.cangnova.cangjie.resolve.ResolverForProject
import org.cangnova.cangjie.storage.CancellableSimpleLock
import org.cangnova.cangjie.storage.guarded
import org.cangnova.cangjie.utils.firstIsInstanceOrNull
import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.ReentrantLock


/**
 * 项目解析门面
 *
 * 这是仓颉语言分析系统的高层 API 入口，为 IDE 提供代码分析、符号解析和类型检查服务。
 * 它封装了底层的解析器管理、缓存策略和并发控制，提供简单易用的接口。
 *
 * ## 核心职责
 *
 * 1. **解析器管理**: 创建和缓存 [IdeaResolverForProject] 实例
 * 2. **分析结果缓存**: 缓存文件级和元素级的分析结果
 * 3. **依赖追踪**: 追踪代码修改，自动失效过期缓存
 * 4. **并发控制**: 保证多线程环境下的数据一致性
 * 5. **解析器复用**: 通过分层架构复用已有的解析结果
 *
 * ## 分层架构
 *
 * ```
 * ┌─────────────────────────────────────────┐
 * │  ProjectResolutionFacade (门面层)       │  ← 提供高层 API
 * │  ├─ 缓存管理                            │
 * │  ├─ 依赖追踪                            │
 * │  └─ 并发控制                            │
 * └──────────────┬──────────────────────────┘
 *                │
 *                ↓
 * ┌─────────────────────────────────────────┐
 * │  IdeaResolverForProject (解析器层)      │  ← 管理模块解析器
 * │  └─ 每个模块一个 ResolverForModule      │
 * └──────────────┬──────────────────────────┘
 *                │
 *                ↓
 * ┌─────────────────────────────────────────┐
 * │  ResolverForModule (模块解析器层)       │  ← 执行实际分析
 * │  ├─ PackageFragmentProvider             │
 * │  ├─ ComponentProvider                    │
 * │  └─ LazyResolveSession                   │
 * └─────────────────────────────────────────┘
 * ```
 *
 * ## 三种门面实例
 *
 * 根据使用场景，系统中会存在三种 ProjectResolutionFacade 实例：
 *
 * ### 1. facadeForLibraries - 库解析门面
 * ```kotlin
 * ProjectResolutionFacade(
 *     debugString = "facadeForLibraries",
 *     resolverDebugName = "project libraries [3 libraries: stdlib, std.collection, std.io]",
 *     reuseDataFrom = null,  // 不依赖其他门面
 *     moduleFilter = { it.isLibraryContext },
 *     invalidateOnOOCB = false  // 库不会因代码修改而失效
 * )
 * ```
 * **特点**:
 * - 只处理外部依赖库（stdlib 等）
 * - 长期缓存，不会因用户代码修改而失效
 * - 作为基础层，被其他门面复用
 *
 * ### 2. facadeForModules - 模块解析门面
 * ```kotlin
 * ProjectResolutionFacade(
 *     debugString = "facadeForModules",
 *     resolverDebugName = "project source roots and libraries [...]",
 *     reuseDataFrom = facadeForLibraries,  // 复用库解析结果
 *     moduleFilter = { true },  // 处理所有模块
 *     invalidateOnOOCB = true  // 代码修改后失效
 * )
 * ```
 * **特点**:
 * - 处理项目源码 + 所有依赖库
 * - 复用 facadeForLibraries 的结果
 * - 代码修改后自动失效重建
 *
 * ### 3. facadeForSpecialContext - 特殊场景门面
 * ```kotlin
 * ProjectResolutionFacade(
 *     debugString = "facadeForSpecialContext (Library or NotUnderContentRoot)",
 *     resolverDebugName = "completion/highlighting in scratch.cj",
 *     syntheticFiles = setOf(scratchFile),  // 临时文件
 *     reuseDataFrom = facadeForLibraries,  // 根据文件类型选择
 *     invalidateOnOOCB = true
 * )
 * ```
 * **特点**:
 * - 处理临时文件（Scratch、代码片段等）
 * - 根据文件类型复用合适的基础门面
 * - 针对特定文件创建轻量级解析器
 *
 * ## 缓存机制
 *
 * ### 两级缓存架构
 *
 * ```
 * ┌────────────────────────────────────────────┐
 * │  cachedResolverForProject                  │  ← 第一级：解析器缓存
 * │  (缓存 IdeaResolverForProject 实例)        │
 * │  依赖：dependencies + exceptionTracker     │
 * │        + outOfCodeBlockTracker (可选)      │
 * └──────────────┬─────────────────────────────┘
 *                │
 *                ↓
 * ┌────────────────────────────────────────────┐
 * │  analysisResults                           │  ← 第二级：分析结果缓存
 * │  (缓存每个文件的 PerFileAnalysisCache)     │
 * │  依赖：dependencies + exceptionTracker     │
 * │        + outOfCodeBlockTracker             │
 * └────────────────────────────────────────────┘
 * ```
 *
 * ### 依赖追踪
 *
 * **Out-Of-Code-Block (OOCB) 追踪**:
 * - 监听代码块外的修改（类声明、函数签名等）
 * - 这些修改需要重新分析整个项目
 * - 通过 `invalidateOnOOCB` 参数控制
 *
 * **In-Code-Block 修改**:
 * - 函数体内的修改
 * - 通常只需重新分析当前文件
 * - PerFileAnalysisCache 自动处理
 *
 * ## 并发控制
 *
 * ### 锁机制
 *
 * ```kotlin
 * analysisResultsLock (ReentrantLock)
 *   ↓ 包装
 * analysisResultsSimpleLock (CancellableSimpleLock)
 *   ↓ 提供
 * - 可取消的锁等待
 * - 自动检查 ProgressManager.checkCanceled()
 * - 避免长时间阻塞 UI 线程
 * ```
 *
 * ### 线程安全策略
 *
 * 1. **解析器创建**: 通过 CachedValue + StorageManager 保证单例
 * 2. **分析结果访问**: 通过 analysisResultsSimpleLock 保证互斥
 * 3. **缓存失效**: 通过依赖追踪自动触发
 * 4. **取消检查**: 在锁等待时定期检查用户取消
 *
 * ## 解析器复用机制
 *
 * 通过 [reuseDataFrom] 参数实现分层复用：
 *
 * ```kotlin
 * // 场景：用户打开一个 Scratch 文件
 *
 * 1. 创建特殊场景门面
 *    val scratchFacade = ProjectResolutionFacade(
 *        reuseDataFrom = facadeForModules  // 复用模块门面
 *    )
 *
 * 2. scratchFacade 创建解析器时
 *    IdeaResolverForProject(
 *        delegateResolver = facadeForModules.cachedResolverForProject
 *    )
 *
 * 3. 解析 Scratch 文件中的符号时
 *    - 先在 Scratch 文件自己的作用域查找
 *    - 找不到则委托给 delegateResolver
 *    - delegateResolver 再委托给库解析器
 *    - 最终从 stdlib 中找到符号定义
 *
 * 复用链：scratchFacade → facadeForModules → facadeForLibraries
 * ```
 *
 * ## 性能优化
 *
 * ### 1. 懒加载
 * - 解析器按需创建，不使用的模块不会占用内存
 * - 分析结果延迟计算，只在需要时才解析
 *
 * ### 2. 增量分析
 * - 只重新分析修改的文件
 * - 未修改的文件复用缓存结果
 *
 * ### 3. 并行分析
 * - 多个文件可以并行分析
 * - 通过细粒度锁减少竞争
 *
 * ### 4. 缓存分层
 * - 库解析结果长期缓存
 * - 项目代码短期缓存
 * - 临时文件即时计算
 *
 * ## 使用示例
 *
 * ### 获取元素的分析结果
 * ```kotlin
 * val facade = CangJieCacheServiceImpl.getOrBuildGlobalFacade().facadeForModules
 * val element: CjElement = ...
 *
 * // 获取分析结果
 * val result = facade.getAnalysisResultsForElement(element)
 *
 * // 使用 BindingContext 查询符号信息
 * val descriptor = result.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, element]
 * val type = result.bindingContext.getType(element)
 * ```
 *
 * ### 批量分析多个元素
 * ```kotlin
 * val elements: List<CjElement> = ...
 * val result = facade.getAnalysisResultsForElements(elements)
 *
 * // 使用组合的 BindingContext
 * elements.forEach { element ->
 *     val descriptor = result.bindingContext[BindingContext.DECLARATION_TO_DESCRIPTOR, element]
 * }
 * ```
 *
 * ### 获取元素的解析器
 * ```kotlin
 * val element: PsiElement = ...
 * val resolver = facade.resolverForElement(element)
 *
 * // 使用解析器查找包片段
 * val packageFragments = resolver.packageFragmentProvider
 *     .getPackageFragments(FqName("std.collection"))
 * ```
 *
 * ## 错误处理
 *
 * ### 缓存失效期间的访问
 * ```kotlin
 * try {
 *     perFileCache.getAnalysisResults(element)
 * } catch (e: IllegalStateException) {
 *     // 捕获 "Cache has been invalidated during performing analysis" 异常
 *     // 重新获取缓存并重试
 * }
 * ```
 *
 * ### 找不到 AnalysisContext
 * ```kotlin
 * val elementContext = element.analysisContext
 * if (elementContext == null) {
 *     // 返回包含错误信息的 AnalysisResult
 *     return AnalysisResult.internalError(
 *         bindingContext,
 *         IllegalStateException("No AnalysisContext for element")
 *     )
 * }
 * ```
 *
 * ## 生命周期
 *
 * 1. **创建**: 由 [CangJieCacheServiceImpl] 在需要时创建
 * 2. **使用**: IDE 各种功能（高亮、补全、导航等）通过门面获取分析结果
 * 3. **失效**: 代码修改触发依赖追踪，缓存自动失效
 * 4. **重建**: 下次访问时自动重新创建
 * 5. **销毁**: 项目关闭时由 IntelliJ 平台自动清理
 *
 * @param debugString 门面的简短调试名称，如 "facadeForLibraries"、"facadeForModules"
 * @param resolverDebugName 解析器的详细调试名称，包含具体的模块和依赖信息
 *                          例如："project libraries [3 libraries: stdlib, std.collection, std.io]"
 * @param project IntelliJ 项目实例
 * @param globalContext 全局上下文，包含 StorageManager 和 ExceptionTracker
 * @param reuseDataFrom 要复用的基础门面，用于实现分层架构
 *                      - null: 不复用（库解析门面）
 *                      - facadeForLibraries: 复用库解析结果（模块解析门面）
 *                      - facadeForModules: 复用模块解析结果（特殊场景门面）
 * @param moduleFilter 模块过滤器，决定此门面处理哪些模块
 *                     - `{ it.isLibraryContext }`: 只处理库模块
 *                     - `{ true }`: 处理所有模块
 *                     - `{ it == specialContext }`: 只处理特定模块
 * @param dependencies 依赖列表，用于追踪缓存失效
 *                     通常包含 ProjectRootModificationTracker
 * @param invalidateOnOOCB 是否在 Out-Of-Code-Block 修改时失效缓存
 *                         - true: 模块解析门面（代码修改后失效）
 *                         - false: 库解析门面（长期缓存）
 * @param syntheticFiles 合成文件集合（Scratch、代码片段等），默认为空
 * @param allModules 所有需要处理的模块上下文，null 表示从 AnalysisContextProvider 获取
 *
 * @see IdeaResolverForProject
 * @see CangJieCacheServiceImpl
 * @see PerFileAnalysisCache
 */
class ProjectResolutionFacade(
    private val debugString: String,
    private val resolverDebugName: String,
    val project: Project,
    val globalContext: GlobalContextImpl,
    val reuseDataFrom: ProjectResolutionFacade?,
    val moduleFilter: (IdeaModuleInfo) -> Boolean,
    dependencies: List<Any>,
    private val invalidateOnOOCB: Boolean,
    val syntheticFiles: Collection<CjFile> = listOf(),
    val allModules: Collection<IdeaModuleInfo>? = null  // null 意味着从 AnalysisContextProvider 获取
) {

    /**
     * 缓存的解析器提供者
     *
     * 使用 IntelliJ 的 CachedValue 机制缓存 [IdeaResolverForProject] 实例。
     *
     * ## 缓存策略
     *
     * **依赖追踪**:
     * - 基础依赖：[resolverForProjectDependencies]（如 ProjectRootModificationTracker）
     * - OOCB 追踪：如果 [invalidateOnOOCB] 为 true，额外添加 outOfCodeBlockTracker
     *
     * **失效条件**:
     * - 项目结构修改（添加/删除文件、修改依赖）
     * - 代码块外修改（类声明、函数签名等）
     * - 异常发生（通过 exceptionTracker 追踪）
     *
     * **重建时机**:
     * - 缓存失效后，下次访问 [cachedResolverForProject] 时自动重建
     *
     * @see computeModuleResolverProvider
     * @see cachedResolverForProject
     */
    private val cachedValue = CachedValuesManager.getManager(project).createCachedValue(
        {
            // 计算并获取模块解析器提供者
            val resolverProvider = computeModuleResolverProvider()

            // 根据invalidateOnOOCB标志决定是否添加超出代码块的依赖
            // 如果需要，将项目依赖解析器和超出代码块的跟踪器合并
            val allDependencies = if (invalidateOnOOCB) {
                resolverForProjectDependencies + CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker
            } else {
                resolverForProjectDependencies
            }

            // 创建并返回缓存值结果，包含解析器提供者和所有依赖
            CachedValueProvider.Result.create(resolverProvider, allDependencies)
        },
        /* trackValue = */ false
    )
    private val analysisResultsLock = ReentrantLock()
    private val resolverForProjectDependencies = dependencies + globalContext.exceptionTracker
    private val cachedResolverForProject: ResolverForProject<IdeaModuleInfo>
        get() = globalContext.storageManager.compute { cachedValue.value }

    private val analysisResultsSimpleLock = CancellableSimpleLock(
        analysisResultsLock,
        checkCancelled = {
            ProgressManager.checkCanceled()
        },
        interruptedExceptionHandler = { throw ProcessCanceledException(it) })

    /**
     * 计算模块解析器提供者
     *
     * 此函数负责构建一个解析器实例，用于解析项目中的模块信息它通过聚合所有模块信息，
     * 过滤和解析这些模块，并考虑合成文件和模块依赖关系来完成这项任务
     *
     * @return ResolverForProject<AnalysisContext> 实例，用于解析模块信息
     */
    private fun computeModuleResolverProvider(): ResolverForProject<IdeaModuleInfo> {
        // 获取项目描述符
        val projectDescriptor = CjProjectDescriptorService.getInstance(project).projectDescriptor

        // 当解析器重建时，需要使 ExtendManager 缓存失效
        // 这样可以确保被删除/注释掉的扩展声明不会继续生效
        // 只在 invalidateOnOOCB 为 true 时清理（表示这是处理源代码的门面，而非库门面）
        if (invalidateOnOOCB) {
            projectDescriptor.extendManager.invalidate()
        }

        // 初始化代理解析器，如果没有重用的数据，则使用空解析器
        val delegateResolverForProject: ResolverForProject<IdeaModuleInfo> =
            reuseDataFrom?.cachedResolverForProject ?: EmptyResolverForProject()

        val allModuleInfos = (allModules ?: getModuleInfosFromIdeaModel(project))
            .toMutableSet().also {
                it.checkValidity {
                    ("allModules".takeIf { allModules != null }
                        ?: "getModuleInfosFromIdeaModel(project )") + toString()
                }
            }

        val syntheticFilesByModule = syntheticFiles.groupBy { it.moduleInfo }
        val syntheticFilesModules = syntheticFilesByModule.keys
        allModuleInfos.addAll(syntheticFilesModules)



        // 根据模块过滤条件过滤解析的模块
        val resolvedModules = allModuleInfos.filter(moduleFilter)
        // 解析模块及其依赖关系
        val resolvedModulesWithDependencies = resolvedModules
        // 返回模块解析器实例
        return IdeaResolverForProject(
            resolverDebugName,
            globalContext.withProject(project),
            projectDescriptor,
            resolvedModulesWithDependencies,
            syntheticFilesByModule,
            delegateResolverForProject,
            CangJieModificationTrackerService.getInstance(project).outOfBlockModificationTracker

        )
    }


    internal fun getResolverForProject(): ResolverForProject<IdeaModuleInfo> = cachedResolverForProject
    internal fun resolverForModuleInfo(context: IdeaModuleInfo) = cachedResolverForProject.resolverForModule(context)


    private val analysisResults = CachedValuesManager.getManager(project).createCachedValue(
        {
            val resolverForProject = cachedResolverForProject

            val results = object : SLRUCache<CjFile, PerFileAnalysisCache>(2, 3) {
                private val lock = ReentrantLock()

                override fun createValue(file: CjFile): PerFileAnalysisCache {
                    val fileContext = file.moduleInfo
                    val componentProvider = if (fileContext != null) {
                        resolverForProject.resolverForModule(fileContext).componentProvider
                    } else {
                        throw IllegalStateException("No AnalysisContext for file: ${file.name}")
                    }
                    return PerFileAnalysisCache(file, componentProvider)
                }

                override fun getIfCached(key: CjFile): PerFileAnalysisCache? {
                    if (lock.tryLock()) {
                        try {
                            return super.getIfCached(key)
                        } finally {
                            lock.unlock()
                        }
                    }
                    return null
                }

                override fun get(key: CjFile): PerFileAnalysisCache {
                    lock.lock()
                    try {
                        val cache = super.get(key)
                        if (cache.isValid) {
                            return cache
                        }
                        remove(key)
                        return super.get(key)
                    } finally {
                        lock.unlock()
                    }
                }

            }


            val allDependencies = resolverForProjectDependencies +
                    CangJieCodeBlockModificationListener.getInstance(project).cangjieOutOfCodeBlockTracker
            CachedValueProvider.Result.create(results, allDependencies)
        }, false
    )

    internal fun findModuleDescriptor(ideaModuleInfo: IdeaModuleInfo): ModuleDescriptor {
        return cachedResolverForProject.descriptorForModule(ideaModuleInfo)
    }

    internal fun getAnalysisResultsForElements(
        elements: Collection<CjElement>,
        callback: DiagnosticSink.DiagnosticsCallback? = null
    ): AnalysisResult {
        assert(elements.isNotEmpty()) { "elements collection should not be empty" }

        val cache = analysisResultsSimpleLock.guarded { analysisResults.value!! }
        val results = elements.map { analysisResultForElement(it, cache, callback) }
        val bindingContext = CompositeBindingContext.create(results.map { it.bindingContext })
        results.firstOrNull { it.isError() }?.let {
            return AnalysisResult.internalError(bindingContext, it.error)
        }

        return AnalysisResult.success(bindingContext, findModuleDescriptor(elements.first().moduleInfo))


    }

    internal fun getAnalysisResultsForElement(
        element: CjElement,
        callback: DiagnosticSink.DiagnosticsCallback? = null
    ): AnalysisResult {
        val cache = analysisResultsSimpleLock.guarded {
            analysisResults.value!!
        }
        val result = analysisResultForElement(element, cache, callback)
        val bindingContext = result.bindingContext
        result.takeIf { it.isError() }?.let {
            return AnalysisResult.internalError(bindingContext, it.error)
        }

        return AnalysisResult.success(bindingContext, findModuleDescriptor(element.moduleInfo))

    }

    private fun analysisResultForElement(
        element: CjElement,
        cache: SLRUCache<CjFile, PerFileAnalysisCache>,
        callback: DiagnosticSink.DiagnosticsCallback?
    ): AnalysisResult {
        val containingCjFile = element.getContainingCjFile()
        val perFileCache = cache[containingCjFile]
        return try {
            perFileCache.getAnalysisResults(element, callback)
        } catch (e: Throwable) {
            if (e is ControlFlowException) {
                throw e
            }
            val actualCache = analysisResultsSimpleLock.guarded {
                analysisResults.upToDateOrNull?.get()
            }
            if (cache !== actualCache) {
                throw IllegalStateException(
                    "Cache has been invalidated during performing analysis for $containingCjFile",
                    e
                )
            }
            throw e
        }
    }

    internal fun fetchAnalysisResultsForElement(element: CjElement): AnalysisResult? {
        val cache: SLRUCache<CjFile, PerFileAnalysisCache>? =
            analysisResultsLock.tryGuarded {
                analysisResults.upToDateOrNull?.get()
            }
        val perFileCache = cache?.getIfCached(element.getContainingCjFile())
        return perFileCache?.fetchAnalysisResults(element)
    }

    internal fun resolverForElement(element: PsiElement): ResolverForModule {
        val moduleInfos = mutableSetOf<IdeaModuleInfo>()

        // 尝试从文件获取上下文
        val containingFile = element.containingFile
        val elementModuleInfos = ModuleInfoProvider.getInstance(element.project).collect(
            element,
            config =  ModuleInfoProvider.Configuration.Default,
        )

        for (result in elementModuleInfos) {
            val moduleInfo = result.getOrNull()
            if (moduleInfo != null) {
                val resolver = cachedResolverForProject.tryGetResolverForModule(moduleInfo)
                if (resolver != null) {
                    return resolver
                } else {
                    moduleInfos += moduleInfo
                }
            }

            val error = result.exceptionOrNull()
            if (error != null) {
                LOG.warn("Could not find correct module information", error)
            }
        }
        val cjFile = containingFile as? CjFile
        return cachedResolverForProject.tryGetResolverForModule(NotUnderContentRootModuleInfo(project, cjFile))
            ?: cachedResolverForProject.diagnoseUnknownModuleInfo(moduleInfos.toList())
    }

    companion object {
        private val LOG = Logger.getInstance(ProjectResolutionFacade::class.java)
    }
}

const val CHECK_CANCELLATION_PERIOD_MS: Long = 50
inline fun <T> ReentrantLock.tryGuarded(crossinline computable: () -> T): T? =
    if (tryLock(CHECK_CANCELLATION_PERIOD_MS, TimeUnit.MILLISECONDS)) {
        try {
            computable()
        } finally {
            unlock()
        }
    } else {
        null
    }
