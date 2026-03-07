/*
 * Copyright 2026 LinQingYing. and contributors.
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

import com.intellij.execution.Platform
import com.intellij.execution.target.TargetPlatform
import com.intellij.openapi.diagnostic.ControlFlowException
import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.progress.ProcessCanceledException
import com.intellij.openapi.project.Project
import com.intellij.openapi.roots.ProjectRootModificationTracker
import com.intellij.openapi.util.Key
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.util.CachedValueProvider
import com.intellij.psi.util.CachedValuesManager
import org.cangnova.cangjie.ExceptionTracker
import org.cangnova.cangjie.context.GlobalContext
import org.cangnova.cangjie.context.GlobalContextImpl
import org.cangnova.cangjie.moduleinfo.IdeaModuleInfo
import org.cangnova.cangjie.moduleinfo.LibraryInfo
import org.cangnova.cangjie.moduleinfo.LibrarySourceInfo
import org.cangnova.cangjie.moduleinfo.ModuleSourceInfo
import org.cangnova.cangjie.moduleinfo.NotUnderContentRootModuleInfo
import org.cangnova.cangjie.moduleinfo.provider.moduleInfo
import org.cangnova.cangjie.moduleinfo.util.getDependentModules
import org.cangnova.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import org.cangnova.cangjie.projectStructure.RootKindFilter
import org.cangnova.cangjie.projectStructure.matches
import org.cangnova.cangjie.psi.CjCodeFragment
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.psi.NotNullableUserDataProperty
import org.cangnova.cangjie.psi.psiUtil.contains
import org.cangnova.cangjie.resolve.ModuleResolutionFacadeImpl
import org.cangnova.cangjie.resolve.ResolutionFacade
import org.cangnova.cangjie.resolve.ResolverForProject.Companion.resolverForLibrariesName
import org.cangnova.cangjie.resolve.ResolverForProject.Companion.resolverForModulesName
import org.cangnova.cangjie.resolve.ResolverForProject.Companion.resolverForSpecialInfoName
import org.cangnova.cangjie.storage.LockBasedStorageManager
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachments
import org.cangnova.cangjie.utils.sumByLong


internal val LOG = Logger.getInstance(CangJieCacheService::class.java)

/**
 * 平台分析设置实现
 *
 * 用于指定目标平台的分析配置，目前支持 Windows/Linux/macOS 等平台。
 */
data class PlatformAnalysisSettingsImpl(
    val platform: TargetPlatform,

    ) : PlatformAnalysisSettings

/**
 * 仓颉语言缓存服务实现
 *
 * 这个服务是仓颉语言分析系统的核心组件，负责：
 * 1. 为代码元素提供 ResolutionFacade（解析门面），使得代码分析、类型推导等功能得以实现
 * 2. 管理项目级别的全局解析器缓存，避免重复创建解析器
 * 3. 区分处理项目源码、库代码和特殊文件（如代码片段）
 * 4. 利用 IntelliJ 的缓存机制，在项目结构变化时自动失效缓存
 *
 * ## 为什么需要这个服务？
 * - 解析和分析代码是昂贵的操作，缓存可以显著提升 IDE 性能
 * - 不同类型的文件（源码、库、代码片段）需要不同的解析策略
 * - 需要根据项目结构变化（如添加/删除模块）自动更新缓存
 *
 * ## 核心设计
 * - 使用 GlobalFacade 管理全局解析器（facadeForModules 和 facadeForLibraries）
 * - 使用 SLRU（Segmented LRU）缓存提高多平台/多 SDK 场景下的性能
 * - 使用 CachedValuesManager 自动跟踪依赖，在项目根结构变化时失效缓存
 */
