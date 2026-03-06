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

package org.cangnova.cangjie.resolve

/**
 * 项目解析器抽象基类
 *
 * 本文件提供了项目级代码解析的核心基础设施。
 * 解析器负责将分析上下文（AnalysisContext）映射到模块描述符（ModuleDescriptor），
 * 并为每个模块创建相应的解析器（ResolverForModule）。
 *
 * ## 核心概念
 *
 * ### 分析上下文 (AnalysisContext)
 *
 * 分析上下文是代码分析的最小单元，代表一个需要分析的范围。可以是：
 * - **源码模块**: 项目中的源代码模块
 * - **库模块**: 依赖的库
 * - **特殊上下文**: 用于代码补全、语法高亮等的临时上下文
 *
 * ### 模块描述符 (ModuleDescriptor)
 *
 * 模块描述符是分析引擎的内部表示，包含：
 * - 模块依赖关系
 * - 包片段提供者（PackageFragmentProvider）
 * - 内置类型（BuiltIns）
 *
 * ### 解析器 (ResolverForModule)
 *
 * 为特定模块提供符号解析能力，包括：
 * - 查找类型定义
 * - 解析函数和变量
 * - 处理导入语句
 *
 * ## 工作流程
 *
 * ```
 * AnalysisContext (例如: CjModuleAnalysisContext)
 *   ↓
 * descriptorForModule()
 *   ↓
 * ModuleDescriptor 创建/获取
 *   ↓
 * resolverForModuleDescriptor()
 *   ↓
 * ResolverForModule 创建/获取
 *   ↓
 * 符号解析（类型检查、代码补全等）
 * ```
 *
 * ## 主要组件
 *
 * - [AbstractResolverForProject]: 项目解析器抽象基类
 * - [ModuleData]: 模块数据容器，关联描述符和修改追踪器
 * - [DiagnoseUnknownContextReporter]: 诊断未知上下文的错误报告器
 * - [DelegatingPackageFragmentProvider]: 委托式包片段提供者
 *
 * ## 线程安全
 *
 * 所有涉及内部状态修改的操作都通过 `projectContext.storageManager.lock` 保护，
 * 确保在多线程环境下的安全性。
 *
 * @see ResolverForProject
 * @see ModuleInfo
 * @see ModuleDescriptor
 * @see ResolverForModule
 */

import com.intellij.openapi.Disposable
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.util.ModificationTracker
import com.intellij.platform.workspace.jps.entities.LibraryId
import com.intellij.workspaceModel.ide.impl.legacyBridge.library.LibraryBridge
import org.cangnova.cangjie.builtins.StandardNames.STD_PACKAGE_NAME
import org.cangnova.cangjie.context.ProjectContext
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.descriptors.impl.StdlibModuleDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import org.cangnova.cangjie.descriptors.impl.ProjectDescriptorImpl
import org.cangnova.cangjie.moduleinfo.LibraryInfo
import org.cangnova.cangjie.moduleinfo.ModuleInfo
import org.cangnova.cangjie.moduleinfo.SourceForBinaryModuleInfo
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.caches.ModuleContent
import org.cangnova.cangjie.utils.exceptions.CangJieExceptionWithAttachments
import org.cangnova.cangjie.utils.exceptions.checkWithAttachment


