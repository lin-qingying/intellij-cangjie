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

package org.cangnova.cangjie.descriptors

import com.intellij.openapi.module.Module
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.modules
import com.intellij.openapi.roots.OrderRootType
import com.intellij.openapi.roots.impl.libraries.LibraryEx
import com.intellij.openapi.roots.libraries.Library
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.PlatformDependentAnalyzerServices

/**
 * 模块来源类型枚举
 *
 * 用于标识模块信息的来源,区分不同类型的代码模块。
 * 这有助于在代码分析、依赖解析和符号查找时采用不同的处理策略。
 *
 * ## 枚举值说明
 *
 * ### MODULE
 * **项目模块**
 *
 * 表示当前项目中的模块,包含项目自己的源代码。
 * 这些模块:
 * - 可以被编辑和修改
 * - 拥有完整的源代码和 PSI 树
 * - 可以进行实时的语法和语义分析
 * - 支持重构和代码生成
 *
 * 示例: 项目中的 `main` 模块、`test` 模块
 *
 * ### LIBRARY
 * **库模块**
 *
 * 表示外部库或依赖项,通常以编译后的形式存在。
 * 这些模块:
 * - 只读,不可编辑
 * - 可能只有二进制代码(如 .cjo文件)
 * - 可能提供反编译视图或文档
 * - 用于类型检查和符号解析
 *
 * 示例: 标准库、第三方库
 *
 * ### OTHER
 * **其他类型**
 *
 * 表示特殊用途的模块,如:
 * - 内置类型和函数的虚拟模块
 * - 代码片段或临时代码
 * - 脚本文件
 * - SDK 提供的特殊模块
 *
 * ## 使用示例
 * ```kotlin
 * fun analyzeModule(moduleInfo: ModuleInfo) {
 *     when (moduleInfo.moduleOrigin) {
 *         ModuleOrigin.MODULE -> {
 *             // 分析项目源代码
 *             performFullAnalysis(moduleInfo)
 *         }
 *         ModuleOrigin.LIBRARY -> {
 *             // 从库的元数据中提取信息
 *             extractLibraryMetadata(moduleInfo)
 *         }
 *         ModuleOrigin.OTHER -> {
 *             // 特殊处理
 *             handleSpecialModule(moduleInfo)
 *         }
 *     }
 * }
 * ```
 *
 * @see ModuleInfo 模块信息接口
 */
enum class ModuleOrigin {
    /** 项目模块,包含项目自己的源代码 */
    MODULE,

    /** 库模块,来自外部依赖或标准库 */
    LIBRARY,

    /** 其他类型的模块,如内置模块或特殊用途模块 */
    OTHER
}

/**
 * 内置库依赖策略
 *
 * 定义模块如何依赖内置库（built-ins）的策略。
 *
 * ## 枚举值说明
 *
 * ### NONE
 * 不依赖内置库。
 * 适用场景:
 * - 内置库本身
 * - 完全独立的模块
 *
 * ### AFTER_SDK
 * 在 SDK 之后依赖内置库。
 * 适用场景:
 * - 常规模块
 * - 用户代码
 *
 * ### LAST
 * 最后依赖内置库。
 * 适用场景:
 * - 需要覆盖标准库行为的特殊模块
 * - 测试模块
 */
enum class DependencyOnBuiltIns {
    /** 不依赖内置库 */
    NONE,

    /** 在 SDK 之后依赖内置库 */
    AFTER_SDK,

    /** 最后依赖内置库 */
    LAST
}