internal class CangJieCacheServiceImpl(val project: Project) : CangJieCacheService {
    /**
     * 为代码元素获取 ResolutionFacade
     *
     * 这是最常用的 API，用于分析单个代码元素（如函数、类、变量）。
     *
     * ## 缓存策略
     * - 以文件为粒度进行缓存（因为同一文件中的元素通常使用相同的 facade）
     * - 依赖 ProjectRootModificationTracker，确保项目结构变化时缓存失效
     *
     * @param element 需要分析的代码元素
     * @return 用于分析该元素的 ResolutionFacade
     */
    override fun getResolutionFacade(element: CjElement): ResolutionFacade {
        val file = element.fileForElement()

        return CachedValuesManager.getCachedValue(file) {
            val settings = PlatformAnalysisSettingsImpl(TargetPlatform(Platform.WINDOWS))

            CachedValueProvider.Result(
                getFacadeToAnalyzeFile(file, settings),

                ProjectRootModificationTracker.getInstance(project),
            )
        }
    }

    /**
     * 从 SLRU 缓存中获取或创建值
     *
     * ## 为什么使用双重检查锁定（Double-Check Locking）？
     * - 第一次检查在锁外进行，避免每次都获取锁，提高并发性能
     * - 如果缓存未命中，在锁外创建新值，避免长时间持有锁
     * - 第二次检查在锁内进行，确保只有一个实例被放入缓存
     *
     * ## 权衡（Trade-off）
     * - 可能会创建多个实例（因为多个线程可能同时在锁外创建）
     * - 但只有一个实例会被缓存和使用，其他实例会被 GC 回收
     * - 相比在锁内创建（阻塞所有其他线程），这是更好的性能折衷
     */
    private fun <K, V> SLRUCache<K, V>.getOrCreateValue(key: K): V =
        synchronized(this) {
            this.getIfCached(key)
        } ?: run {
            // do actual value calculation out of any locks
            // trade-off: several instances could be created, but only one would be used
            val newValue = this.createValue(key)
            synchronized(this) {
                val cached = this.getIfCached(key)
                cached ?: run {
                    newValue?.let { this.put(key, it) }
                    newValue
                }
            }
        }

    /**
     * 全局 Facade 缓存
     *
     * ## 为什么使用 SLRU 缓存？
     * - SLRU（Segmented LRU）比普通 LRU 在多变的访问模式下性能更好
     * - 将缓存分为"受保护"和"试用"两个段，频繁访问的项会被提升到受保护段
     *
     * ## 缓存容量计算：2 * 3 * 2 = 12
     * - 2 个平台（如 JVM、Native）
     * - 3 个 SDK 版本（支持多个 SDK 同时存在）
     * - 2 个分析模式（如调试模式、发布模式）
     *
     * 目前简化为只缓存一个 "CangJie" 键，未来可扩展为支持多平台/多 SDK
     */
    private val globalFacadesPerPlatformAndSdk: SLRUCache<String, GlobalFacade> =
        SLRUCache.slruCache(2 * 3 * 2, 2 * 3 * 2) { GlobalFacade() }

    /**
     * 获取模块解析 Facade
     *
     * 用于分析项目源码模块，包含所有依赖（库和其他模块）
     */
    private fun facadeForModules(/*settings: PlatformAnalysisSettings*/) =
        getOrBuildGlobalFacade(/*settings*/).facadeForModules

    /**
     * 获取库解析 Facade
     *
     * 用于分析库代码（如 stdlib、第三方依赖）
     */
    private fun librariesFacade() =
        getOrBuildGlobalFacade().facadeForLibraries


    /**
     * 获取或构建全局 Facade
     *
     * ## 为什么需要 @Synchronized？
     * - 确保同一时刻只有一个线程在创建 GlobalFacade
     * - GlobalFacade 的创建成本很高（需要收集所有模块、构建解析器）
     *
     * 目前简化为使用固定键 "CangJie"，未来可根据平台和设置动态生成键
     */
    @Synchronized
    private fun getOrBuildGlobalFacade(/*settings: PlatformAnalysisSettings*/) =
        globalFacadesPerPlatformAndSdk["CangJie"]