/**
 * 项目解析器抽象基类
 *
 * 这是项目级代码解析的核心组件，负责管理整个项目的所有模块及其解析器。
 * 它维护了分析上下文、模块描述符和模块解析器之间的映射关系。
 *
 * ## 核心职责
 *
 * 1. **上下文管理**: 管理所有需要分析的上下文（源码模块、库模块等）
 * 2. **描述符缓存**: 为每个上下文创建并缓存对应的模块描述符
 * 3. **解析器创建**: 为每个模块描述符创建和缓存解析器
 * 4. **依赖解析**: 处理模块间的依赖关系
 * 5. **失效处理**: 检测模块变更并重新创建描述符
 *
 * ## 类型参数
 *
 * @param M 分析上下文的类型，必须继承自 [ModuleInfo]
 *
 * ## 构造参数
 *
 * @param debugName 调试名称，用于错误消息和日志
 * @param projectContext 项目上下文，提供存储管理器和项目实例
 * @param projectDescriptor 项目描述符，代表整个项目的分析视图
 * @param modules 要管理的所有分析上下文集合
 * @param fallbackModificationTracker 备用的修改追踪器，当上下文不提供追踪器时使用
 * @param delegateResolver 委托解析器，用于处理当前解析器无法处理的上下文
 * @param packageOracleFactory 包预言工厂，用于快速判断包是否存在
 *
 * ## 关键数据结构
 *
 * ### descriptorByModule (M -> ModuleData)
 * - 从分析上下文到模块数据的映射
 * - 缓存已创建的模块描述符和修改追踪器
 * - 用于检测模块是否需要重新创建
 *
 * ### contextByDescriptor (ModuleDescriptorImpl -> M)
 * - 从模块描述符到分析上下文的反向映射
 * - 用于根据描述符查找对应的上下文
 *
 * ### resolverByModuleDescriptor (ModuleDescriptor -> ResolverForModule)
 * - 从模块描述符到解析器的映射
 * - 缓存已创建的解析器，避免重复创建
 *
 * ## 线程安全
 *
 * 所有内部状态的读写都在 `projectContext.storageManager.lock` 的保护下进行：
 * ```kotlin
 * projectContext.storageManager.compute {
 *     // 线程安全的操作
 * }
 * ```
 *
 * ## 生命周期管理
 *
 * 实现了 [Disposable] 接口，当解析器不再需要时：
 * 1. 将 `disposed` 标志设为 true
 * 2. 清空所有缓存的映射表
 * 3. 将所有模块描述符标记为无效
 *
 * ## 使用示例
 *
 * ```kotlin
 * class MyResolver : AbstractResolverForProject<CjModuleAnalysisContext>(
 *     debugName = "My Project Resolver",
 *     projectContext = context,
 *     projectDescriptor = descriptor,
 *     modules = allModules
 * ) {
 *     override fun createResolverForModule(
 *         descriptor: ModuleDescriptor,
 *         context: CjModuleAnalysisContext
 *     ): ResolverForModule {
 *         // 创建模块解析器
 *     }
 *
 *     override fun modulesContent(module: CjModuleAnalysisContext): ModuleContent<CjModuleAnalysisContext> {
 *         // 返回模块内容
 *     }
 * }
 * ```
 *
 * ## 错误处理
 *
 * - 如果请求未知的上下文，会调用 [diagnoseUnknownModuleInfo] 生成详细的错误报告
 * - 如果解析器已被释放，会抛出 [InvalidResolverException]
 *
 * @see ResolverForProject
 * @see ModuleInfo
 * @see ModuleDescriptor
 * @see ResolverForModule
 * @see Disposable
 */
