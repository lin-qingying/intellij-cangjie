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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.context.ProjectContext
import org.cangnova.cangjie.context.withModule
import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.ProjectDescriptor
import org.cangnova.cangjie.descriptors.impl.ModuleDescriptorImpl

import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.AbstractResolverForProject
import org.cangnova.cangjie.resolve.CangJieResolverForModuleFactory
import org.cangnova.cangjie.resolve.IdePackageOracleFactory
import org.cangnova.cangjie.resolve.LanguageSettingsProvider
import org.cangnova.cangjie.resolve.ResolverForModule
import org.cangnova.cangjie.resolve.ResolverForModuleFactory
import org.cangnova.cangjie.resolve.ResolverForProject
import org.cangnova.cangjie.resolve.lazy.IdeaAbsentDescriptorHandler
import com.intellij.openapi.components.service
import com.intellij.openapi.progress.ProgressManager
import com.intellij.openapi.util.ModificationTracker
import com.intellij.psi.search.GlobalSearchScope
import java.util.*

/**
 * 模块内容封装
 *
 * 将模块的分析上下文、合成文件和搜索作用域封装在一起，提供给解析器使用。
 *
 * ## 核心概念
 *
 * ### 合成文件 (Synthetic Files)
 * 合成文件是指那些不在项目源码根目录下的文件，例如：
 * - 代码补全时的临时文件
 * - 代码片段（Code Fragment）
 * - Scratch 文件
 * - 动态生成的虚拟文件
 *
 * ### 模块内容作用域
 * [moduleContentScope] 定义了模块的文件搜索范围，用于：
 * - 限定符号解析的查找范围
 * - 索引构建的文件过滤
 * - 代码导航和引用查找
 *
 * ## 使用场景
 *
 * ```kotlin
 * // 创建模块内容
 * val moduleContent = ModuleContent(
 *     context = myAnalysisContext,
 *     syntheticFiles = listOf(scratchFile),
 *     moduleContentScope = GlobalSearchScope.moduleScope(module)
 * )
 *
 * // 解析器使用模块内容
 * val resolver = factory.createResolverForModule(
 *     descriptor, context, moduleContent, ...
 * )
 * ```
 *
 * @param M 分析上下文类型，协变以支持子类型
 * @param context 模块的分析上下文
 * @param syntheticFiles 模块包含的合成文件集合
 * @param moduleContentScope 模块的全局搜索作用域
 */
data class ModuleContent<out M : AnalysisContext>(
    val context: M,
    val syntheticFiles: Collection<CjFile>,
    val moduleContentScope: GlobalSearchScope
)