    /**
     * 全局 Facade 容器
     *
     * 这个内部类封装了项目级别的解析器 Facade，包括：
     * - facadeForLibraries：用于分析库代码（只读，可复用）
     * - facadeForModules：用于分析项目源码（可变，依赖库 Facade）
     *
     * ## 为什么分离库和模块的 Facade？
     * 1. 性能优化：库代码通常不变，可以缓存并被多个模块 Facade 复用
     * 2. 内存优化：避免为每个模块重复创建库的解析信息
     * 3. 增量更新：源码变化时只需重建模块 Facade，库 Facade 保持不变
     *
     * ## 异常追踪链
     * - librariesContext = context + 库解析器异常追踪
     * - modulesContext = librariesContext + 模块解析器异常追踪
     * - 这样可以区分异常来源于库解析还是模块解析
     */
    private inner class GlobalFacade {
        private val context = GlobalContext("cangjie", project)
        private val moduleFilters = GlobalFacadeModuleFilters(project)

        // 为 resolver 名称添加项目名称后缀，便于调试和追踪
        private val librariesResolverName = "$resolverForLibrariesName [${project.name}]"
        private val modulesResolverName = "$resolverForModulesName [${project.name}]"

        private val librariesContext = context.contextWithCompositeExceptionTracker(project, librariesResolverName)

        val facadeForLibraries = ProjectResolutionFacade(
            "facadeForLibraries", librariesResolverName,
            project, context,
            reuseDataFrom = null,
            moduleFilter = moduleFilters::libraryFacadeFilter,

            invalidateOnOOCB = false,
            dependencies = listOf(
                ProjectRootModificationTracker.getInstance(project)
            ),
        )

        private val modulesContext =
            librariesContext.contextWithCompositeExceptionTracker(project, modulesResolverName)

        val facadeForModules = ProjectResolutionFacade(
            "facadeForModules", modulesResolverName,
            project, modulesContext,
            reuseDataFrom = facadeForLibraries,
            moduleFilter = { true },
            dependencies = listOf(ProjectRootModificationTracker.getInstance(project)),
            invalidateOnOOCB = true,
        )
    }


    /**
     * 为文件获取分析 Facade
     *
     * ## 为什么需要区分"特殊文件"？
     * - 项目源码文件：使用标准的模块 Facade，可以复用缓存
     * - 特殊文件（如代码片段、库文件、临时文件）：需要创建独立的 Facade
     *   - 代码片段（CjCodeFragment）：例如 REPL、求值器中的代码
     *   - 不在项目源码根目录下的文件：例如外部库的源码
     *   - 这些文件的分析上下文与项目结构不匹配，不能使用全局 Facade
     *
     * ## 处理流程
     * 1. 检查文件是否是"特殊文件"（不在项目源码中）
     * 2. 如果是特殊文件，创建专门的 Facade（通过 getFacadeForSpecialFiles）
     * 3. 如果是普通项目文件，使用标准的模块 Facade（通过 getResolutionFacadeByModuleInfo）
     */
    private fun getFacadeToAnalyzeFile(file: CjFile, settings: PlatformAnalysisSettings): ResolutionFacade {
        val moduleInfo = file.moduleInfo


        val specialFile =   filterNotInProjectSource(file, moduleInfo)

        if (specialFile != null) {
            val specialFiles = setOf(specialFile)
            val projectFacade = getFacadeForSpecialFiles(specialFiles, settings)
            return ModuleResolutionFacadeImpl(projectFacade, moduleInfo).createdFor(specialFiles, moduleInfo)
        }
        return getResolutionFacadeByModuleInfo(moduleInfo /*, settings*/).createdFor(
            emptyList(),
            moduleInfo/*, settings*/
        )

    }


    /**
     * 根据分析上下文获取 ResolutionFacade
     *
     * 这是直接通过模块信息获取 Facade 的 API，跳过了文件级别的缓存。
     * 适用于已知具体模块上下文的场景（如从索引中查询符号）。
     *
     * @param context 分析上下文（通常对应一个源码模块或库模块）
     * @return 包装后的模块 ResolutionFacade
     */
    override fun getResolutionFacadeByModuleInfo(context: IdeaModuleInfo): ResolutionFacade {

        val projectFacade = facadeForModules()

        return ModuleResolutionFacadeImpl(projectFacade, context)
    }