abstract class AbstractResolverForProject<M : ModuleInfo>(

    private val debugName: String,
    protected val projectContext: ProjectContext,
    protected val projectDescriptor: ProjectDescriptor,
    modules: Collection<M>,
    protected val fallbackModificationTracker: ModificationTracker? = null,
    private val delegateResolver: ResolverForProject<M> = EmptyResolverForProject(),
    private val packageOracleFactory: PackageOracleFactory = PackageOracleFactory.OptimisticFactory
) : ResolverForProject<M>(), Disposable {
    /**
     * 释放标志
     *
     * 标记解析器是否已被释放。
     * 一旦释放，所有操作都会抛出 [InvalidResolverException]。
     *
     * 使用 @Volatile 确保多线程可见性。
     */
    @Volatile
    protected var disposed = false

    /**
     * 模块数据映射表
     *
     * 从分析上下文到模块数据的映射。
     * 模块数据包含模块描述符和修改追踪器。
     *
     * **线程安全**: 由 `projectContext.storageManager.lock` 保护
     */
    // Protected by ("projectContext.storageManager.lock")
    protected val descriptorByModule = hashMapOf<M, ModuleData>()

    /**
     * 解析器名称
     *
     * 用于错误消息和调试输出。
     */
    override val name: String
        get() = "Resolver for '$debugName'"

    /**
     * 释放解析器资源
     *
     * 清理所有内部状态并将模块描述符标记为无效。
     * 此方法应在解析器不再使用时调用。
     *
     * ## 清理步骤
     *
     * 1. 设置 `disposed = true`
     * 2. 从 `contextByDescriptor` 移除所有条目
     * 3. 将所有模块描述符标记为 `isValid = false`
     * 4. 清空 `descriptorByModule` 和 `contextByDescriptor`
     */
    override fun dispose() {
        projectContext.storageManager.compute {
            disposed = true
            descriptorByModule.values.forEach {
                contextByDescriptor.remove(it.moduleDescriptor)
                it.moduleDescriptor.isValid = false
            }
            descriptorByModule.clear()
            contextByDescriptor.keys.forEach { it.isValid = false }
            contextByDescriptor.clear()
        }
    }

    /**
     * 模块数据容器
     *
     * 封装了模块描述符及其修改追踪器。
     * 用于检测模块是否已过时（需要重新创建）。
     *
     * @property moduleDescriptor 模块描述符
     * @property modificationTracker 修改追踪器（可选）
     * @property modificationCount 创建时的修改计数快照
     *
     * ## 过时检测
     *
     * 通过比较当前修改计数和快照来判断模块是否已修改：
     * ```kotlin
     * if (moduleData.isOutOfDate()) {
     *     // 重新创建模块描述符
     * }
     * ```
     */
    protected class ModuleData(
        val moduleDescriptor: ModuleDescriptorImpl,
        val modificationTracker: ModificationTracker?
    ) {
        /**
         * 创建时的修改计数
         *
         * 如果没有追踪器，使用 Long.MIN_VALUE 表示永不过时。
         */
        val modificationCount: Long = modificationTracker?.modificationCount ?: Long.MIN_VALUE

        /**
         * 检查模块是否已过时
         *
         * @return true 如果当前修改计数大于快照值
         */
        fun isOutOfDate(): Boolean {
            val currentModCount = modificationTracker?.modificationCount
            return currentModCount != null && currentModCount > modificationCount
        }
    }

    /**
     * 检查解析器是否已为描述符计算
     *
     * @param descriptor 要检查的模块描述符
     * @return true 如果已为此描述符创建解析器
     */
    internal fun isResolverForModuleDescriptorComputed(descriptor: ModuleDescriptor) =
        projectContext.storageManager.compute {
            descriptor in resolverByModuleDescriptor
        }

    /**
     * 描述符到上下文的反向映射表
     *
     * 用于根据模块描述符查找对应的分析上下文。
     *
     * **线程安全**: 由 `projectContext.storageManager.lock` 保护
     */
    // Protected by ("projectContext.storageManager.lock")
    private val contextByDescriptor = hashMapOf<ModuleDescriptorImpl, M>()

    /**
     * 描述符到解析器的映射表
     *
     * 缓存已创建的模块解析器，避免重复创建。
     *
     * **线程安全**: 由 `projectContext.storageManager.lock` 保护
     */
    // Protected by ("projectContext.storageManager.lock")
    private val resolverByModuleDescriptor = hashMapOf<ModuleDescriptor, ResolverForModule>()

    /**
     * 上下文到可解析信息的映射
     *
     * 在新的 AnalysisContext 系统中，每个上下文都是独立的，不需要 flatten。
     * 直接将每个上下文映射到自身。
     */

    private val contextToResolvableInfo: Map<M, M> =
        modules.associateWith { it }

    /**
     * 所有模块的集合
     *
     * 包含当前解析器管理的所有模块和委托解析器的所有模块。
     */
    override val allModules: Set<M> by lazy {
        this.contextToResolvableInfo.keys + delegateResolver.allModules
    }

    /**
     * 创建模块解析器的核心实现（抽象方法）
     *
     * 子类必须实现此方法来创建具体的模块解析器。
     * 此方法由 [createResolverForModule] 模板方法调用。
     *
     * @param descriptor 模块描述符
     * @param context 分析上下文
     * @return 为该模块创建的解析器
     */
    protected abstract fun doCreateResolverForModule(descriptor: ModuleDescriptor, context: M): ResolverForModule

    /**
     * 创建模块解析器（模板方法）
     *
     * 这是一个模板方法，负责：
     * 1. 调用 [doCreateResolverForModule] 创建解析器
     * 2. 调用 [onResolverCreated] 钩子方法进行后处理
     *
     * 子类应该重写 [doCreateResolverForModule] 而不是此方法。
     *
     * @param descriptor 模块描述符
     * @param context 分析上下文
     * @return 为该模块创建的解析器
     */
    fun createResolverForModule(descriptor: ModuleDescriptor, context: M): ResolverForModule {
        val resolver = doCreateResolverForModule(descriptor, context)
        onResolverCreated(descriptor, context, resolver)
        return resolver
    }

    /**
     * 解析器创建后的钩子方法
     *
     * 在模块解析器创建完成后调用，用于执行额外的初始化操作。
     * 子类可以重写此方法来：
     * - 设置扩展发现器
     * - 预加载扩展声明
     * - 执行其他模块级初始化
     *
     * 默认实现为空。
     *
     * @param descriptor 模块描述符
     * @param context 分析上下文
     * @param resolver 刚创建的模块解析器
     */
    protected open fun onResolverCreated(descriptor: ModuleDescriptor, context: M, resolver: ResolverForModule) {
        // 默认空实现，子类可以重写
    }

    /**
     * 诊断未知上下文
     *
     * 当请求的上下文不在 [allModules] 中时被调用。
     * 生成详细的错误报告并抛出异常。
     *
     * @param contexts 未知的上下文列表
     * @throws CangJieExceptionWithAttachments 带有诊断信息的异常
     */


    override fun diagnoseUnknownModuleInfo(contexts: List<ModuleInfo>): Nothing {
        DiagnoseUnknownContextReporter.report(name, contexts, allModules)

    }

    /**
     * 检查解析器是否有效
     *
     * 如果解析器已被释放，抛出异常。
     *
     * @throws InvalidResolverException 如果解析器已释放
     */
    private fun checkValid() {
        if (disposed) {
            reportInvalidResolver()
        }
    }

    /**
     * 获取模块内容（抽象方法）
     *
     * 子类必须实现此方法来提供模块的内容信息。
     *
     * @param module 分析上下文
     * @return 模块内容，包含源文件和合成文件
     */
    abstract fun modulesContent(module: M): ModuleContent<M>

    /**
     * 报告无效解析器
     *
     * 当解析器已被释放时抛出异常。
     * 子类可以重写此方法提供更详细的错误信息。
     *
     * @throws InvalidResolverException 解析器无效异常
     */
    protected open fun reportInvalidResolver() {
        throw InvalidResolverException("$name is invalidated")
    }

    /**
     * 尝试获取模块解析器
     *
     * 与 [resolverForModule] 类似，但如果上下文不正确则返回 null 而不是抛出异常。
     * 当当前解析器无法处理上下文时，会尝试委托给 [delegateResolver]。
     *
     * ## 委托机制
     *
     * 这与 [doGetDescriptorForModule] 的行为保持一致：
     * - 首先检查当前解析器是否能处理该上下文
     * - 如果不能，则委托给 delegateResolver（通常是 facadeForLibraries 的解析器）
     *
     * 这对于库中的扩展声明解析至关重要：
     * - facadeForModules 的解析器处理源码模块
     * - facadeForLibraries 的解析器处理库模块
     * - 当发现库中的扩展时，需要通过委托获取正确的解析器
     *
     * @param context 分析上下文
     * @return 模块解析器，如果上下文不正确且委托解析器也无法处理则返回 null
     */
    override fun tryGetResolverForModule(context: M): ResolverForModule? {
        checkValid()
        // 如果是库源码模块（LibrarySourceInfo），重定向到对应的二进制模块（LibraryInfoImpl）
        // 因为 allModules 和 contextToResolvableInfo 中存储的是二进制模块
        val actualContext = if (context is SourceForBinaryModuleInfo) {
            @Suppress("UNCHECKED_CAST")
            (context.binariesModuleInfo as? M) ?: context
        } else {
            context
        }
        if (!isCorrectContext(actualContext)) {
            // 当前解析器无法处理，尝试委托给 delegateResolver
            // 这确保了分层解析架构中，库模块可以通过委托正确解析
            return delegateResolver.tryGetResolverForModule(actualContext)
        }
        return resolverForModuleDescriptor(doGetDescriptorForModule(actualContext))
    }


    /**
     * 检查上下文是否正确
     *
     * 判断上下文或其原始上下文是否在 [allModules] 中。
     * 支持以下类型的上下文映射：
     * - DerivedModuleInfo: 映射到 originalModuleInfo
     * - SourceForBinaryModuleInfo: 映射到 binariesModuleInfo（库源码映射到对应的二进制模块）
     *
     * @param context 要检查的分析上下文
     * @return true 如果上下文有效
     */
    private fun isCorrectContext(context: M): Boolean {
        val originalContext = when (context) {
            is DerivedModuleInfo -> context.originalModuleInfo
            is SourceForBinaryModuleInfo -> context.binariesModuleInfo
            else -> context
        }
        return originalContext in allModules
    }

    private fun recreateModuleDescriptor(module: M): ModuleData {
        val oldDescriptor = descriptorByModule[module]?.moduleDescriptor
        if (oldDescriptor != null) {
            oldDescriptor.isValid = false
            contextByDescriptor.remove(oldDescriptor)
            resolverByModuleDescriptor.remove(oldDescriptor)
        }

        val moduleData = createModuleDescriptor(module)
        descriptorByModule[module] = moduleData

        return moduleData
    }

    protected open fun getAdditionalCapabilities(): Map<ModuleCapability<*>, Any?> = emptyMap()

    private fun createModuleDescriptor(module: M): ModuleData {

        // 特殊处理：如果是 stdlib 模块且 ProjectDescriptorImpl 中已提前创建，则直接使用
        if (((module as? LibraryInfo)?.library?.isStdlib() == true || module.name == STD_PACKAGE_NAME) && projectDescriptor is ProjectDescriptorImpl) {
            try {
                val existingStdlibModule = projectDescriptor.stdlibModule
                // 检查是否已经在 contextByDescriptor 中注册
                if (existingStdlibModule !in contextByDescriptor) {
                    // 将module中的模块能力补全到existingStdlibModule
                    module.capabilities.forEach {
                        existingStdlibModule.addCapability(it.key, it.value)
                    }
                    // 同时添加额外的模块能力（如 ExtendManager）
                    getAdditionalCapabilities().forEach {
                        existingStdlibModule.addCapability(it.key, it.value)
                    }
                    contextByDescriptor[existingStdlibModule] = module
                }


                val modificationTracker =
                    (module as? TrackableModuleInfo)?.createModificationTracker() ?: fallbackModificationTracker

                return ModuleData(existingStdlibModule, modificationTracker)
            } catch (e: Exception) {
                // 如果获取失败（如 SDK 未配置），继续正常创建流程
                System.err.println("Warning: Failed to use pre-created stdlib module, creating new one: ${e.message}")
            }
        }

        // 在创建新模块之前，先移除旧的同名模块（支持模块刷新场景）
        // 注意：对于 stdlib，如果上面的逻辑成功，不会执行到这里
        val moduleName = module.name
        if (projectDescriptor is ProjectDescriptorImpl) {
            // 不要移除 stdlib 模块，因为它已经在 ProjectDescriptorImpl.init 中创建
            if (moduleName != STD_PACKAGE_NAME) {
                projectDescriptor.removeModule(moduleName)
            }
        }

        val moduleDescriptor = ModuleDescriptorImpl(
            projectDescriptor,
            module.name,
            module.displayedName,
            projectContext.storageManager,
            module.capabilities + getAdditionalCapabilities(),
            null // stableName
            // isBuiltInsModule
        )
        contextByDescriptor[moduleDescriptor] = module


        setupModuleDescriptor(module, moduleDescriptor)
        val modificationTracker =
            (module as? TrackableModuleInfo)?.createModificationTracker() ?: fallbackModificationTracker
        return ModuleData(moduleDescriptor, modificationTracker)
    }


    private fun setupModuleDescriptor(module: M, moduleDescriptor: ModuleDescriptorImpl) {
        checkValid()

        // 特殊处理：如果 moduleDescriptor 是从 ProjectDescriptorImpl 复用的 stdlib，跳过初始化
        // 因为它已经在 ProjectDescriptorImpl.createStdlibModule() 中完成了初始化
        if (module.name == STD_PACKAGE_NAME
            && projectDescriptor is ProjectDescriptorImpl
            && moduleDescriptor === projectDescriptor.stdlibModule
        ) {

            // stdlib 已经初始化，只需要设置依赖即可
            // 注意：这里不调用 setDependencies，因为 stdlib 的依赖已经在创建时设置
            // （stdlib 依赖 builtInsModule）
            return
        }

        moduleDescriptor.setDependencies(
            LazyModuleDependencies(
                projectContext.storageManager,
                module,
                null,
                this,
                instantiatedDependencies = listOf(projectDescriptor.stdlibModule)
            )
        )

        val content = modulesContent(module)

        // 源码模块使用DelegatingPackageFragmentProvider
        moduleDescriptor.initialize(
            DelegatingPackageFragmentProvider(
                this, moduleDescriptor, content,
                packageOracleFactory.createOracle(module)
            )
        )

    }

    /**
     * 为库模块加载包片段提供者
     *
     * 根据模块的依赖类型选择合适的PackageFragmentProvider：
     * - Binary依赖：使用BuiltInsLoader加载.cjo文件
     * - 其他类型：回退到DelegatingPackageFragmentProvider
     *
     * @param moduleDescriptor 模块描述符
     * @param content 模块内容
     * @param module 分析上下文
     * @return PackageFragmentProvider
     */
    private fun loadPackageFragmentProvider(
        moduleDescriptor: StdlibModuleDescriptorImpl,
        content: ModuleContent<M>,
        module: M
    ): PackageFragmentProvider {
        // TODO: 实现根据CjDependency类型选择PackageFragmentProvider
        // 1. 从module中获取对应的CjDependency
        // 2. 如果是CjDependency.Binary，获取.cjo文件路径
        // 3. 调用BuiltInsLoader.Instance.createPackageFragmentProvider(storageManager, moduleDescriptor, cjoFiles)
        // 4. 否则回退到DelegatingPackageFragmentProvider

        // 暂时回退到DelegatingPackageFragmentProvider
        return DelegatingPackageFragmentProvider(
            this,
            moduleDescriptor,
            content,
            packageOracleFactory.createOracle(module)
        )
    }

    /**
     * 检查模块上下文是否正确
     *
     * 验证请求的上下文是否在此解析器管理的模块列表中。
     * 如果上下文不在 [allModules] 中，会调用 [diagnoseUnknownModuleInfo] 生成详细的错误报告。
     *
     * ## 调用时机
     *
     * 在以下场景中被调用：
     * 1. [descriptorForModule] - 获取模块描述符前验证上下文
     * 2. [resolverForModuleDescriptorImpl] - 创建解析器前验证上下文
     *
     * ## 错误场景
     *
     * 当此方法检测到无效上下文时，会触发 [diagnoseUnknownModuleInfo]，导致异常：
     * ```
     * CangJieExceptionWithAttachments:
     *   Resolver for 'completion/highlighting in ...' does not know how to resolveName
     * ```
     *
     * 常见原因：
     * - **未注册的模块**: 上下文未在解析器初始化时注册到 [allModules]
     * - **项目未同步**: 项目结构变更后未重新构建解析器
     * - **临时文件**: Scratch 文件或临时文件没有关联的模块上下文
     * - **派生上下文问题**: 派生上下文的 [DerivedAnalysisContext.originalContext] 不在 [allModules] 中
     *
     * ## 工作原理
     *
     * ```
     * checkModuleIsCorrect(context)
     *   ↓
     * isCorrectContext(context)?
     *   ↓ (false - 上下文无效)
     * diagnoseUnknownModuleInfo(listOf(context))
     *   ↓
     * DiagnoseUnknownContextReporter.report()
     *   ↓
     * 抛出 CangJieExceptionWithAttachments
     *   附件: contexts.txt, allModules.txt
     * ```
     *
     * ## 示例
     *
     * ```kotlin
     * // 正常情况
     * val context = allModules.first()
     * checkModuleIsCorrect(context)  // 通过，不抛异常
     *
     * // 错误情况
     * val unknownContext = SomeOtherContext()
     * checkModuleIsCorrect(unknownContext)  // 抛出异常
     * // CangJieExceptionWithAttachments: ... does not know how to resolveName
     * ```
     *
     * ## 调试建议
     *
     * 如果遇到此错误：
     * 1. 检查异常附件中的 `contexts.txt` 和 `allModules.txt`
     * 2. 确认上下文是否应该在 [allModules] 中
     * 3. 检查项目是否需要重新同步（File → Reload All from Disk）
     * 4. 对于派生上下文，检查 `originalContext` 是否有效
     *
     * @param context 要验证的分析上下文
     * @throws CangJieExceptionWithAttachments 如果上下文不在 [allModules] 中
     *
     * @see isCorrectContext
     * @see diagnoseUnknownModuleInfo
     * @see DiagnoseUnknownContextReporter
     */
    private fun checkModuleIsCorrect(context: M) {
        if (!isCorrectContext(context)) {
            diagnoseUnknownModuleInfo(listOf(context))
        }
    }

    override fun descriptorForModule(context: M): ModuleDescriptorImpl {
        checkValid()
        checkModuleIsCorrect(context)
        return doGetDescriptorForModule(context)
    }

    private fun doGetDescriptorForModule(module: M): ModuleDescriptorImpl {
        val moduleFromThisResolver =
            module.takeIf { it is DerivedModuleInfo && it.originalModuleInfo in contextToResolvableInfo }
                ?: contextToResolvableInfo[module]
                ?: return delegateResolver.descriptorForModule(module) as ModuleDescriptorImpl

        return projectContext.storageManager.compute {
            var moduleData = descriptorByModule.getOrPut(moduleFromThisResolver) {
                createModuleDescriptor(moduleFromThisResolver)
            }
            if (moduleData.isOutOfDate()) {
                moduleData = recreateModuleDescriptor(moduleFromThisResolver)
            }
            moduleData.moduleDescriptor
        }
    }

    private fun renderResolversChainContents(): String {
        val resolversChain = generateSequence(this) { it.delegateResolver as? AbstractResolverForProject<M> }

        return resolversChain.joinToString("\n\n") { resolver ->
            "Resolver: ${resolver.name}\n'contextByDescriptor' content:\n[${resolver.renderResolverContexts()}]"
        }
    }

    private fun renderResolverContexts(): String = projectContext.storageManager.compute {
        contextByDescriptor.entries.joinToString(",\n") { (descriptor, context) ->
            """
            {
                moduleDescriptor: $descriptor
                context: $context
            }
            """.trimIndent()
        }
    }

    private fun resolverForModuleDescriptorImpl(descriptor: ModuleDescriptor): ResolverForModule? {
        return projectContext.storageManager.compute {
            checkValid()
            descriptor.assertValid()

            val module = contextByDescriptor[descriptor]
            if (module == null) {
                if (delegateResolver is EmptyResolverForProject<*>) {
                    return@compute null
                }
                return@compute (delegateResolver as AbstractResolverForProject<M>).resolverForModuleDescriptorImpl(
                    descriptor
                )
            }
            resolverByModuleDescriptor.getOrPut(descriptor) {
                checkModuleIsCorrect(module)

                ResolverForModuleComputationTracker.getInstance(projectContext.project)
                    ?.onResolverComputed(module)

                createResolverForModule(descriptor, module)
            }
        }
    }

    final override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule {
        val moduleResolver = resolverForModuleDescriptorImpl(descriptor)


        checkWithAttachment(
            moduleResolver != null,
            lazyMessage = { "$descriptor is not contained in resolver $name" },
            attachments = {
                it.withAttachment(
                    "resolverContents.txt",
                    "Expected module descriptor: $descriptor\n\n${renderResolversChainContents()}"
                )
            }
        )

        return moduleResolver
    }
}