/**
 * IntelliJ IDEA 环境下的项目解析器实现
 *
 * 这是仓颉语言在 IDE 环境中的核心解析器，负责管理整个项目的代码分析和符号解析。
 * 它继承自 [AbstractResolverForProject]，为 IDE 特定的功能提供了专门的实现。
 *
 * ## 核心职责
 *
 * 1. **模块管理**: 管理项目中所有模块的解析器实例
 * 2. **合成文件支持**: 处理不在项目源码根目录下的特殊文件
 * 3. **解析器工厂**: 为每个模块创建合适的解析器
 * 4. **性能优化**: 复用已有的解析结果，提升分析性能
 *
 * ## 架构设计
 *
 * ```
 * IdeaResolverForProject (项目级解析器)
 *   ↓
 * 管理多个 ResolverForModule (模块级解析器)
 *   ↓
 * 每个模块解析器包含:
 *   - PackageFragmentProvider (包片段提供者)
 *   - ComponentProvider (依赖注入容器)
 *   - LazyModuleDescriptor (延迟加载的模块描述符)
 * ```
 *
 * ## 解析器分层架构
 *
 * 根据不同的使用场景，项目中会创建多个 `IdeaResolverForProject` 实例：
 *
 * 1. **库解析器** (`resolverForLibrariesName`)
 *    - 只解析外部依赖库（stdlib 等）
 *    - 长期缓存，不会因代码修改而失效
 *    - 提供基础的库符号信息
 *
 * 2. **模块解析器** (`resolverForModulesName`)
 *    - 解析项目源码模块 + 所有依赖库
 *    - 复用库解析器的结果 (通过 delegateResolver)
 *    - 代码修改后会失效并重建
 *
 * 3. **特殊场景解析器** (`resolverForSpecialInfoName`)
 *    - 处理代码补全、语法高亮等临时场景
 *    - 针对特定文件创建轻量级解析器
 *    - 根据文件类型选择合适的基础解析器
 *
 * ## 合成文件处理
 *
 * 合成文件是那些不在项目正常源码目录下的文件，例如：
 * - Scratch 文件：用户在 IDE 中创建的临时文件
 * - 代码片段：代码补全或高亮时的临时代码
 * - 虚拟文件：动态生成的反编译代码
 *
 * 这些文件通过 [syntheticFilesByModule] 参数传入，解析器会为它们创建特殊的作用域。
 *
 * ## 性能优化策略
 *
 * ### 1. 解析器复用 (Delegate Resolver)
 * 通过 [delegateResolver] 参数，可以复用已有解析器的结果：
 * ```kotlin
 * // 库解析器（基础层）
 * val librariesResolver = IdeaResolverForProject(
 *     debugName = "project libraries",
 *     modules = libraryModules,
 *     delegateResolver = null  // 不依赖其他解析器
 * )
 *
 * // 模块解析器（复用库解析器）
 * val modulesResolver = IdeaResolverForProject(
 *     debugName = "project source roots and libraries",
 *     modules = sourceModules + libraryModules,
 *     delegateResolver = librariesResolver  // ✅ 复用库解析器
 * )
 * ```
 *
 * ### 2. 增量分析
 * 使用 [fallbackModificationTracker] 追踪文件修改：
 * - 只有修改的模块才会重新解析
 * - 未修改的模块保持缓存的解析结果
 *
 * ### 3. 懒加载
 * 模块描述符和解析结果都是懒加载的：
 * - 只有真正需要时才创建解析器
 * - 未使用的模块不会占用内存
 *
 * ## 使用示例
 *
 * ### 创建项目解析器
 * ```kotlin
 * val resolver = IdeaResolverForProject(
 *     debugName = "project source roots and libraries",
 *     projectContext = GlobalContext("myProject", project),
 *     projectDescriptor = ProjectDescriptorImpl(),
 *     modules = listOf(moduleContext1, moduleContext2),
 *     syntheticFilesByModule = mapOf(
 *         moduleContext1 to listOf(scratchFile)
 *     ),
 *     delegateResolver = librariesResolver,
 *     fallbackModificationTracker = ProjectRootModificationTracker.getInstance(project)
 * )
 * ```
 *
 * ### 获取模块解析器
 * ```kotlin
 * // 为特定模块获取解析器
 * val moduleResolver = resolver.resolverForModule(myAnalysisContext)
 *
 * // 使用解析器查找符号
 * val packageFragment = moduleResolver.packageFragmentProvider
 *     .getPackageFragments(FqName("std.collection"))
 * ```
 *
 * ## 线程安全
 *
 * 本类的实例是线程安全的：
 * - 模块解析器的创建使用了同步机制
 * - 懒加载的描述符使用了 LockBasedStorageManager
 * - 可以在多线程环境中安全使用
 *
 * ## 生命周期
 *
 * 解析器的生命周期由 [CangJieCacheServiceImpl] 管理：
 * - 项目打开时创建
 * - 代码修改时可能失效重建（根据 invalidateOnOOCB 设置）
 * - 项目关闭时销毁
 *
 * @param debugName 解析器的调试名称，用于日志和诊断
 *                  例如："project libraries [3 libraries: stdlib, std.collection, std.io]"
 * @param projectContext 项目上下文，包含全局配置和服务
 * @param projectDescriptor 项目描述符，管理所有模块的描述符
 * @param modules 需要解析的所有模块（包括源码模块和库模块）
 * @param syntheticFilesByModule 每个模块包含的合成文件映射
 *                                例如：{moduleContext -> [scratchFile1, scratchFile2]}
 * @param delegateResolver 委托解析器，用于复用已有的解析结果（通常是库解析器）
 * @param fallbackModificationTracker 可选的修改追踪器，用于增量分析
 *                                     例如：ProjectRootModificationTracker.getInstance(project)
 *
 * @see AbstractResolverForProject
 * @see ResolverForModule
 * @see CangJieCacheServiceImpl
 */