    /**
     * 为多个代码元素获取 ResolutionFacade
     *
     * ## 使用场景
     * - 批量分析多个元素（如批量检查、重构）
     * - 需要在同一个上下文中分析多个文件
     *
     * ## 优化策略
     * - 如果所有元素来自同一个文件，使用单文件的缓存路径
     * - 如果来自多个文件，需要创建一个能处理所有文件的 Facade
     */
    override fun getResolutionFacade(elements: List<CjElement>): ResolutionFacade {

        val files = getFilesForElements(elements)
        if (files.size == 1) return getResolutionFacade(files.single())


        return getFacadeToAnalyzeFiles(files/*, settings*/)

    }

    /**
     * 获取代码片段的上下文文件
     *
     * ## 为什么需要这个方法？
     * - 代码片段（CjCodeFragment）不是独立的文件，而是嵌入在其他文件中的代码
     * - 例如：REPL 中的代码、调试器中的表达式求值、代码模板预览
     * - 需要找到真正的上下文文件来确定其所属模块和可见符号
     *
     * ## 递归处理
     * - 代码片段可以嵌套（片段的上下文又是另一个片段）
     * - 递归找到最终的真实文件
     */
    private fun CjCodeFragment.getContextFile(): CjFile? {
        val contextElement = context ?: return null
        val contextFile = (contextElement as? CjElement)?.getContainingCjFile()
            ?: throw AssertionError("Analyzing cangjie code fragment of type ${this::class.java} with java context of type ${contextElement::class.java}")
        return if (contextFile is CjCodeFragment) contextFile.getContextFile() else contextFile
    }

    /**
     * 过滤出不在项目源码中的文件
     *
     * 批量版本的 filterNotInProjectSource
     */
    private fun Collection<CjFile>.filterNotInProjectSource(context: IdeaModuleInfo): Set<CjFile> =
        mapNotNullTo(mutableSetOf()) { filterNotInProjectSource(it, context) }

    /**
     * 判断文件是否不在项目源码中
     *
     * ## 什么是"不在项目源码中"？
     * 1. 文件不在项目的源码根目录下（RootKindFilter.projectSources 不匹配）
     * 2. 文件不在模块的可见范围内（context.scope 不包含）
     *
     * ## 为什么需要特殊处理？
     * - 这些文件可能是：
     *   - 库的源码（在 SDK 或依赖库中）
     *   - 临时文件（如从剪贴板粘贴的代码）
     *   - 外部文件（不属于任何模块）
     * - 这些文件不能使用标准的模块 Facade，需要创建独立的分析上下文
     *
     * @return 如果文件不在项目源码中，返回该文件；否则返回 null
     */
    private fun filterNotInProjectSource(file: CjFile, context: IdeaModuleInfo): CjFile? {
        val fileToAnalyze = when (file) {
            is CjCodeFragment -> file.getContextFile()
            else -> file
        }

        if (fileToAnalyze == null) {
            return null
        }

        val isInProjectSource = RootKindFilter.projectSources.matches(fileToAnalyze)
                && context.contentScope.contains(fileToAnalyze)

        return if (!isInProjectSource) fileToAnalyze else null
    }

    /**
     * 为特殊文件获取 Facade
     *
     * ## 缓存设计
     * - 使用 CachedValuesManager 管理 SLRU 缓存
     * - 缓存键是（文件集合 + 平台设置）的组合
     * - 当项目根结构变化时，整个 SLRU 缓存会失效
     *
     * ## 并发安全性
     * - 不能使用局部锁（local lock），因为在 Upsource 等场景中：
     *   - 可能创建多个 CangJieCacheService 实例
     *   - 但它们共享同一个 CachedValue 实例（见 UP-8046）
     *   - CachedValueManager.getKeyForClass 使用 provider 的类名作为键
     * - 通过 getOrCreateValue 的双重检查锁定保证线程安全
     */
    private fun getFacadeForSpecialFiles(
        files: Set<CjFile>,
        settings: PlatformAnalysisSettings
    ): ProjectResolutionFacade {
        val cachedValue: SLRUCache<Pair<Set<CjFile>, PlatformAnalysisSettings>, ProjectResolutionFacade> =
            CachedValuesManager.getManager(project).getCachedValue(project, specialFilesCacheProvider)

        // In Upsource, we create multiple instances of KotlinCacheService, which all access the same CachedValue instance (UP-8046)
        // This is so because class name of provider is used as a key when fetching cached value, see CachedValueManager.getKeyForClass.
        // To avoid race conditions, we can't use any local lock to access the cached value contents.
        return cachedValue.getOrCreateValue(files to settings)
    }