/**
 * 无效解析器异常
 *
 * 当尝试使用已被释放（disposed）的解析器时抛出此异常。
 *
 * @param message 错误消息
 */
class InvalidResolverException(message: String) : IllegalStateException(message)

/**
 * 未知上下文诊断报告器
 *
 * 负责为未知的分析上下文生成详细的错误报告。
 * 根据解析器名称和上下文类型，生成不同类型的异常以便于诊断。
 *
 * ## 错误分类
 *
 * 根据解析器名称的不同部分，生成不同的异常：
 *
 * ### 库解析器错误 (Libraries Resolver)
 * - 名称包含 "project libraries"
 * - 通常表示库依赖配置问题
 *
 * ### 模块解析器错误 (Modules Resolver)
 * - 名称包含 "project source roots and libraries"
 * - 根据上下文数量和类型细分：
 *   - 空上下文列表: 可能是初始化问题
 *   - 单个 ScriptDependencies 上下文: 脚本依赖问题
 *   - 单个 Library 上下文: 库配置问题
 *   - 其他情况: 通用模块解析错误
 *
 * ### 特殊信息解析器错误 (Special Info Resolver)
 * - 名称包含 "completion/highlighting in"
 * - 用于代码补全和语法高亮的临时上下文
 * - 可能表示项目结构未正确初始化
 *
 * ## 错误报告格式
 *
 * 异常会附带两个诊断文件：
 * - **contexts.txt**: 导致错误的未知上下文列表
 * - **allModules.txt**: 解析器管理的所有已知模块
 *
 * 这些信息可以帮助诊断为什么某个上下文未被注册到解析器中。
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 当请求未知上下文时自动调用
 * override fun diagnoseUnknownModuleInfo(contexts: List<AnalysisContext>): Nothing {
 *     DiagnoseUnknownContextReporter.report(name, contexts, allModules)
 * }
 * ```
 *
 * @see CangJieExceptionWithAttachments
 * @see ResolverForProject
 */
