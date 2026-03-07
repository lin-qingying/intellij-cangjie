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
 * 分析器外观（Analyzer Facade）
 *
 * 本文件提供了仓颉代码分析系统的外观接口，是连接 IDE 和分析引擎的桥梁。
 * 它定义了代码解析、类型检查和符号解析的核心抽象。
 *
 * ## 核心概念
 *
 * ### 分析上下文 (AnalysisContext)
 *
 * 分析上下文代表一个需要分析的代码范围，可以是：
 * - **源码模块**: 项目中的源代码
 * - **库模块**: 依赖的外部库
 * - **特殊上下文**: 用于代码补全、语法高亮等临时场景
 *
 * 分析上下文的扩展类型：
 * - [TrackableModuleInfo]: 可追踪修改的上下文，用于增量分析
 * - [DerivedAnalysisContext]: 派生上下文，基于原始上下文提供不同视图
 *
 * ### 解析器层次结构
 *
 * ```
 * ResolverForProject (项目解析器)
 *   ↓
 * 管理所有模块的 AnalysisContext → ModuleDescriptor 映射
 *   ↓
 * ResolverForModule (模块解析器)
 *   ↓
 * 提供 PackageFragmentProvider (包片段提供者)
 * 提供 ComponentProvider (依赖注入容器)
 *   ↓
 * 符号解析、类型检查、代码补全等
 * ```
 *
 * ### 工作流程
 *
 * ```
 * IDE 请求解析文件
 *   ↓
 * 获取文件所属的 AnalysisContext
 *   ↓
 * ResolverForProject.resolverForModule(context)
 *   ↓
 * 创建/获取 ModuleDescriptor
 *   ↓
 * ResolverForModuleFactory.createResolverForModule()
 *   ↓
 * 创建 ResolveSession (懒加载解析会话)
 *   ↓
 * 返回 ResolverForModule (包含 PackageFragmentProvider)
 *   ↓
 * IDE 使用解析器进行符号查找、类型检查等
 * ```
 *
 * ## 主要组件
 *
 * - [ResolverForProject]: 项目级解析器，管理所有模块
 * - [ResolverForModule]: 模块级解析器，提供包片段和依赖注入容器
 * - [ResolverForModuleFactory]: 模块解析器工厂，负责创建模块解析器
 * - [LazyModuleDependencies]: 懒加载的模块依赖关系
 * - [LanguageSettingsProvider]: 语言版本设置提供者
 *
 * ## 设计模式
 *
 * ### 外观模式 (Facade Pattern)
 *
 * 本文件使用外观模式简化了复杂的分析系统接口：
 * - 隐藏了内部的解析会话、声明提供者、存储管理器等复杂细节
 * - 提供了简单的 `resolverForModule(context)` API
 * - 将多个子系统（PSI、描述符、类型系统）封装在统一的接口后
 *
 * ### 工厂模式 (Factory Pattern)
 *
 * [ResolverForModuleFactory] 使用工厂模式创建解析器：
 * - 支持自定义工厂实现（如 [CangJieResolverForModuleFactory]）
 * - 提供向后兼容的重载方法
 * - 允许扩展和定制解析器创建过程
 *
 * ### 懒加载模式 (Lazy Loading)
 *
 * [LazyModuleDependencies] 使用懒加载避免循环依赖：
 * - 依赖关系仅在首次访问时计算
 * - 通过 `StorageManager.createLazyValue` 实现线程安全
 * - 避免模块初始化时的性能问题
 *
 * @see ResolverForProject
 * @see ResolverForModule
 * @see AnalysisContext
 * @see ModuleDescriptor
 */

import com.intellij.openapi.project.Project
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.config.LanguageVersionSettingsImpl
import org.cangnova.cangjie.container.ComponentProvider
import org.cangnova.cangjie.container.get
import org.cangnova.cangjie.context.ModuleContext
import org.cangnova.cangjie.context.ProjectContext
import org.cangnova.cangjie.context.withModule
import org.cangnova.cangjie.descriptors.CjProjectDescriptorService
import org.cangnova.cangjie.descriptors.ModuleCapability
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.PackageFragmentProvider
import org.cangnova.cangjie.descriptors.impl.CompositePackageFragmentProvider
import org.cangnova.cangjie.descriptors.impl.ModuleDependencies
import org.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl
import org.cangnova.cangjie.frontend.createContainerForLazyResolve
import org.cangnova.cangjie.moduleinfo.DependencyOnBuiltIns
import org.cangnova.cangjie.moduleinfo.ModuleInfo
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.LazyModuleDependencies.Companion.assertModuleDependencyIsCorrect
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.caches.ModuleContent
import org.cangnova.cangjie.resolve.calls.util.languageVersionSettings
import org.cangnova.cangjie.resolve.extensions.AnalysisHandlerExtension
import org.cangnova.cangjie.resolve.lazy.AbsentDescriptorHandler
import org.cangnova.cangjie.resolve.lazy.ResolveSession
import org.cangnova.cangjie.resolve.lazy.declarations.DeclarationProviderFactoryService
import org.cangnova.cangjie.resolve.scopes.optimization.OptimizingOptions
import org.cangnova.cangjie.storage.StorageManager

/**
 * 可追踪的分析上下文
 *
 * 扩展 [AnalysisContext]，添加修改追踪能力。
 * 用于增量分析和缓存失效检测。
 *
 * ## 使用场景
 *
 * - **增量编译**: 检测源文件是否被修改，决定是否需要重新分析
 * - **缓存失效**: 当模块内容变化时，自动使缓存的描述符失效
 * - **性能优化**: 避免重复分析未修改的代码
 *
 * ## 实现示例
 *
 * ```kotlin
 * class MyModuleContext : TrackableModuleInfo {
 *     override fun createModificationTracker(): ModificationTracker {
 *         return ModificationTracker {
 *             // 返回当前修改计数，例如文件的修改时间戳
 *             fileModificationCount
 *         }
 *     }
 * }
 * ```
 *
 * ## 工作原理
 *
 * [AbstractResolverForProject] 会调用此方法获取修改追踪器：
 * ```
 * 模块首次创建时
 *   ↓
 * 调用 createModificationTracker()
 *   ↓
 * 记录初始修改计数
 *   ↓
 * 后续访问时检查修改计数是否变化
 *   ↓
 * 如果变化，重新创建模块描述符
 * ```
 *
 * @see ModificationTracker
 * @see AbstractResolverForProject.ModuleData
 */