    private fun getFacadeToAnalyzeFiles(files: Collection<CjFile>/*, settings: PlatformAnalysisSettings*/): ResolutionFacade {
        val moduleInfo = files.first().moduleInfo

        val specialFiles = files.filterNotInProjectSource(moduleInfo)

        if (specialFiles.isNotEmpty()) {
            val projectFacade = getFacadeForSpecialFiles(specialFiles, DefaultPlatformAnalysisSettings)
            return ModuleResolutionFacadeImpl(projectFacade, moduleInfo).createdFor(specialFiles, moduleInfo)
        }

        return getResolutionFacadeByModuleInfo(moduleInfo/*, settings*/).createdFor(
            emptyList(),
            moduleInfo/*, settings*/
        )
    }

    /**
     * 特殊文件缓存的提供者
     *
     * ## 为什么需要这个 Provider？
     * - createFacadeForFilesWithSpecialModuleInfo 内部的计算依赖项目根结构
     * - 当项目根结构变化时（如添加/删除模块），需要清空整个 SLRU 缓存
     * - 通过依赖 ProjectRootModificationTracker 实现自动失效
     *
     * ## 缓存层次
     * 1. CachedValue: 持有 SLRU 缓存实例
     * 2. SLRU 缓存: 缓存（文件集合 + 设置） → ProjectResolutionFacade 的映射
     * 3. ProjectResolutionFacade: 内部又有自己的缓存
     */
    private val specialFilesCacheProvider = CachedValueProvider {
        // NOTE: computations inside createFacadeForFilesWithSpecialModuleInfo depend on project root structure
        // so we additionally drop the whole slru cache on change
        CachedValueProvider.Result(
            SLRUCache.slruCache<Pair<Set<CjFile>, PlatformAnalysisSettings>, ProjectResolutionFacade>(2, 3) {
                createFacadeForFilesWithSpecialModuleInfo(it.first, it.second)
            },

            ProjectRootModificationTracker.getInstance(project)
        )
    }