class IdeaResolverForProject(
    debugName: String,
    projectContext: ProjectContext,
    projectDescriptor: ProjectDescriptor,
    modules: Collection<AnalysisContext>,
    private val syntheticFilesByModule: Map<AnalysisContext, Collection<CjFile>>,
    delegateResolver: ResolverForProject<AnalysisContext>,
    fallbackModificationTracker: ModificationTracker? = null,

    ) : AbstractResolverForProject<AnalysisContext>(

    debugName,
    projectContext,
    projectDescriptor,
    modules,
    fallbackModificationTracker,
    delegateResolver,
    projectContext.project.service<IdePackageOracleFactory>()
) {

    /**
     * 解析器创建时间
     *
     * 记录此解析器实例的创建时间，用于调试和诊断。
     * 可以通过这个时间戳追踪解析器的生命周期和缓存失效情况。
     */
    private val created = Date().toString()


    /**
     * 获取模块解析器工厂
     *
     * 根据分析上下文的类型，返回合适的 [ResolverForModuleFactory] 实例。
     * 当前实现返回统一的 [CangJieResolverForModuleFactory]。
     *
     * ## 设计考虑
     *
     * 未来可能根据不同的模块类型返回不同的工厂：
     * ```kotlin
     * return when {
     *     context.isLibraryContext -> LibraryResolverFactory()
     *     context is ScriptModuleInfo -> ScriptResolverFactory()
     *     else -> CangJieResolverForModuleFactory()
     * }
     * ```
     *
     * @param context 模块的分析上下文
     * @return 模块解析器工厂实例
     */
    private fun getResolverForModuleFactory(context: AnalysisContext): ResolverForModuleFactory {


        return CangJieResolverForModuleFactory()
    }

    /**
     * 获取模块内容
     *
     * 返回指定模块的 [ModuleContent]，包含：
     * - 分析上下文
     * - 合成文件集合（如果有）
     * - 模块的搜索作用域
     *
     * ## 工作原理
     *
     * 从 [syntheticFilesByModule] 映射中查找模块对应的合成文件：
     * - 如果找到，返回包含这些合成文件的 ModuleContent
     * - 如果没有，返回空的合成文件列表
     *
     * ## 使用场景
     *
     * 此方法在创建模块解析器时被调用，用于：
     * - 确定模块需要解析的文件范围
     * - 处理临时文件（Scratch、代码片段等）
     * - 设置符号查找的作用域
     *
     * @param module 目标模块的分析上下文
     * @return 模块内容，包含上下文、合成文件和作用域
     */
    override fun modulesContent(module: AnalysisContext): ModuleContent<AnalysisContext> =
        ModuleContent(module, syntheticFilesByModule[module] ?: emptyList(), module.scope)

    /**
     * 创建模块解析器
     *
     * 为指定的模块创建一个 [ResolverForModule] 实例，这是整个解析系统的核心方法。
     *
     * ## 工作流程
     *
     * ```
     * 1. 构建模块内容 (ModuleContent)
     *    ├─ 获取合成文件
     *    └─ 确定搜索作用域
     *
     * 2. 获取语言版本设置
     *    └─ 从 LanguageSettingsProvider 获取
     *
     * 3. 获取解析器工厂
     *    └─ getResolverForModuleFactory(context)
     *
     * 4. 获取优化选项
     *    └─ ResolveOptimizingOptionsProvider.getOptimizingOptions(...)
     *
     * 5. 创建模块解析器
     *    ├─ 创建 LazyModuleDescriptor
     *    ├─ 创建 PackageFragmentProvider
     *    ├─ 创建 ComponentProvider (依赖注入容器)
     *    └─ 设置 AbsentDescriptorHandler
     *
     * 6. 返回 ResolverForModule
     * ```
     *
     * ## 核心组件
     *
     * ### PackageFragmentProvider
     * 提供包级别的符号信息：
     * - 查找包中的类、函数、属性
     * - 支持懒加载和增量解析
     * - 处理多个源码根的符号聚合
     *
     * ### ComponentProvider
     * 依赖注入容器，管理解析器的各种服务：
     * - TypeResolver：类型解析器
     * - CallResolver：调用解析器
     * - OverloadResolver：重载解析器
     * - ExpressionTypingServices：表达式类型推导服务
     *
     * ### IdeaAbsentDescriptorHandler
     * 处理缺失的符号定义：
     * - 提供友好的错误诊断
     * - 支持 IDE 的错误高亮
     * - 避免因缺失符号导致的崩溃
     *
     * ## 性能优化
     *
     * ### 1. 懒加载策略
     * 模块描述符采用懒加载：
     * ```kotlin
     * class LazyModuleDescriptor {
     *     val declarations by lazy {
     *         // 只在首次访问时解析
     *         parseAndAnalyze()
     *     }
     * }
     * ```
     *
     * ### 2. 增量分析
     * 通过 resolveOptimizingOptions 启用增量分析：
     * - 只重新分析修改的文件
     * - 复用未修改文件的解析结果
     *
     * ### 3. 密封类继承优化
     * 使用 [IdeSealedClassInheritorsProvider] 提供密封类的继承信息：
     * - 利用 IDE 索引快速查找继承者
     * - 避免全项目扫描
     *
     * ## 使用示例
     *
     * ```kotlin
     * // 获取模块描述符
     * val moduleDescriptor = projectResolver.getModuleDescriptor(myContext)
     *
     * // 创建模块解析器
     * val moduleResolver = projectResolver.createResolverForModule(
     *     moduleDescriptor, myContext
     * )
     *
     * // 使用解析器查找符号
     * val fragments = moduleResolver.packageFragmentProvider
     *     .getPackageFragments(FqName("std.collection"))
     *
     * val listClass = fragments
     *     .flatMap { it.getMemberScope().getContributedClassifier(Name.identifier("List")) }
     *     .firstOrNull()
     * ```
     *
     * ## 线程安全
     *
     * 此方法是线程安全的：
     * - 父类 AbstractResolverForProject 确保同一模块只创建一次解析器
     * - LockBasedStorageManager 保证懒加载的线程安全
     * - 可以在多线程环境中并发调用
     *
     * @param descriptor 模块描述符，包含模块的元信息
     * @param context 模块的分析上下文
     * @return 新创建的模块解析器实例
     * @see ResolverForModule
     * @see CangJieResolverForModuleFactory
     * @see LanguageSettingsProvider
     */
    override fun createResolverForModule(descriptor: ModuleDescriptor, context: AnalysisContext): ResolverForModule {
        // 1. 构建模块内容：包含分析上下文、合成文件和搜索作用域
        val moduleContent =
            ModuleContent(context, syntheticFilesByModule[context] ?: listOf(), context.scope)

        val project = projectContext.project

        // 2. 获取语言版本设置
        // 不同的模块可能使用不同的语言版本（例如兼容旧版本的库）
        val languageVersionSettings =
            project.service<LanguageSettingsProvider>().getLanguageVersionSettings(context, project)

        // 3. 获取模块解析器工厂
        // 根据模块类型选择合适的解析器工厂
        val resolverForModuleFactory = getResolverForModuleFactory(context)

        // 4. 获取解析优化选项
        // 包括增量分析、缓存策略等性能优化配置
        val optimizingOptions = ResolveOptimizingOptionsProvider.getOptimizingOptions(project, descriptor, context)

        // 5. 创建模块解析器
        // 这是核心步骤，创建包含所有解析服务的 ResolverForModule 实例
        val resolverForModule = resolverForModuleFactory.createResolverForModule(
            descriptor as ModuleDescriptorImpl,
            projectContext.withModule(descriptor),  // 创建包含当前模块的项目上下文
            moduleContent,
            this,  // 传入项目解析器自身，用于跨模块解析
            languageVersionSettings,
            sealedInheritorsProvider = IdeSealedClassInheritorsProvider,  // IDE 优化：利用索引快速查找密封类继承者
            resolveOptimizingOptions = optimizingOptions,
            absentDescriptorHandlerClass = IdeaAbsentDescriptorHandler::class.java  // 处理缺失符号的错误处理器
        )

        // 6. (可选) 触发解析器创建追踪
        // 用于性能分析和调试，记录解析器创建事件
        // ResolverForModuleComputationTrackerEx.getInstance(project)?.onCreateResolverForModule(descriptor, context)

        return resolverForModule
    }


}