interface TrackableModuleInfo : ModuleInfo {
    /**
     * 创建修改追踪器
     *
     * @return 修改追踪器，用于检测上下文是否已修改
     */
    fun createModificationTracker(): ModificationTracker
}

/**
 * 派生的分析上下文
 *
 * 允许基于原始上下文创建具有不同行为的派生上下文。
 * 派生上下文可以改变依赖关系、可见性规则或其他分析配置。
 *
 * ## 使用场景
 *
 * 1. **平台特定代码**: 将通用代码解析为特定平台（JVM、Native）的视图
 * 2. **测试代码**: 为测试代码提供对生产代码内部符号的访问
 * 3. **代码补全**: 创建临时上下文，提供更宽松的可见性规则
 * 4. **IDE 特殊功能**: 实现特定的 IDE 功能（如"显示实现"）
 *
 * ## 重要约束
 *
 * **派生上下文只能引用派生上下文，普通上下文不能引用派生上下文。**
 *
 * 这个约束在 [LazyModuleDependencies] 中强制执行：
 * ```kotlin
 * assert(dependency !is DerivedAnalysisContext || this is DerivedAnalysisContext) {
 *     "Derived analysis contexts may not be referenced from regular ones"
 * }
 * ```
 *
 * ## 实现示例
 *
 * ```kotlin
 * // 为代码补全创建派生上下文
 * class CompletionAnalysisContext(
 *     override val originalContext: AnalysisContext,
 *     private val completionFile: CjFile
 * ) : DerivedAnalysisContext {
 *     override val contextId = "completion_${originalContext.contextId}"
 *
 *     override val dependencies: List<AnalysisContext>
 *         get() = originalContext.dependencies // 继承原始依赖
 * }
 * ```
 *
 * ## 工作流程
 *
 * ```
 * 用户触发代码补全
 *   ↓
 * 创建 DerivedAnalysisContext (基于当前文件的 originalContext)
 *   ↓
 * ResolverForProject 处理派生上下文
 *   ↓
 * 使用 originalContext 的模块描述符（或创建新的）
 *   ↓
 * 提供符号解析能力
 * ```
 *
 * @property originalContext 原始的分析上下文
 *
 * @see AbstractResolverForProject.isCorrectContext
 * @see LazyModuleDependencies
 */
interface DerivedModuleInfo : ModuleInfo {
    /**
     * 原始的分析上下文
     *
     * 派生上下文基于此原始上下文创建，通常共享同一个模块描述符。
     */
    val originalModuleInfo: ModuleInfo
}

/**
 * 模块解析器
 *
 * 为特定模块提供符号解析能力的核心组件。
 * 每个模块都有自己的解析器实例，包含包片段提供者和依赖注入容器。
 *
 * ## 组成部分
 *
 * ### PackageFragmentProvider
 *
 * 提供对包片段（PackageFragment）的访问，包片段包含：
 * - 顶层函数、变量、属性
 * - 类和接口定义
 * - 类型别名
 * - 枚举定义
 *
 * ### ComponentProvider
 *
 * 依赖注入容器，提供对分析组件的访问：
 * - `ResolveSession`: 懒加载解析会话
 * - `BindingTrace`: 绑定追踪器，记录符号绑定信息
 * - `TypeResolver`: 类型解析器
 * - `CallResolver`: 调用解析器
 * - 其他分析服务
 *
 * ## 使用场景
 *
 * ```kotlin
 * val resolver = resolverForProject.resolverForModule(context)
 *
 * // 1. 获取包片段提供者进行符号查找
 * val packageView = resolver.packageFragmentProvider.getPackageFragments(fqName)
 *
 * // 2. 获取依赖注入容器访问分析服务
 * val resolveSession = resolver.componentProvider.get<ResolveSession>()
 * val descriptor = resolveSession.resolveToDescriptor(element)
 * ```
 *
 * ## 生命周期
 *
 * - **创建**: 由 [ResolverForModuleFactory] 创建
 * - **缓存**: 在 [AbstractResolverForProject] 中缓存
 * - **失效**: 当模块内容变化时，重新创建
 *
 * @property packageFragmentProvider 包片段提供者，用于符号查找
 * @property componentProvider 依赖注入容器，提供分析服务
 *
 * @see ResolverForProject
 * @see ResolverForModuleFactory
 * @see PackageFragmentProvider
 * @see ComponentProvider
 */
class ResolverForModule(
    val packageFragmentProvider: PackageFragmentProvider,
    val componentProvider: ComponentProvider
)

/**
 * 空解析器
 *
 * [ResolverForProject] 的空实现，用于占位符或链式解析器的终点。
 * 所有操作都会失败或返回空结果。
 *
 * ## 使用场景
 *
 * 1. **委托解析器终点**: 作为解析器链的最后一个节点
 * 2. **默认值**: 当没有有效解析器时的占位符
 * 3. **测试**: 用于单元测试中的 mock 对象
 *
 * ## 行为
 *
 * - `tryGetResolverForModule()`: 返回 null
 * - `resolverForModuleDescriptor()`: 抛出异常
 * - `descriptorForModule()`: 调用 `diagnoseUnknownModuleInfo()` 并抛出异常
 * - `allModules`: 返回空列表
 *
 * @param M 分析上下文类型
 *
 * @see ResolverForProject
 * @see AbstractResolverForProject
 */
class EmptyResolverForProject<M : ModuleInfo> : ResolverForProject<M>() {
    override val name: String
        get() = "Empty resolver"

    override fun tryGetResolverForModule(context: M): ResolverForModule? = null
    override fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule =
        throw IllegalStateException("$descriptor is not contained in this resolver")

    override fun descriptorForModule(context: M) = diagnoseUnknownModuleInfo(listOf(context))
    override val allModules: Collection<M> = listOf()
    override fun diagnoseUnknownModuleInfo(contexts: List<ModuleInfo>) =
        throw IllegalStateException("Should not be called for $contexts")
}