private object DiagnoseUnknownContextReporter {
    /**
     * 报告未知上下文错误
     *
     * 根据解析器名称和上下文类型生成详细的错误报告。
     *
     * @param name 解析器名称，用于判断错误类型
     * @param contexts 未知的上下文列表
     * @param allModules 解析器管理的所有模块
     * @throws CangJieExceptionWithAttachments 带诊断信息的异常
     */
    fun report(name: String, contexts: List<ModuleInfo>, allModules: Collection<ModuleInfo>): Nothing {
        val message = "$name does not know how to resolveName"
        val error = when {

            name.contains(ResolverForProject.resolverForLibrariesName) -> errorInLibrariesResolver(message)
            name.contains(ResolverForProject.resolverForModulesName) -> {
                when {
                    contexts.isEmpty() -> errorInModulesResolverWithEmptyInfos(message)
                    contexts.size == 1 -> {
                        val contextAsString = contexts.single().toString()
                        when {
                            contextAsString.contains("ScriptDependencies") -> errorInModulesResolverWithScriptDependencies(
                                message
                            )

                            contextAsString.contains("Library") -> errorInModulesResolverWithLibraryInfo(message)
                            else -> errorInModulesResolver(message)
                        }
                    }

                    else -> errorInModulesResolver(message)
                }
            }


            name.contains(ResolverForProject.resolverForSpecialInfoName) -> {
                when {
                    name.contains("ScriptModuleInfo") -> errorInScriptModuleInfoResolver(message)
                    else -> errorInSpecialModuleInfoResolver(message)
                }
            }

            else -> otherError(message)
        }

        throw error.withAttachment("contexts.txt", contexts).withAttachment("allModules.txt", allModules)
    }