/**
 * 模块信息接口
 *
 * **已弃用**：推荐使用 [AnalysisContext] 接口。
 *
 * 该接口表示仓颉语言项目中的一个模块的抽象信息。
 * 模块是代码组织的基本单元,可以是项目源代码模块、外部库,或特殊用途的模块。
 *
 * ## 弃用原因
 *
 * ModuleInfo 接口包含了过多的项目模型特定信息，导致：
 * 1. **过度设计**：包含很多分析器不需要的属性（如 capabilities、analyzerServices）
 * 2. **耦合过紧**：与特定的项目结构和构建系统绑定
 * 3. **难以扩展**：添加新的模块类型需要修改核心接口
 *
 * ## 迁移指南
 *
 * ### 之前的代码
 * ```kotlin
 * fun analyzeModule(moduleInfo: ModuleInfo) {
 *     val scope = moduleInfo.contentScope
 *     val deps = moduleInfo.dependencies()
 *     val services = moduleInfo.analyzerServices
 * }
 * ```
 *
 * ### 迁移后的代码
 * ```kotlin
 * fun analyzeModule(context: AnalysisContext) {
 *     val scope = context.scope
 *     val deps = context.dependencies
 *     // analyzerServices 现在通过全局服务获取
 *     val services = AnalyzerServices.getInstance(context.project)
 * }
 * ```
 *
 * ### 获取上下文
 * ```kotlin
 * // 之前：需要手动创建 ModuleInfo
 * val moduleInfo: ModuleInfo = createModuleInfo(module)
 *
 * // 现在：通过 Provider 获取
 * val context = project.analysisContextProvider.getContextForFile(file)
 * ```
 *
 * ## 设计演进
 *
 * **旧设计** (ModuleInfo)：
 * - 继承自 AnalysisContext
 * - 包含模块特定的属性（name, capabilities, analyzerServices）
 * - 依赖方法 dependencies()
 *
 * **新设计** (AnalysisContext)：
 * - 最小化接口，只包含分析必需的信息
 * - 依赖属性 dependencies（更符合 Kotlin 风格）
 * - 通过 Provider 模式获取，支持不同项目类型
 *
 * @see AnalysisContext 推荐使用的替代接口
 * @see AnalysisContextProvider 上下文获取服务
 * @deprecated 使用 [AnalysisContext] 代替，通过 [AnalysisContextProvider] 获取实例
 */
@Deprecated(
    message = "使用 AnalysisContext 代替。ModuleInfo 包含过多项目模型特定信息。",
    replaceWith = ReplaceWith(
        "AnalysisContext",
        "org.cangnova.cangjie.descriptors.AnalysisContext"
    ),
    level = DeprecationLevel.WARNING
)
interface ModuleInfo : AnalysisContext {

    /**
     * 模块名称
     *
     * 模块的唯一标识名称,用于:
     * - 在 IDE 中显示模块
     * - 生成输出文件名
     * - 依赖声明
     *
     * @return 模块名称对象
     * @see Name
     */
    val name: Name

    // ========== AnalysisContext 实现 ==========

    /**
     * 上下文标识符（实现 [AnalysisContext.contextId]）
     *
     * 默认使用模块名称的字符串表示。
     */
    override val contextId: String
        get() = name.asString()

    /**
     * 所属项目（实现 [AnalysisContext.project]）
     *
     * 该模块所属的 IntelliJ 项目实例。
     * 用于访问项目级的服务和配置。
     *
     * @return IntelliJ 项目对象
     * @see Project
     */
    override val project: Project

    /**
     * 当前上下文的文件搜索范围（实现 [AnalysisContext.scope]）
     *
     * 等同于 [contentScope]，提供向后兼容。
     *
     * @return 全局搜索作用域
     * @see GlobalSearchScope
     * @see contentScope
     */
    override val scope: GlobalSearchScope
        get() = contentScope

    /**
     * 内容搜索作用域
     *
     * 定义该模块包含的所有文件的搜索范围。
     * 通常包括:
     * - 源代码文件
     * - 资源文件
     * - 生成的代码
     *
     * 该作用域用于:
     * - 符号查找和解析
     * - 代码导航
     * - 重构操作
     *
     * @return 全局搜索作用域
     * @see GlobalSearchScope
     */
    val contentScope: GlobalSearchScope

    /**
     * 模块内容作用域
     *
     * 定义该模块及其依赖的所有可访问内容的搜索范围。
     * 默认情况下等同于 [contentScope],但子类可以扩展以包含依赖。
     *
     * 该作用域包括:
     * - 模块自身的内容
     * - 直接和传递依赖的内容
     * - 标准库和运行时
     *
     * @return 扩展的搜索作用域
     */
    val moduleContentScope: GlobalSearchScope
        get() = contentScope