/**
 * 项目解析器抽象基类
 *
 * 这是项目级代码解析的核心抽象，定义了从分析上下文到模块解析器的转换接口。
 * 它是分析系统的入口点，IDE 通过它获取符号解析能力。
 *
 * ## 核心职责
 *
 * 1. **上下文管理**: 维护所有需要分析的上下文（[allModules]）
 * 2. **描述符映射**: 将 [AnalysisContext] 映射到 [ModuleDescriptor]
 * 3. **解析器创建**: 为每个模块描述符创建或获取 [ResolverForModule]
 * 4. **错误诊断**: 当请求未知上下文时提供详细的错误信息
 *
 * ## 类型参数
 *
 * @param M 分析上下文类型，必须继承自 [AnalysisContext]
 *
 * ## 主要 API
 *
 * ### resolverForModule(context: M)
 *
 * 获取指定上下文的模块解析器：
 * ```kotlin
 * val resolver = resolverForProject.resolverForModule(myContext)
 * val descriptor = resolver.packageFragmentProvider.getContributedClassifier(name, location)
 * ```
 *
 * ### descriptorForModule(context: M)
 *
 * 获取指定上下文的模块描述符：
 * ```kotlin
 * val moduleDescriptor = resolverForProject.descriptorForModule(context)
 * val dependencies = moduleDescriptor.allDependencyModules
 * ```
 *
 * ### tryGetResolverForModule(context: M)
 *
 * 尝试获取解析器，如果上下文无效则返回 null 而不抛出异常：
 * ```kotlin
 * val resolver = resolverForProject.tryGetResolverForModule(context)
 * if (resolver != null) {
 *     // 使用解析器
 * }
 * ```
 *
 * ## 解析器链
 *
 * 项目通常有多个解析器组成链式结构：
 *
 * ```
 * SpecialInfoResolver (代码补全/高亮)
 *   ↓ (委托)
 * ModulesResolver (源码模块)
 *   ↓ (委托)
 * LibrariesResolver (依赖库)
 *   ↓ (委托)
 * EmptyResolver (终点)
 * ```
 *
 * 每个解析器负责特定范围的上下文，如果无法处理则委托给下一个。
 *
 * ## 实现类
 *
 * - [AbstractResolverForProject]: 通用实现，管理描述符缓存
 * - [EmptyResolverForProject]: 空实现，用于链的终点
 *
 * ## 线程安全
 *
 * 实现类通常使用 [StorageManager] 保证线程安全。
 *
 * @property allModules 此解析器管理的所有分析上下文
 * @property name 解析器名称，用于错误消息和调试
 *
 * @see AbstractResolverForProject
 * @see ResolverForModule
 * @see AnalysisContext
 * @see ModuleDescriptor
 */
abstract class ResolverForProject<M : ModuleInfo> {
    /**
     * 此解析器管理的所有模块上下文
     */
    abstract val allModules: Collection<M>

    /**
     * 获取模块解析器
     *
     * 这是主要的公共 API，通过上下文获取解析器。
     *
     * @param context 分析上下文
     * @return 模块解析器
     * @throws IllegalStateException 如果上下文不在 [allModules] 中
     */
    fun resolverForModule(context: M): ResolverForModule =
        resolverForModuleDescriptor(descriptorForModule(context))

    /**
     * 解析器名称，用于错误消息和调试
     */
    abstract val name: String

    /**
     * 获取模块描述符
     *
     * @param context 分析上下文
     * @return 模块描述符
     * @throws IllegalStateException 如果上下文不在 [allModules] 中
     */
    abstract fun descriptorForModule(context: M): ModuleDescriptor

    /**
     * 通过模块描述符获取解析器
     *
     * @param descriptor 模块描述符
     * @return 模块解析器
     * @throws IllegalStateException 如果描述符未注册到此解析器
     */
    abstract fun resolverForModuleDescriptor(descriptor: ModuleDescriptor): ResolverForModule

    /**
     * 诊断未知上下文
     *
     * 当请求的上下文不在 [allModules] 中时被调用。
     * 实现类应生成详细的错误报告，包括上下文信息和可能的原因。
     *
     * @param contexts 未知的上下文列表
     * @throws CangJieExceptionWithAttachmentsImpl 带诊断信息的异常
     */
    abstract fun diagnoseUnknownModuleInfo(contexts: List<ModuleInfo>): Nothing

    override fun toString() = name

    /**
     * 尝试获取模块解析器
     *
     * 与 [resolverForModule] 类似，但如果上下文无效则返回 null 而不抛出异常。
     *
     * @param context 分析上下文
     * @return 模块解析器，如果上下文无效则返回 null
     */
    abstract fun tryGetResolverForModule(context: M): ResolverForModule?

    companion object {
        /** 库解析器名称标识 */
        const val resolverForLibrariesName = "project libraries"

        /** 模块解析器名称标识 */
        const val resolverForModulesName = "project source roots and libraries"

        /** 特殊信息解析器名称标识（用于代码补全/高亮） */
        const val resolverForSpecialInfoName = "completion/highlighting in "

    }
}

/**
 * 解析器计算追踪器
 *
 * 用于监控和追踪模块解析器的创建过程。
 * 这是一个可选的扩展点，允许外部组件观察解析器的计算时机。
 *
 * ## 使用场景
 *
 * 1. **性能监控**: 记录解析器创建的时间，识别性能瓶颈
 * 2. **调试工具**: 在开发模式下跟踪解析器创建顺序
 * 3. **统计信息**: 收集解析器使用统计，用于优化
 * 4. **增量构建**: 追踪哪些模块被重新分析
 *
 * ## 实现示例
 *
 * ```kotlin
 * class MyResolverTracker : ResolverForModuleComputationTracker {
 *     override fun onResolverComputed(context: AnalysisContext) {
 *         LOG.info("Resolver computed for context: ${context.contextId}")
 *         // 记录时间戳、更新统计等
 *     }
 * }
 * ```
 *
 * ## 注册方式
 *
 * 通过 IntelliJ Platform 的组件系统注册：
 * ```xml
 * <projectService
 *     serviceInterface="org.cangnova.cangjie.resolveName.ResolverForModuleComputationTracker"
 *     serviceImplementation="com.example.MyResolverTracker"/>
 * ```
 *
 * ## 调用时机
 *
 * 在 [AbstractResolverForProject.resolverForModuleDescriptorImpl] 中，
 * 解析器创建后立即调用：
 *
 * ```
 * resolverByModuleDescriptor.getOrPut(descriptor) {
 *     ResolverForModuleComputationTracker.getInstance(project)
 *         ?.onResolverComputed(module)  // ← 调用追踪器
 *
 *     createResolverForModule(descriptor, module)
 * }
 * ```
 *
 * @see ResolverForModule
 * @see AbstractResolverForProject
 */