    /**
     * 为具有特殊模块信息的文件创建 Facade
     *
     * ## 什么是"特殊模块信息"？
     * - 文件的分析上下文与标准项目模块不同
     * - 例如：库文件、代码片段、临时文件
     *
     * ## explicitSettings 参数的作用
     * - 允许覆盖文件"固有"的平台设置
     * - 使用场景：模块是通用的（common），但我们需要为特定平台创建 Facade
     *   - 例如：分析跨平台代码时，需要针对 JVM 平台创建专门的 Facade
     *
     * @param files 需要分析的文件集合
     * @param explicitSettings 可选的显式平台设置，用于覆盖文件的默认设置
     */
    // explicitSettings allows to override the "innate" settings of the files' moduleInfo
    // This can be useful, if the module is common, but we want to create a facade to
    private fun createFacadeForFilesWithSpecialModuleInfo(
        files: Set<CjFile>,
        explicitSettings: PlatformAnalysisSettings? = null
    ): ProjectResolutionFacade {
        // 假设所有文件来自同一个模块（如果不是，这里会抛出异常）
        val specialModuleInfo = files.map { it.moduleInfo }.toSet().single()

        /**
         * 为合成文件缓存创建依赖追踪器
         *
         * ## 为什么需要区分两种文件？
         * 1. 真实文件（originalFile == it）
         *    - 监听 modificationStamp（文件系统级别的修改时间戳）
         *    - 文件保存时会触发事件，缓存自动失效
         *
         * 2. 虚拟文件（originalFile != it）
         *    - 例如：J2K（Java-to-Kotlin）转换生成的临时文件
         *    - 这些文件不在文件系统中，不会收到修改事件
         *    - 监听 outOfBlockModificationCount（代码块外的修改次数）
         *    - 这是 PSI 级别的修改计数，不依赖文件系统事件
         */
        // Dummy files created e.g. by J2K do not receive events.
        val dependencyTrackerForSyntheticFileCache = if (files.all { it.originalFile != it }) {
            ModificationTracker { files.sumByLong { it.outOfBlockModificationCount } }
        } else ModificationTracker { files.sumByLong { it.modificationStamp } }

        val resolverDebugName =
            "$resolverForSpecialInfoName $specialModuleInfo for files ${files.joinToString { it.name }} "

        fun makeProjectResolutionFacade(
            debugName: String,
            globalContext: GlobalContextImpl,
            reuseDataFrom: ProjectResolutionFacade? = null,
            moduleFilter: (IdeaModuleInfo) -> Boolean = { true },
            allModules: Collection<IdeaModuleInfo>? = null
        ): ProjectResolutionFacade {
            return ProjectResolutionFacade(
                debugName,
                resolverDebugName,
                project,
                globalContext,
//                settings,
                syntheticFiles = files,
                reuseDataFrom = reuseDataFrom,
                moduleFilter = moduleFilter,
                dependencies = listOf(
                    dependencyTrackerForSyntheticFileCache,
                    ProjectRootModificationTracker.getInstance(project)
                ),
                invalidateOnOOCB = true,
                allModules = allModules
            )
        }

        /**
         * 根据上下文类型创建不同的 Facade
         *
         * ## 三种上下文类型的处理策略
         *
         * 1. 库上下文或不在内容根下的文件
         *    - 复用 librariesFacade 的数据
         *    - 只分析当前上下文，不包含依赖
         *    - 例如：查看 stdlib 源码、外部库文件
         *
         * 2. 源码上下文
         *    - 复用 modulesFacade 的数据
         *    - 分析当前上下文及其所有依赖（因为源码可能引用其他模块）
         *    - 例如：项目中的 .cj 源文件
         *
         * 3. 既是库上下文又是源码上下文（边缘情况）
         *    - 这种情况不应该发生，通常是项目配置错误
         *    - 例如：同一个文件既在 classes 根目录又在 sources 根目录下
         *    - 创建独立的 Facade，不复用任何数据
         *    - 记录警告日志便于排查问题
         */
        return when {
            specialModuleInfo is ModuleSourceInfo -> {
                val dependentModules = specialModuleInfo.getDependentModules()
                val modulesFacade = facadeForModules( )
                val globalContext =
                    modulesFacade.globalContext.contextWithCompositeExceptionTracker(
                        project,
                        "facadeForSpecialModuleInfo (ModuleSourceInfo)"
                    )
                makeProjectResolutionFacade(
                    "facadeForSpecialModuleInfo (ModuleSourceInfo)",
                    globalContext,
                    reuseDataFrom = modulesFacade,
                    moduleFilter = { it in dependentModules }
                )
            }

            specialModuleInfo is LibrarySourceInfo || specialModuleInfo is NotUnderContentRootModuleInfo -> {
                val librariesFacade = librariesFacade( )
                val debugName = "facadeForSpecialModuleInfo (LibrarySourceInfo or NotUnderContentRootModuleInfo)"
                val globalContext = librariesFacade.globalContext.contextWithCompositeExceptionTracker(project, debugName)
                makeProjectResolutionFacade(
                    debugName,
                    globalContext,
                    reuseDataFrom = librariesFacade,
                    moduleFilter = { it == specialModuleInfo }
                )
            }
            else -> throw IllegalStateException("Unknown AnalysisContext ${specialModuleInfo.javaClass}")
        }
    }