    // 以下方法不要内联，它们用于避免异常分析器合并这些错误

    /** SDK 解析器错误 */
    private fun errorInSdkResolver(message: String) = CangJieExceptionWithAttachments(message)

    /** 库解析器错误 */
    private fun errorInLibrariesResolver(message: String) = CangJieExceptionWithAttachments(message)

    /** 模块解析器错误 */
    private fun errorInModulesResolver(message: String) = CangJieExceptionWithAttachments(message)

    /** 模块解析器错误 - 空上下文列表 */
    private fun errorInModulesResolverWithEmptyInfos(message: String) = CangJieExceptionWithAttachments(message)

    /** 模块解析器错误 - 脚本依赖 */
    private fun errorInModulesResolverWithScriptDependencies(message: String) =
        CangJieExceptionWithAttachments(message)

    /** 模块解析器错误 - 库信息 */
    private fun errorInModulesResolverWithLibraryInfo(message: String) = CangJieExceptionWithAttachments(message)

    /** 脚本依赖信息解析器错误 */
    private fun errorInScriptDependenciesInfoResolver(message: String) = CangJieExceptionWithAttachments(message)

    /** 脚本模块信息解析器错误 */
    private fun errorInScriptModuleInfoResolver(message: String) = CangJieExceptionWithAttachments(message)