interface ResolverForModuleComputationTracker {
    /**
     * 解析器计算完成回调
     *
     * 当为给定上下文创建解析器时被调用。
     *
     * @param context 已创建解析器的分析上下文
     */
    fun onResolverComputed(context: ModuleInfo)

    companion object {
        /**
         * 获取项目的追踪器实例
         *
         * @param project 项目实例
         * @return 追踪器实例，如果未注册则返回 null
         */
        fun getInstance(project: Project): ResolverForModuleComputationTracker? =
            project.getComponent(ResolverForModuleComputationTracker::class.java) ?: null
    }
}

/**
 * 模块解析器工厂抽象基类
 *
 * 负责创建 [ResolverForModule] 实例的工厂接口。
 * 这是分析系统的扩展点，允许自定义解析器创建逻辑。
 *
 * ## 工厂方法演化
 *
 * 该类提供了多个重载的 `createResolverForModule` 方法，用于向后兼容：
 *
 * ### 完整版本（推荐）
 * ```kotlin
 * createResolverForModule(
 *     moduleDescriptor,
 *     moduleContext,
 *     moduleContent,
 *     resolverForProject,
 *     languageVersionSettings,
 *     sealedInheritorsProvider,
 *     resolveOptimizingOptions,        // 优化选项
 *     absentDescriptorHandlerClass      // 缺失描述符处理器
 * )
 * ```
 *
 * ### 简化版本（已弃用）
 * ```kotlin
 * createResolverForModule(
 *     moduleDescriptor,
 *     moduleContext,
 *     moduleContent,
 *     resolverForProject,
 *     languageVersionSettings,
 *     sealedInheritorsProvider
 * )
 * ```
 *
 * ## 类型参数
 *
 * @param M 分析上下文类型，必须继承自 [AnalysisContext]
 *
 * ## 方法参数说明
 *
 * - **moduleDescriptor**: 要创建解析器的模块描述符
 * - **moduleContext**: 模块上下文，提供项目实例和存储管理器
 * - **moduleContent**: 模块内容，包含源文件、合成文件和搜索作用域
 * - **resolverForProject**: 项目解析器，用于解析依赖模块
 * - **languageVersionSettings**: 语言版本设置（如 CangJie 1.0、2.0 等）
 * - **sealedInheritorsProvider**: 密封类继承者提供者
 * - **resolveOptimizingOptions**: 解析优化选项（可选）
 * - **absentDescriptorHandlerClass**: 缺失描述符处理器类（可选）
 *
 * ## 实现类
 *
 * - [CangJieResolverForModuleFactory]: 仓颉语言的默认实现
 *
 * ## 使用场景
 *
 * 1. **自定义分析流程**: 实现特定的分析策略
 * 2. **性能优化**: 添加缓存或预计算逻辑
 * 3. **测试**: 提供 mock 实现用于单元测试
 * 4. **多语言支持**: 为不同语言提供不同的解析器
 *
 * ## 工作流程
 *
 * ```
 * AbstractResolverForProject.createResolverForModule()
 *   ↓
 * ResolverForModuleFactory.createResolverForModule()
 *   ↓
 * 创建 DeclarationProviderFactory (声明提供者工厂)
 *   ↓
 * 创建 DI Container (依赖注入容器)
 *   ↓
 * 创建 ResolveSession (解析会话)
 *   ↓
 * 创建 PackageFragmentProvider (包片段提供者)
 *   ↓
 * 返回 ResolverForModule
 * ```
 *
 * @see ResolverForModule
 * @see CangJieResolverForModuleFactory
 * @see AbstractResolverForProject.createResolverForModule
 */
abstract class ResolverForModuleFactory {
    /**
     * 创建模块解析器（完整版本）
     *
     * 这是推荐使用的方法，提供所有可配置参数。
     *
     * @param moduleDescriptor 模块描述符
     * @param moduleContext 模块上下文
     * @param moduleContent 模块内容
     * @param resolverForProject 项目解析器
     * @param languageVersionSettings 语言版本设置
     * @param sealedInheritorsProvider 密封类继承者提供者
     * @param resolveOptimizingOptions 解析优化选项（可选）
     * @param absentDescriptorHandlerClass 缺失描述符处理器类（可选）
     * @return 模块解析器
     */
    open fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider,
        resolveOptimizingOptions: OptimizingOptions?,
        absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
    ): ResolverForModule {
        @Suppress("DEPRECATION")
        return createResolverForModule(
            moduleDescriptor,
            moduleContext,
            moduleContent,
            resolverForProject,
            languageVersionSettings,
            sealedInheritorsProvider,
            resolveOptimizingOptions
        )
    }

    /**
     * 创建模块解析器（带优化选项）
     *
     * @deprecated 请使用完整版本的方法
     */
    @Deprecated(
        "Left only for compatibility, please use full version",
        ReplaceWith("createResolverForModule(moduleDescriptor, moduleContext, moduleContent, resolverForProject, languageVersionSettings, sealedInheritorsProvider, null, null)")
    )
    open fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider,
        resolveOptimizingOptions: OptimizingOptions?,
    ): ResolverForModule {
        @Suppress("DEPRECATION")
        return createResolverForModule(
            moduleDescriptor,
            moduleContext,
            moduleContent,
            resolverForProject,
            languageVersionSettings,
            sealedInheritorsProvider
        )
    }

    /**
     * 创建模块解析器（基础版本）
     *
     * @deprecated 请使用完整版本的方法
     */
    @Deprecated(
        "Left only for compatibility, please use full version",
        ReplaceWith("createResolverForModule(moduleDescriptor, moduleContext, moduleContent, resolverForProject, languageVersionSettings, sealedInheritorsProvider, null, null)")
    )
    open fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider
    ): ResolverForModule {
        return createResolverForModule(
            moduleDescriptor,
            moduleContext,
            moduleContent,
            resolverForProject,
            languageVersionSettings,
            sealedInheritorsProvider,
            resolveOptimizingOptions = null,
            absentDescriptorHandlerClass = null
        )
    }
}