/**
 * ## 文件总结
 *
 * 本文件定义了 IntelliJ IDEA 环境下仓颉语言的项目解析器核心实现。
 *
 * ### 关键类
 *
 * 1. **ModuleContent**
 *    - 封装模块内容（上下文、合成文件、作用域）
 *    - 提供给解析器使用的基础数据结构
 *
 * 2. **IdeaResolverForProject**
 *    - 项目级解析器的 IDE 实现
 *    - 管理所有模块的解析器实例
 *    - 支持解析器复用和性能优化
 *
 * ### 核心工作流程
 *
 * ```
 * 用户编辑代码
 *   ↓
 * CangJieCacheServiceImpl.getResolutionFacade()
 *   ↓
 * ProjectResolutionFacade.facadeForModules
 *   ↓
 * IdeaResolverForProject.resolverForModule()
 *   ↓
 * IdeaResolverForProject.createResolverForModule()
 *   ↓
 * CangJieResolverForModuleFactory.createResolverForModule()
 *   ↓
 * ResolverForModule (包含 PackageFragmentProvider、ComponentProvider 等)
 *   ↓
 * 代码分析、符号解析、类型检查等服务
 * ```
 *
 * ### 性能优化要点
 *
 * - **分层架构**: 库解析器 → 模块解析器 → 特殊场景解析器
 * - **解析器复用**: 通过 delegateResolver 避免重复解析依赖库
 * - **懒加载**: 模块描述符和解析结果按需创建
 * - **增量分析**: 只重新解析修改的文件
 * - **索引优化**: 利用 IDE 索引加速符号查找
 *
 * ### 相关文件
 *
 * - [CangJieCacheServiceImpl]: 解析器的缓存和生命周期管理
 * - [ProjectResolutionFacade]: 项目解析门面，提供高层 API
 * - [CangJieResolverForModuleFactory]: 模块解析器工厂
 * - [AbstractResolverForProject]: 抽象基类，提供通用解析逻辑
 *
 * @see CangJieCacheServiceImpl
 * @see ProjectResolutionFacade
 * @see AbstractResolverForProject
 */