    /** 特殊模块信息解析器错误 */
    private fun errorInSpecialModuleInfoResolver(message: String) = CangJieExceptionWithAttachments(message)

    /** 其他未分类错误 */
    private fun otherError(message: String) = CangJieExceptionWithAttachments(message)
}

/**
 * 委托式包片段提供者
 *
 * 这是一个优化的包片段提供者实现，负责为模块提供包片段。
 * 它通过委托给实际的模块解析器来获取包片段，并使用包预言（PackageOracle）进行优化。
 *
 * ## 优化策略
 *
 * ### 快速失败优化
 *
 * 在解析器尚未计算前，使用 [packageOracle] 快速判断包是否存在：
 * - 如果 oracle 确定包不存在，直接返回空结果
 * - 如果包可能存在，才委托给实际的解析器
 *
 * 这避免了不必要的解析器创建和符号解析。
 *
 * ### 合成文件特殊处理
 *
 * 合成文件（synthetic files）是编译器生成的虚拟文件，
 * 它们的包可能不在实际的文件系统中。
 * [syntheticFilePackages] 记录了所有合成文件的包名，
 * 确保这些包不会被 oracle 误判为不存在。
 *
 * ## 工作原理
 *
 * ```
 * getPackageFragments(fqName)
 *   ↓
 * certainlyDoesNotExist(fqName)?
 *   ↓ (false - 可能存在)
 * resolverForProject.resolverForModuleDescriptor(module)
 *   ↓
 * delegate.packageFragmentProvider.getPackageFragments(fqName)
 *   ↓
 * 返回包片段列表
 * ```
 *
 * ## 使用场景
 *
 * - 符号解析：查找给定包中的所有类型和函数
 * - 代码补全：列出可导入的包
 * - 包层次遍历：获取子包列表
 *
 * @param M 分析上下文类型
 * @property resolverForProject 项目解析器，用于获取模块解析器
 * @property module 模块描述符
 * @property moduleContent 模块内容，包含源文件和合成文件信息
 * @property packageOracle 包预言，用于快速判断包是否存在
 *
 * @see PackageFragmentProviderOptimized
 * @see PackageOracle
 * @see AbstractResolverForProject
 */