/**
 * 仓颉语言解析器工厂
 *
 * [ResolverForModuleFactory] 的仓颉语言实现，负责创建仓颉模块的解析器。
 * 这是仓颉分析系统的核心工厂类，集成了所有必需的组件。
 *
 * ## 创建流程
 *
 * ```
 * createResolverForModule()
 *   ↓
 * 1. 创建 DeclarationProviderFactory
 *    - 从 DeclarationProviderFactoryService 获取工厂
 *    - 处理源文件和合成文件
 *   ↓
 * 2. 创建 BindingTrace
 *    - 从 CodeAnalyzerInitializer 创建追踪器
 *    - 记录符号绑定信息
 *   ↓
 * 3. 创建 DI Container
 *    - 调用 createContainerForLazyResolve()
 *    - 注入各种分析服务（TypeResolver、CallResolver 等）
 *   ↓
 * 4. 获取 ResolveSession
 *    - 从容器中获取解析会话
 *    - 提供懒加载的符号解析
 *   ↓
 * 5. 创建 PackageFragmentProvider
 *    - 从 ResolveSession 获取包片段提供者
 *    - 组合成 CompositePackageFragmentProvider
 *   ↓
 * 6. 返回 ResolverForModule
 * ```
 *
 * ## 关键组件说明
 *
 * ### DeclarationProviderFactory
 *
 * 声明提供者工厂，负责为 PSI 元素创建描述符：
 * - 类、接口、枚举的描述符
 * - 函数、属性、变量的描述符
 * - 支持 Stub 索引加速
 *
 * ### BindingTrace
 *
 * 绑定追踪器，记录 PSI 元素到描述符的映射：
 * - 缓存解析结果
 * - 提供双向查询（PSI ↔ Descriptor）
 * - 用于诊断和错误报告
 *
 * ### DI Container
 *
 * 依赖注入容器，管理分析服务的生命周期：
 * - `ResolveSession`: 解析会话
 * - `TypeResolver`: 类型解析器
 * - `CallResolver`: 调用解析器
 * - `DescriptorResolver`: 描述符解析器
 * - 更多分析服务...
 *
 * ### ResolveSession
 *
 * 懒加载解析会话，提供按需解析：
 * - 避免一次性解析整个项目
 * - 支持增量分析
 * - 处理循环依赖
 *
 * ## 使用示例
 *
 * ```kotlin
 * val factory = CangJieResolverForModuleFactory()
 * val resolver = factory.createResolverForModule(
 *     moduleDescriptor = myModuleDescriptor,
 *     moduleContext = myModuleContext,
 *     moduleContent = myModuleContent,
 *     resolverForProject = myResolverForProject,
 *     languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
 *     sealedInheritorsProvider = mySealedInheritorsProvider,
 *     resolveOptimizingOptions = null,
 *     absentDescriptorHandlerClass = null
 * )
 *
 * // 使用解析器
 * val packageFragmentProvider = resolver.packageFragmentProvider
 * val resolveSession = resolver.componentProvider.get<ResolveSession>()
 * ```
 *
 * @see ResolverForModuleFactory
 * @see ResolverForModule
 * @see createContainerForLazyResolve
 */
class CangJieResolverForModuleFactory : ResolverForModuleFactory() {
    override fun <M : ModuleInfo> createResolverForModule(
        moduleDescriptor: ModuleDescriptorImpl,
        moduleContext: ModuleContext,
        moduleContent: ModuleContent<M>,
        resolverForProject: ResolverForProject<M>,
        languageVersionSettings: LanguageVersionSettings,
        sealedInheritorsProvider: SealedClassInheritorsProvider,
        resolveOptimizingOptions: OptimizingOptions?,
        absentDescriptorHandlerClass: Class<out AbsentDescriptorHandler>?
    ): ResolverForModule {

        val project = moduleContext.project
        val (context, syntheticFiles, moduleContentScope) = moduleContent

        val declarationProviderFactory = DeclarationProviderFactoryService.createDeclarationProviderFactory(
            project, moduleContext.storageManager, syntheticFiles,
            moduleContentScope,
            context
        )
        val trace = CodeAnalyzerInitializer.getInstance(project).createTrace()


        val container = createContainerForLazyResolve(

            moduleContext,
            trace,
            declarationProviderFactory,
            moduleContentScope,
            languageVersionSettings,
            absentDescriptorHandlerClass = absentDescriptorHandlerClass
        )
        val providersForModule = arrayListOf(
            container.get<ResolveSession>().getPackageFragmentProvider(),

            )
        return ResolverForModule(
            CompositePackageFragmentProvider(providersForModule, "CompositeProvider for $moduleDescriptor"),
            container
        )
    }

    /**
     * 源模块信息
     *
     * 用于 analyzeFiles 方法的内部模块信息类。
     * 表示要分析的源文件集合及其基本配置。
     *
     * @param name 模块名称
     * @param capabilities 模块能力映射
     * @param dependencies 依赖的模块列表
     * @param dependOnOldBuiltIns 是否依赖旧版内置类型
     */
    private class SourceModuleInfo(
        override val name: Name,
        override val capabilities: Map<ModuleCapability<*>, Any?>,
        dependencies: Iterable<ModuleInfo>,
        private val dependOnOldBuiltIns: Boolean
    ) : ModuleInfo {
        override val dependencies = listOf(this, *dependencies.toList().toTypedArray())
        override val analyzerServices: PlatformDependentAnalyzerServices
            get() = PlatformDependentAnalyzerServicesImpl

        override fun modulesWhoseInternalsAreVisible(): Collection<ModuleInfo> = emptySet()

        override fun dependencyOnBuiltIns(): DependencyOnBuiltIns =
            if (dependOnOldBuiltIns) DependencyOnBuiltIns.LAST
            else DependencyOnBuiltIns.NONE
    }