    /**
     * 显示名称
     *
     * 用于在 UI 中显示的模块名称。
     * 默认使用 [name] 的字符串表示,但可以自定义为更友好的显示名称。
     *
     * 示例:
     * - 项目模块: "myproject-main"
     * - 库模块: "cangjie-stdlib-1.0.0"
     * - 内置模块: "Cangjie Built-ins"
     *
     * @return 显示名称字符串
     */
    val displayedName: String get() = name.asString()

    /**
     * 模块来源类型
     *
     * 标识该模块的来源,用于确定如何处理和分析模块。
     *
     * @return 模块来源枚举值
     * @see ModuleOrigin
     */
    val moduleOrigin: ModuleOrigin

    /**
     * 是否为源码上下文（实现 [AnalysisContext.isSourceContext]）
     *
     * 根据 [moduleOrigin] 判断：
     * - [ModuleOrigin.MODULE]：true（源码模块）
     * - [ModuleOrigin.LIBRARY]：false（库模块）
     * - [ModuleOrigin.OTHER]：false（其他类型）
     */
    override val isSourceContext: Boolean
        get() = moduleOrigin == ModuleOrigin.MODULE

    /**
     * 模块能力映射
     *
     * 声明该模块支持的能力集合。
     * 能力是键值对,键是能力类型,值是能力的具体实现或配置。
     *
     * 默认包含 [Capability] 指向自身,表示基本的模块信息能力。
     *
     * 使用示例:
     * ```kotlin
     * val platformCapability = moduleInfo.capabilities[PlatformCapability]
     * if (platformCapability is JvmPlatform) {
     *     // 处理 JVM 平台特定逻辑
     * }
     * ```
     *
     * @return 能力类型到能力实现的映射
     * @see ModuleCapability
     */
    val capabilities: Map<ModuleCapability<*>, Any?>
        get() = mapOf(Capability to this)

    /**
     * 获取模块依赖列表（实现 [AnalysisContext.dependencies]）
     *
     * 返回该模块直接依赖的所有模块。
     * 默认返回空列表,表示无依赖(如内置模块)。
     *
     * 依赖顺序很重要:
     * - 先声明的依赖优先级更高
     * - 符号解析按依赖顺序进行
     * - 可能影响编译顺序
     *
     * @return 依赖的模块信息列表
     */
    override val dependencies: List<ModuleInfo>
        get() = emptyList()

    /**
     * 分析器服务
     *
     * 提供平台相关的代码分析服务,包括:
     * - 类型检查
     * - 符号解析
     * - 语义分析
     * - 平台特定的优化
     *
     * @return 分析器服务实例
     * @see PlatformDependentAnalyzerServices
     */
    val analyzerServices: PlatformDependentAnalyzerServices

    /**
     * 获取对内置库的依赖类型
     *
     * 确定该模块如何依赖内置库(标准库和运行时)。
     * 默认使用 [analyzerServices] 提供的策略。
     *
     * @return 内置库依赖类型
     * @see DependencyOnBuiltIns
     */
    fun dependencyOnBuiltIns(): DependencyOnBuiltIns = analyzerServices.dependencyOnBuiltIns()

    companion object {
        /**
         * 模块信息能力常量
         *
         * 用于在 [capabilities] 映射中查询基本的模块信息能力。
         */
        val Capability = ModuleCapability<ModuleInfo>("ModuleInfo")
    }
}