    /**
     * 从元素列表中提取不重复的文件列表
     *
     * distinct() 确保每个文件只出现一次，避免重复处理
     */
    private fun getFilesForElements(elements: List<CjElement>): List<CjFile> {
        return elements.map {
            it.fileForElement()
        }.distinct()
    }


    /**
     * 获取元素所属的文件
     *
     * ## 错误处理策略
     * - 使用 @Suppress("USELESS_ELVIS") 是因为：
     *   - getContainingCjFile() 声明为非空返回，但实际可能返回 null（PSI 实现细节）
     *   - Elvis 操作符提供额外的空值检查，提高健壮性
     *
     * - 区分控制流异常（ControlFlowException）：
     *   - 控制流异常（如 ProcessCanceledException）需要立即抛出
     *   - 其他异常包装为 ICangJieExceptionWithAttachments，附带详细的上下文信息
     *   - 附件信息包括：出错的元素、所属文件、原始错误消息
     *   - 这些信息对于调试 PSI 相关问题非常有用
     */
    private fun CjElement.fileForElement() = try {

        @Suppress("USELESS_ELVIS")
        getContainingCjFile() ?: throw IllegalStateException("containingCjFile was null for $this of ${this.javaClass}")
    } catch (e: Exception) {
        if (e is ControlFlowException) throw e
        throw CangJieExceptionWithAttachments("Couldn't get containingCjFile for cjElement", e)
            .withPsiAttachment("element", this)
            .withPsiAttachment("file", this.containingFile)
            .withAttachment("original", e.message)
    }
}

/**
 * 模块过滤器接口
 *
 * 用于在创建 Facade 时过滤需要包含的模块，目前未使用，为未来扩展预留。
 *
 * ## 设计意图
 * - sdkFacadeFilter: 过滤 SDK 模块（如标准库内建类型）
 * - libraryFacadeFilter: 过滤第三方库模块
 * - moduleFacadeFilter: 过滤项目源码模块
 *
 * 未来可能用于细粒度控制 Facade 包含哪些模块，优化内存和性能。
 */
internal interface ModuleFilters {
    fun sdkFacadeFilter(module: IdeaModuleInfo): Boolean
    fun libraryFacadeFilter(module: IdeaModuleInfo): Boolean
    fun moduleFacadeFilter(module: IdeaModuleInfo): Boolean
}private object ClassLoaderBuiltInsModuleFilters :  ModuleFilters {
    override fun sdkFacadeFilter(module: IdeaModuleInfo): Boolean = false
    override fun libraryFacadeFilter(module: IdeaModuleInfo): Boolean = module is LibraryInfo
    override fun moduleFacadeFilter(module: IdeaModuleInfo): Boolean = !module.isLibraryClasses()
}


internal class GlobalFacadeModuleFilters(project: Project) : ModuleFilters {
    private val impl = ClassLoaderBuiltInsModuleFilters

    override fun sdkFacadeFilter(module: IdeaModuleInfo): Boolean = impl.sdkFacadeFilter(module)
    override fun libraryFacadeFilter(module: IdeaModuleInfo): Boolean = impl.libraryFacadeFilter(module)
    override fun moduleFacadeFilter(module: IdeaModuleInfo): Boolean = impl.moduleFacadeFilter(module)
}
/**
 * 为 GlobalContext 添加组合异常追踪器
 *
 * ## 为什么需要组合异常追踪器？
 * - 当创建层次化的 Facade 时（如 modulesContext 依赖 librariesContext）
 * - 需要追踪整个链条上的异常情况
 * - 任何一层出现异常，都应该导致上层缓存失效
 *
 * ## 注释掉的代码
 * - 原本支持两种模式：组合分析（composite）和独立分析（separate）
 * - 目前简化为只使用独立分析（每个 Facade 有独立的锁和异常追踪）
 * - 保留注释代码便于未来重新启用组合分析模式
 */