    companion object {
        /**
         * 分析文件集合
         *
         * 该方法用于分析一组仓颉源文件，创建必要的模块和解析器，并执行完整的代码分析。
         * 这是一个静态方法，主要用于独立的文件分析场景，如命令行编译器、测试工具等。
         *
         * ## 功能说明
         *
         * 1. **创建模块信息**: 根据提供的参数创建 SourceModuleInfo
         * 2. **初始化项目上下文**: 使用文件的项目或创建新的项目上下文
         * 3. **创建解析器工厂**: 使用 CangJieResolverForModuleFactory
         * 4. **创建单模块项目解析器**: 使用 ResolverForSingleModuleProject
         * 5. **执行分析**: 通过扩展点或默认分析器进行代码分析
         *
         * ## 参数说明
         *
         * @param files 要分析的仓颉源文件集合
         * @param moduleName 模块名称
         * @param dependOnBuiltIns 是否依赖内置类型（通常为 true）
         * @param languageVersionSettings 语言版本设置
         * @param targetEnvironment 目标环境配置
         * @param capabilities 模块能力映射，用于扩展模块功能
         * @param explicitProjectContext 显式的项目上下文（可选）
         *
         * @return 分析结果，包含绑定上下文和模块描述符
         *
         * ## 工作流程
         *
         * ```
         * 1. 创建 SourceModuleInfo
         *    - 包含模块名称、能力、依赖关系
         *   ↓
         * 2. 获取项目实例
         *    - 从文件或显式上下文
         *   ↓
         * 3. 创建 ResolverForModuleFactory
         *    - 使用 CangJieResolverForModuleFactory
         *   ↓
         * 4. 创建 ProjectContext
         *    - 如果未提供则创建新的
         *   ↓
         * 5. 创建 ResolverForSingleModuleProject
         *    - 配置搜索范围、语言版本等
         *   ↓
         * 6. 获取模块描述符
         *    - resolver.descriptorForModule(moduleInfo)
         *   ↓
         * 7. 获取分析容器
         *    - resolver.resolverForModule(moduleInfo).componentProvider
         *   ↓
         * 8. 执行扩展点分析（如果有）
         *    - AnalysisHandlerExtension.doAnalysis
         *   ↓
         * 9. 执行默认分析
         *    - LazyTopDownAnalyzer.analyzeDeclarations
         *   ↓
         * 10. 执行分析完成回调
         *    - AnalysisHandlerExtension.analysisCompleted
         *   ↓
         * 11. 返回分析结果
         * ```
         *
         * ## 使用示例
         *
         * ```kotlin
         * val files = listOf(cjFile1, cjFile2)
         * val result = CangJieResolverForModuleFactory.analyzeFiles(
         *     files = files,
         *     moduleName = Name.identifier("myModule"),
         *     dependOnBuiltIns = true,
         *     languageVersionSettings = LanguageVersionSettingsImpl.DEFAULT,
         *     targetEnvironment = CompilerEnvironment,
         *     explicitProjectContext = null,
         *     metadataPartProviderFactory = { EmptyMetadataPartProvider }
         * )
         *
         * if (result.isError()) {
         *     result.throwIfError()
         * } else {
         *     val bindingContext = result.bindingContext
         *     // 使用分析结果
         * }
         * ```
         *
         * @see SourceModuleInfo
         * @see ResolverForSingleModuleProject
         * @see AnalysisHandlerExtension
         * @see LazyTopDownAnalyzer
         */
        fun analyzeFiles(
            files: Collection<CjFile>,
            moduleName: Name,
            dependOnBuiltIns: Boolean,
            languageVersionSettings: LanguageVersionSettings,
            targetEnvironment: TargetEnvironment,
            capabilities: Map<ModuleCapability<*>, Any?> = emptyMap(),
            explicitProjectContext: ProjectContext? = null,
        ): AnalysisResult {
            // 创建源模块信息
            val moduleInfo = SourceModuleInfo(
                moduleName,
                capabilities,
                emptyList(), // 依赖列表
                dependOnBuiltIns
            )

            // 获取项目实例
            val project = files.firstOrNull()?.project
                ?: throw AssertionError("No files to analyze")
            val projectDescriptor = CjProjectDescriptorService.getInstance(project).projectDescriptor

            // 创建解析器工厂
            val resolverForModuleFactory = CangJieResolverForModuleFactory()

            // 创建或使用提供的项目上下文
            val projectContext = explicitProjectContext
                ?: ProjectContext(project, "analyze files")

            // 创建单模块项目解析器
            val resolver = ResolverForSingleModuleProject(
                debugName = "sources for analyze files",
                projectContext = projectContext,
                projectDescriptor = projectDescriptor,
                module = moduleInfo,
                resolverForModuleFactory = resolverForModuleFactory,
                searchScope = GlobalSearchScope.allScope(project),
                languageVersionSettings = languageVersionSettings,
                syntheticFiles = files,

                knownDependencyModuleDescriptors = emptyMap()
            )

            // 获取模块描述符
            val moduleDescriptor = resolver.descriptorForModule(moduleInfo)

            // 获取分析容器
            val container = resolver.resolverForModule(moduleInfo).componentProvider

            // 获取分析处理扩展
            val analysisHandlerExtensions = AnalysisHandlerExtension.EP_NAME.extensionList

            // 获取绑定追踪器
            val trace = container.get<BindingTrace>()

            // 尝试通过扩展点执行分析
            // 如果任何扩展返回非空结果，使用该结果；否则执行默认分析
            var result = analysisHandlerExtensions.firstNotNullOfOrNull { extension ->
                extension.doAnalysis(
                    project,
                    moduleDescriptor,
                    projectContext,
                    files,
                    trace,
                    container
                )
            } ?: run {
                // 默认分析：使用 LazyTopDownAnalyzer
                val analyzer = container.get<LazyTopDownAnalyzer>()
                analyzer.analyzeDeclarations(
                    TopDownAnalysisMode.TopLevelDeclarations,
                    files
                )
                AnalysisResult.success(trace.bindingContext, moduleDescriptor)
            }

            // 允许扩展覆盖分析结果
            result = analysisHandlerExtensions.firstNotNullOfOrNull { extension ->
                extension.analysisCompleted(project, moduleDescriptor, trace, files)
            } ?: result

            return result
        }
    }

}