/**
 * 源代码模块信息接口
 *
 * 该接口表示包含源代码的项目模块,是 [ModuleInfo] 的特化版本。
 * 源代码模块是可以编辑和修改的模块,与只读的库模块不同。
 *
 * ## 核心特征
 *
 * ### 1. 可编辑性
 * 源代码模块包含项目的源代码文件,用户可以:
 * - 编辑和修改代码
 * - 添加或删除文件
 * - 执行重构操作
 * - 运行代码生成
 *
 * ### 2. 实时分析
 * IDE 会对源代码模块进行实时的语法和语义分析:
 * - 语法高亮
 * - 错误检查
 * - 代码补全
 * - 快速修复
 *
 * ### 3. 构建和编译
 * 源代码模块参与项目的构建过程:
 * - 编译为目标代码
 * - 生成输出文件
 * - 打包和部署
 *
 * ## 与 IntelliJ Module 的关系
 *
 * [module] 属性提供对 IntelliJ 平台 Module 对象的访问,通过它可以:
 * - 获取模块的根目录
 * - 访问模块的依赖关系
 * - 读取模块配置
 * - 获取源码根和资源根
 *
 * ## 典型使用场景
 *
 * ### 获取模块的源代码根目录
 * ```kotlin
 * fun getSourceRoots(moduleSourceInfo: ModuleSourceInfo): Array<VirtualFile> {
 *     val rootManager = ModuleRootManager.getInstance(moduleSourceInfo.module)
 *     return rootManager.sourceRoots
 * }
 * ```
 *
 * ### 检查模块依赖
 * ```kotlin
 * fun checkDependencies(moduleSourceInfo: ModuleSourceInfo) {
 *     val rootManager = ModuleRootManager.getInstance(moduleSourceInfo.module)
 *     val dependencies = rootManager.dependencies
 *
 *     for (depModule in dependencies) {
 *         println("依赖模块: ${depModule.name}")
 *     }
 * }
 * ```
 *
 * ### 区分源代码模块和库模块
 * ```kotlin
 * fun processModuleInfo(moduleInfo: ModuleInfo) {
 *     when (moduleInfo) {
 *         is ModuleSourceInfo -> {
 *             // 处理源代码模块
 *             println("源代码模块: ${moduleInfo.module.name}")
 *             analyzeSourceCode(moduleInfo)
 *         }
 *         else -> {
 *             // 处理库模块或其他类型
 *             println("非源代码模块: ${moduleInfo.displayedName}")
 *             loadMetadata(moduleInfo)
 *         }
 *     }
 * }
 * ```
 *
 * ### 获取模块的输出目录
 * ```kotlin
 * fun getOutputPath(moduleSourceInfo: ModuleSourceInfo): String? {
 *     val compilerModuleExtension = CompilerModuleExtension.getInstance(moduleSourceInfo.module)
 *     return compilerModuleExtension?.compilerOutputPath?.path
 * }
 * ```
 *
 * ## 模块类型示例
 *
 * 在仓颉项目中,源代码模块通常包括:
 * - **主模块**: 包含应用的主要代码
 * - **测试模块**: 包含单元测试和集成测试
 * - **示例模块**: 包含示例代码和演示
 *
 * ## 与库模块的对比
 *
 * | 特性 | 源代码模块 (ModuleSourceInfo) | 库模块 |
 * |------|------------------------------|--------|
 * | 可编辑 | ✅ 是 | ❌ 否 |
 * | 实时分析 | ✅ 完整 | ⚠️ 有限 |
 * | 包含源码 | ✅ 是 | ❌ 通常只有编译后代码 |
 * | 可重构 | ✅ 是 | ❌ 否 |
 * | 参与构建 | ✅ 是 | ❌ 否 |
 * | Module 对象 | ✅ 有 | ❌ 无 |
 *
 * @see ModuleInfo 模块信息基础接口
 * @see Module IntelliJ 平台的模块接口
 * @see ModuleOrigin.MODULE 源代码模块的来源类型
 */
interface ModuleSourceInfo : ModuleInfo {
    /**
     * IntelliJ 平台的模块对象
     *
     * 提供对 IntelliJ Platform Module 的直接访问,用于:
     * - 获取模块配置信息
     * - 访问模块的文件结构
     * - 查询模块依赖
     * - 获取构建输出路径
     * - 访问模块级的服务和扩展
     *
     * 通过该对象可以访问 IntelliJ 平台提供的所有模块相关功能。
     *
     * @return IntelliJ Module 对象
     * @see Module
     */
    val module: Module

}