internal fun GlobalContextImpl.contextWithCompositeExceptionTracker(
    project: Project,
    debugName: String
): GlobalContextImpl =
//    if (project.useCompositeAnalysis || project.useLibraryToSourceAnalysis) {
//        this.contextWithCompositeExceptionTracker(name)
//    } else {
    this.contextWithNewLockAndCompositeExceptionTracker(project, debugName)
//    }

/**
 * 创建带有新锁和组合异常追踪器的 GlobalContext
 *
 * ## 核心组件
 * 1. CompositeExceptionTracker: 组合父 Context 的异常追踪器
 * 2. LockBasedStorageManager: 为新 Context 创建独立的锁
 * 3. 取消检查: 定期检查用户是否取消操作（通过进度条）
 * 4. 异常处理: 将内部异常包装为 ProcessCanceledException
 *
 * ## 为什么需要新锁？
 * - 避免死锁：不同层次的 Facade 使用独立的锁
 * - 提高并发：库 Facade 和模块 Facade 可以并行访问
 */
private fun GlobalContextImpl.contextWithNewLockAndCompositeExceptionTracker(
    project: Project,
    debugName: String
): GlobalContextImpl {
    val newExceptionTracker = CompositeExceptionTracker(this.exceptionTracker)
    return GlobalContextImpl(
        LockBasedStorageManager.createWithExceptionHandling(

            debugName,
            newExceptionTracker,
            {
                ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()
            },
            { throw ProcessCanceledException(it) }),
        newExceptionTracker
    )
}

/**
 * 组合异常追踪器
 *
 * ## 实现原理
 * - 维护自己的修改计数（通过父类 ExceptionTracker）
 * - 同时监听委托（delegate）的修改计数
 * - getModificationCount() 返回两者之和
 *
 * ## 使用场景
 * - modulesContext 的异常追踪器委托给 librariesContext
 * - 当库解析出现异常时（delegate 计数增加），模块缓存也应该失效
 * - 当模块解析出现异常时（自身计数增加），模块缓存失效
 */
private class CompositeExceptionTracker(val delegate: ExceptionTracker) : ExceptionTracker() {
    override fun getModificationCount(): Long {
        return super.getModificationCount() + delegate.modificationCount
    }
}

/**
 * 仓颉解析模式说明（已简化）
 *
 * 原本支持两种模式：
 * - COMPOSITE: 组合模式，所有模块共享解析器
 * - SEPARATE: 独立模式，每个模块有独立的解析器
 *
 * 目前简化为只使用 SEPARATE 模式，注释代码保留以便未来扩展。
 *
 * 注意：这个属性显示的是项目全局设置，但实际上解析可能在不同操作中使用不同模式，
 * 所以使用时需要谨慎。
 */
//val Project.useCompositeAnalysis: Boolean
//    get() = CangJieMultiplatformAnalysisModeComponent.getMode(this) == CangJieMultiplatformAnalysisModeComponent.Mode.COMPOSITE

/**
 * 文件的"块外修改计数"
 *
 * ## 什么是"块外修改"？
 * - 在 IntelliJ 的增量分析中，代码被分为"块"（block）
 * - 块内修改：修改函数体内的代码（不影响接口）
 * - 块外修改：修改函数签名、类声明等（影响其他代码）
 *
 * ## 为什么需要这个计数？
 * - 对于虚拟文件（不在文件系统中），无法通过 modificationStamp 追踪修改
 * - 使用这个计数作为缓存失效的依据
 * - 例如：J2K 转换生成的临时文件、REPL 中的代码片段
 *
 * ## 实现方式
 * - 使用 NotNullableUserDataProperty 存储在文件的 UserData 中
 * - 默认值为 0
 * - 每次块外修改时递增
 */
private val FILE_OUT_OF_BLOCK_MODIFICATION_COUNT = Key<Long>("FILE_OUT_OF_BLOCK_MODIFICATION_COUNT")
val CjFile.outOfBlockModificationCount: Long by NotNullableUserDataProperty(FILE_OUT_OF_BLOCK_MODIFICATION_COUNT, 0)


fun IdeaModuleInfo.isLibraryClasses() = /*this is SdkInfo ||*/ this is LibraryInfo