/**
 * 语言版本设置提供者
 *
 * 负责为不同的分析上下文提供对应的语言版本设置。
 * 语言版本设置决定了哪些语言特性可用、哪些被弃用。
 *
 * ## 语言版本设置内容
 *
 * [LanguageVersionSettings] 包含：
 * - **语言版本**: 例如 CangJie 1.0、1.5、2.0
 * - **API 版本**: 可用的标准库 API 版本
 * - **特性标志**: 实验性特性的开关
 * - **编译器标志**: 编译器行为配置
 *
 * ## 使用场景
 *
 * 1. **多版本支持**: 项目中不同模块使用不同语言版本
 * 2. **渐进式迁移**: 逐步升级到新版本语言
 * 3. **实验性特性**: 为特定模块启用实验特性
 * 4. **平台差异**: 不同平台（JVM、Native）的语言特性差异
 *
 * ## 默认实现
 *
 * ```kotlin
 * LanguageSettingsProvider.Default.getLanguageVersionSettings(context, project)
 * // 返回 LanguageVersionSettingsImpl.DEFAULT
 * ```
 *
 * ## 自定义实现示例
 *
 * ```kotlin
 * class MyLanguageSettingsProvider : LanguageSettingsProvider {
 *     override fun getLanguageVersionSettings(
 *         context: AnalysisContext,
 *         project: Project
 *     ): LanguageVersionSettings {
 *         return when (context) {
 *             is LegacyModuleContext -> LanguageVersionSettingsImpl.CANGJIE_1_0
 *             is ModernModuleContext -> LanguageVersionSettingsImpl.CANGJIE_2_0
 *             else -> LanguageVersionSettingsImpl.DEFAULT
 *         }
 *     }
 * }
 * ```
 *
 * ## IDE 集成
 *
 * IDE 实现 ([IDELanguageSettingsProvider]) 会从项目配置读取语言版本：
 * - cjpm.toml 中的 language-version 配置
 * - 项目设置中的语言级别
 * - 模块特定的配置
 *
 * @see LanguageVersionSettings
 * @see IDELanguageSettingsProvider
 */
interface LanguageSettingsProvider {
    /**
     * 获取语言版本设置
     *
     * @param context 分析上下文
     * @param project 项目实例
     * @return 语言版本设置
     */
    fun getLanguageVersionSettings(
        context: ModuleInfo,
        project: Project
    ): LanguageVersionSettings


    companion object {
        /**
         * 默认实现，返回默认语言版本设置
         */
        object Default : LanguageSettingsProvider {
            override fun getLanguageVersionSettings(
                context: ModuleInfo,
                project: Project
            ) = LanguageVersionSettingsImpl.DEFAULT

        }
    }
}

/**
 * IDE 语言版本设置提供者
 *
 * [LanguageSettingsProvider] 的 IDE 实现，从项目配置中读取语言版本设置。
 *
 * ## 实现策略
 *
 * 当前实现始终返回项目级的语言版本设置：
 * ```kotlin
 * project.languageVersionSettings
 * ```
 *
 * ## 未来扩展
 *
 * 可以根据不同类型的上下文返回不同的设置：
 * ```kotlin
 * when (context) {
 *     is CjModuleAnalysisContext -> module.languageVersionSettings
 *     is LibraryAnalysisContext -> library.languageVersionSettings
 *     else -> project.languageVersionSettings
 * }
 * ```
 *
 * ## 配置来源
 *
 * IDE 的语言版本设置可能来自：
 * - **cjpm.toml**: `language-version = "1.5"`
 * - **项目设置**: File → Project Structure → Language Level
 * - **模块设置**: 模块特定的语言级别
 * - **工具链**: 使用的编译器版本
 *
 * ## 服务注册
 *
 * 在 `cangjie-analysis.xml` 中注册：
 * ```xml
 * <projectService
 *     serviceInterface="org.cangnova.cangjie.resolveName.LanguageSettingsProvider"
 *     serviceImplementation="org.cangnova.cangjie.resolveName.IDELanguageSettingsProvider"/>
 * ```
 *
 * @see LanguageSettingsProvider
 * @see LanguageVersionSettings
 */
internal class IDELanguageSettingsProvider : LanguageSettingsProvider {

    override fun getLanguageVersionSettings(
        context: ModuleInfo,
        project: Project
    ): LanguageVersionSettings {
        return when (context) {
            // 未来可以根据上下文类型返回不同设置
            // is CjModuleAnalysisContext -> context.getLanguageVersionSettings()
            else -> project.languageVersionSettings
        }
    }


}