private class DelegatingPackageFragmentProvider<M : ModuleInfo>(
    private val resolverForProject: AbstractResolverForProject<M>,
    private val module: ModuleDescriptor,
    moduleContent: ModuleContent<M>,
    private val packageOracle: PackageOracle
) : PackageFragmentProviderOptimized {
    /**
     * 合成文件的包名集合
     *
     * 记录所有合成文件所在的包，
     * 这些包即使不在文件系统中也应该被认为存在。
     */
    private val syntheticFilePackages = moduleContent.syntheticFiles.map { it.packageFqName }.toSet()

    /**
     * 获取指定包的所有片段
     *
     * @param fqName 包的完全限定名
     * @return 包片段列表，如果包不存在则返回空列表
     */
    @Suppress("OverridingDeprecatedMember", "OVERRIDE_DEPRECATION")
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        if (certainlyDoesNotExist(fqName)) return emptyList()

        @Suppress("DEPRECATION")
        return resolverForProject.resolverForModuleDescriptor(module).packageFragmentProvider.getPackageFragments(fqName)
    }

    /**
     * 收集指定包的所有片段
     *
     * 相比 [getPackageFragments]，此方法将结果添加到现有集合中，
     * 避免创建临时列表。
     *
     * @param fqName 包的完全限定名
     * @param packageFragments 用于收集结果的可变集合
     */
    override fun collectPackageFragments(
        fqName: FqName,
        packageFragments: MutableCollection<PackageFragmentDescriptor>
    ) {
        if (certainlyDoesNotExist(fqName)) return

        resolverForProject.resolverForModuleDescriptor(module)
            .packageFragmentProvider
            .collectPackageFragmentsOptimizedIfPossible(fqName, packageFragments)
    }

    /**
     * 检查包是否为空
     *
     * @param fqName 包的完全限定名
     * @return true 如果包不存在或不包含任何声明
     */
    override fun isEmpty(fqName: FqName): Boolean {
        if (certainlyDoesNotExist(fqName)) return true

        return resolverForProject.resolverForModuleDescriptor(module).packageFragmentProvider.isEmpty(fqName)
    }

    /**
     * 获取指定包的所有子包
     *
     * @param fqName 父包的完全限定名
     * @param nameFilter 子包名称过滤器
     * @return 子包的完全限定名集合
     */
    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        if (certainlyDoesNotExist(fqName)) return emptyList()

        return resolverForProject.resolverForModuleDescriptor(module).packageFragmentProvider.getSubPackagesOf(
            fqName,
            nameFilter
        )
    }

    /**
     * 快速判断包是否确定不存在
     *
     * ## 判断逻辑
     *
     * 1. 如果解析器已经计算过，返回 false（让请求被缓存）
     * 2. 如果是合成文件的包，返回 false（合成包一定存在）
     * 3. 否则，询问 packageOracle
     *
     * @param fqName 包的完全限定名
     * @return true 如果确定包不存在
     */
    private fun certainlyDoesNotExist(fqName: FqName): Boolean {
        // 如果解析器已计算，让请求通过以便被缓存
        if (resolverForProject.isResolverForModuleDescriptorComputed(module)) return false

        // 合成文件的包一定存在
        return !packageOracle.packageExists(fqName) && fqName !in syntheticFilePackages
    }

    /**
     * 返回提供者的字符串表示
     *
     * @return 包含模块和解析器信息的描述字符串
     */
    override fun toString(): String {
        return "DelegatingProvider for $module in ${resolverForProject.name}"
    }
}


fun LibraryEx.isStdlib(): Boolean {
    return (this as? LibraryBridge)?.libraryId?.isStdlib() ?: false
}

fun LibraryId.isStdlib(): Boolean {
    return this.name.endsWith("@stdlib")
}