/**
 * 懒加载模块依赖关系
 *
 * [ModuleDependencies] 的懒加载实现，用于避免模块初始化时的循环依赖问题。
 * 依赖关系仅在首次访问时才计算，通过 [StorageManager] 保证线程安全和单次计算。
 *
 * ## 懒加载机制
 *
 * 依赖关系通过 `StorageManager.createLazyValue` 延迟计算：
 * ```kotlin
 * val dependencies = storageManager.createLazyValue {
 *     // 仅在首次访问 allDependencies 时执行
 *     computeDependencies()
 * }
 * ```
 *
 * ## 依赖收集流程
 *
 * ```
 * allDependencies 首次访问
 *   ↓
 * 1. 添加 firstDependency（如果存在）
 *    - 通常是 SDK 或标准库依赖
 *   ↓
 * 2. 遍历 module.dependencies
 *    - 跳过已添加的 firstDependency
 *    - 验证依赖关系的正确性
 *   ↓
 * 3. 通过 resolverForProject 获取依赖的 ModuleDescriptor
 *   ↓
 * 4. 返回所有依赖的列表
 * ```
 *
 * ## 依赖约束验证
 *
 * ### assertModuleDependencyIsCorrect
 *
 * 验证依赖关系的合法性，防止不兼容的依赖组合：
 *
 * **规则**: 派生上下文只能引用派生上下文，普通上下文不能引用派生上下文。
 *
 * ```kotlin
 * assert(dependency !is DerivedAnalysisContext || this is DerivedAnalysisContext) {
 *     "Derived analysis contexts may not be referenced from regular ones"
 * }
 * ```
 *
 * **原因**: 派生上下文通常有特殊的可见性规则或行为，
 * 如果普通上下文依赖派生上下文，可能导致不一致的符号解析结果。
 *
 * ## 使用示例
 *
 * ```kotlin
 * val moduleDependencies = LazyModuleDependencies(
 *     storageManager = storageManager,
 *     module = myModuleContext,
 *     firstDependency = sdkModule,  // 标准库优先
 *     resolverForProject = resolverForProject
 * )
 *
 * // 首次访问时计算依赖
 * val allDeps = moduleDependencies.allDependencies
 * // [sdkModule, dep1, dep2, dep3]
 *
 * // 后续访问直接返回缓存结果
 * val sameDeps = moduleDependencies.allDependencies
 * ```
 *
 * ## 线程安全
 *
 * 通过 [StorageManager] 保证：
 * - 依赖列表仅计算一次
 * - 多线程并发访问安全
 * - 防止重复计算
 *
 * ## 当前限制
 *
 * - `modulesWhoseInternalsAreVisible`: 返回空集（暂不支持内部可见性）
 * - `directExpectedByDependencies`: 返回空列表（暂不支持 expect/actual）
 * - `allExpectedByDependencies`: 返回空集（暂不支持 expect/actual）
 *
 * 这些限制可能在未来版本中移除，以支持多平台项目和内部可见性。
 *
 * @param M 分析上下文类型
 * @param storageManager 存储管理器，提供懒加载和线程安全
 * @param module 当前模块的分析上下文
 * @param firstDependency 第一个依赖（通常是 SDK），可为 null
 * @param resolverForProject 项目解析器，用于获取依赖的模块描述符
 * @param instantiatedDependencies 已实例化的依赖模块描述符列表，用于跨模块解析场景
 *
 * @see ModuleDependencies
 * @see AbstractResolverForProject
 * @see DerivedAnalysisContext
 * @see StorageManager
 */
class LazyModuleDependencies<M : ModuleInfo>(
    storageManager: StorageManager,
    private val module: M,
    firstDependency: M?,
    private val resolverForProject: AbstractResolverForProject<M>,
    /**
     * 已经实例化的依赖模块描述符
     *
     * 用于跨模块解析场景，当依赖模块（如 std）已经被实例化时，
     * 可以直接使用这些描述符，而不是通过 [resolverForProject] 重新创建。
     *
     * 这解决了 [resolverForProject.descriptorForModule] 返回的
     * [ModuleDescriptorImpl] 其 module 属性指向调用者模块而非实际模块的问题。
     *
     * 使用场景：
     * - 当导入 `std.core.String` 时，需要 std 模块的 [ModuleDescriptor]
     * - 通过此参数传入已实例化的 std 模块描述符
     * - 避免 `getPackage("std.core")` 返回错误的模块引用
     */
    private val instantiatedDependencies: List<ModuleDescriptor> = emptyList()
) : ModuleDependencies {
    companion object {
        /**
         * 验证模块依赖是否正确（通过 ModuleDescriptor）
         *
         * @receiver 当前模块的分析上下文
         * @param dependency 依赖的模块描述符
         */
        private fun ModuleInfo.assertModuleDependencyIsCorrect(dependency: ModuleDescriptor) {
            assertModuleDependencyIsCorrect(dependency.getCapability(ModuleInfo.Capability) ?: return)

        }

        /**
         * 验证模块依赖是否正确（通过 AnalysisContext）
         *
         * 确保派生上下文只被派生上下文引用。
         *
         * @receiver 当前模块的分析上下文
         * @param dependency 依赖的分析上下文
         * @throws AssertionError 如果普通上下文试图引用派生上下文
         */
        private fun ModuleInfo.assertModuleDependencyIsCorrect(dependency: ModuleInfo) {
            assert(dependency !is DerivedModuleInfo || this is DerivedModuleInfo) {
                "Derived analysis contexts may not be referenced from regular ones"
            }
        }
    }

    /**
     * 懒加载的依赖列表
     *
     * 仅在首次访问时计算，后续访问返回缓存结果。
     *
     * **重要**: 依赖列表包含模块自身（放在第一个位置）
     * 这样模块可以通过统一的 packageFragmentProvider 访问自己定义的类型。
     *
     * **依赖来源**:
     * 1. firstDependency（通常是 SDK）
     * 2. instantiatedDependencies（已实例化的跨模块依赖）
     * 3. module.dependencies 中的其他依赖（通过 resolverForProject 解析）
     */
    private val dependencies = storageManager.createLazyValue {
        val moduleDescriptors = mutableSetOf<ModuleDescriptorImpl>()
        firstDependency?.let {
            module.assertModuleDependencyIsCorrect(it)
            moduleDescriptors.add(resolverForProject.descriptorForModule(it))
        }

        // 添加已实例化的依赖（跨模块依赖）
        for (instantiated in instantiatedDependencies) {
            module.assertModuleDependencyIsCorrect(instantiated)
            if (instantiated is ModuleDescriptorImpl) {
                moduleDescriptors.add(instantiated)
            }
        }

        // 处理所有依赖
        for (dependency in module.dependencies) {
            if (dependency == firstDependency) continue
            module.assertModuleDependencyIsCorrect(dependency)

            @Suppress("UNCHECKED_CAST")
            moduleDescriptors.add(resolverForProject.descriptorForModule(dependency as M))
        }

        moduleDescriptors.toList()
    }

    /**
     * 所有依赖模块
     *
     * 包含直接依赖和传递依赖。
     */
    override val allDependencies: List<ModuleDescriptorImpl>
        get() = dependencies()

    /**
     * 内部可见的模块
     *
     * 当前返回空集，未来可能支持内部可见性。
     */
    override val modulesWhoseInternalsAreVisible: Set<ModuleDescriptorImpl>
        get() = emptySet()

    /**
     * 直接的 expected-by 依赖
     *
     * 当前返回空列表，未来可能支持多平台项目。
     */
    override val directExpectedByDependencies: List<ModuleDescriptorImpl>
        get() = emptyList()

    /**
     * 所有的 expected-by 依赖
     *
     * 当前返回空集，未来可能支持多平台项目。
     */
    override val allExpectedByDependencies: Set<ModuleDescriptorImpl>
        get() = emptySet()
}

