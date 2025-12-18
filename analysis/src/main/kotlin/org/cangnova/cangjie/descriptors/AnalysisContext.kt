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

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.psi.CjFile

/**
 * 分析上下文接口
 *
 * 这是代码分析器所需的最小信息集合，表示一个可分析的代码单元。
 *
 * ## 设计原则
 *
 * **最小化**：只包含分析器必须的信息，避免暴露项目模型细节。
 *
 * **视图模式**：这不是项目模型本身，而是项目模型为分析器提供的"视图"。
 * 具体的项目模型（如 CjModule、CjProject）负责实现此接口。
 *
 * **解耦**：分析器不依赖具体的项目结构，可以适配不同的项目类型
 * （CJPM、KCJPM、单文件脚本等）。
 *
 * ## 核心概念
 *
 * ### 上下文 vs 模块
 * - **上下文 (Context)**：分析器视角，关注"需要分析什么"
 * - **模块 (Module)**：项目模型视角，关注"项目如何组织"
 *
 * 一个模块可以创建一个上下文，但上下文也可以来自其他来源（临时文件、代码片段等）。
 *
 * ### 作用域
 * - [scope]：当前上下文包含的文件范围
 * - [scopeWithDependencies]：当前上下文及其所有依赖的文件范围
 *
 * ## 使用示例
 *
 * ### 基本使用
 * ```kotlin
 * fun analyzeCode(context: AnalysisContext) {
 *     // 获取当前上下文的文件范围
 *     val files = FilenameIndex.getAllFiles(context.scope)
 *
 *     // 在包含依赖的范围内解析符号
 *     val symbol = resolveSymbol(symbolName, context.scopeWithDependencies)
 * }
 * ```
 *
 * ### 遍历依赖
 * ```kotlin
 * fun collectAllDependencies(context: AnalysisContext): Set<AnalysisContext> {
 *     val result = mutableSetOf<AnalysisContext>()
 *     val queue = ArrayDeque<AnalysisContext>()
 *     queue.add(context)
 *
 *     while (queue.isNotEmpty()) {
 *         val current = queue.removeFirst()
 *         if (result.add(current)) {
 *             queue.addAll(current.dependencies)
 *         }
 *     }
 *
 *     return result
 * }
 * ```
 *
 * ### 类型区分
 * ```kotlin
 * fun analyzeWithStrategy(context: AnalysisContext) {
 *     if (context.isSourceContext) {
 *         // 源码上下文：进行完整的语法语义分析
 *         performFullAnalysis(context)
 *     } else {
 *         // 库上下文：只加载元数据和符号信息
 *         loadMetadata(context)
 *     }
 * }
 * ```
 *
 * ## 实现指南
 *
 * 从项目模块创建上下文：
 * ```kotlin
 * class MyModuleAnalysisContext(private val module: MyModule) : AnalysisContext {
 *     override val contextId = module.name
 *     override val project = module.intellijProject
 *
 *     override val scope by lazy {
 *         // 基于模块的源码根创建作用域
 *         val roots = module.sourceRoots
 *         ModuleScope(project, roots)
 *     }
 *
 *     override val dependencies by lazy {
 *         // 将模块依赖转换为分析上下文
 *         module.getDependencies().map { it.asAnalysisContext() }
 *     }
 *
 *     override val isSourceContext = true
 * }
 * ```
 */
interface AnalysisContext {

    /**
     * 上下文标识符
     *
     * 用于唯一标识这个分析上下文，主要用于：
     * - 缓存键生成
     * - 调试日志输出
     * - 错误消息中的上下文标识
     *
     * 对于模块上下文，通常使用模块名称。
     * 对于临时文件，可以使用文件路径或生成的唯一标识。
     *
     * @return 上下文的唯一标识字符串
     */
    val contextId: String

    /**
     * 所属 IntelliJ 项目
     *
     * 用于访问项目级别的服务和配置。
     *
     * @return IntelliJ 项目实例
     * @see Project
     */
    val project: Project

    /**
     * 当前上下文的文件搜索范围
     *
     * 定义此上下文包含的所有文件的范围，用于：
     * - 文件查找和索引
     * - 符号搜索限定
     * - 代码导航和引用查找
     *
     * **注意**：此范围不包含依赖，仅包含当前上下文自身的文件。
     * 如需包含依赖，使用 [scopeWithDependencies]。
     *
     * @return 全局搜索作用域
     * @see GlobalSearchScope
     * @see scopeWithDependencies
     */
    val scope: GlobalSearchScope

    /**
     * 依赖的分析上下文列表
     *
     * 返回此上下文直接依赖的所有上下文。
     *
     * **依赖顺序**：
     * - 列表顺序反映依赖优先级
     * - 符号解析时按顺序查找
     * - 可能影响类型推导和重载解析
     *
     * **传递性**：
     * - 此属性只返回直接依赖
     * - 如需传递依赖，需递归遍历
     * - 可使用 [allDependencies] 扩展函数获取所有传递依赖
     *
     * @return 直接依赖的上下文列表
     */
    val dependencies: List<AnalysisContext>

    /**
     * 是否为源码上下文
     *
     * 区分上下文的类型，用于选择不同的分析策略：
     * - `true`：源码上下文，包含可编辑的源代码文件
     * - `false`：库上下文或其他类型，通常是只读的编译产物
     *
     * ## 使用场景
     *
     * ### 源码上下文 (isSourceContext = true)
     * - 项目自己的模块
     * - 可编辑和修改
     * - 需要完整的语法语义分析
     * - 支持重构和代码生成
     *
     * ### 库上下文 (isSourceContext = false)
     * - 外部依赖库
     * - 只读，不可修改
     * - 从元数据加载符号信息
     * - 可能提供反编译视图
     *
     * @return true 表示源码上下文，false 表示库或其他类型
     */
    val isSourceContext: Boolean
}

/**
 * 获取包含依赖的完整搜索范围
 *
 * 返回一个联合作用域，包含当前上下文及其所有直接依赖的文件范围。
 *
 * **注意**：
 * - 仅包含直接依赖，不递归传递依赖
 * - 如需所有传递依赖，使用 [scopeWithAllDependencies]
 * - 结果会被缓存以提高性能
 *
 * ## 使用场景
 * - 符号解析：在当前模块和依赖中查找符号
 * - 代码补全：提供当前模块和依赖的所有可见符号
 * - 类型检查：验证类型在当前上下文中可见
 *
 * @return 包含当前上下文和直接依赖的联合作用域
 */
val AnalysisContext.scopeWithDependencies: GlobalSearchScope
    get() {
        val allScopes = listOf(scope) + dependencies.map { it.scope }
        return GlobalSearchScope.union(allScopes)
    }

/**
 * 获取所有传递依赖的分析上下文
 *
 * 使用广度优先搜索收集当前上下文的所有传递依赖，包括：
 * - 直接依赖
 * - 间接依赖（依赖的依赖）
 * - 多层依赖链
 *
 * **去重保证**：每个依赖只会出现一次，即使存在菱形依赖。
 *
 * ## 使用示例
 * ```kotlin
 * // 获取模块的完整依赖图
 * val allDeps = moduleContext.allDependencies
 * println("模块 ${moduleContext.contextId} 共有 ${allDeps.size} 个传递依赖")
 *
 * // 检查是否依赖某个特定库
 * val usesStdlib = allDeps.any { it.contextId == "stdlib" }
 * ```
 *
 * @return 所有传递依赖的集合（不包含当前上下文自身）
 */
val AnalysisContext.allDependencies: Set<AnalysisContext>
    get() {
        val result = mutableSetOf<AnalysisContext>()
        val queue = ArrayDeque<AnalysisContext>()
        queue.addAll(dependencies)

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            if (result.add(current)) {
                queue.addAll(current.dependencies)
            }
        }

        return result
    }

/**
 * 获取包含所有传递依赖的完整搜索范围
 *
 * 返回一个联合作用域，包含当前上下文及其所有传递依赖的文件范围。
 *
 * **性能提示**：
 * - 此操作会遍历整个依赖树
 * - 对于大型项目，结果可能包含大量文件
 * - 建议在必要时使用，优先使用 [scopeWithDependencies]
 *
 * @return 包含当前上下文和所有传递依赖的联合作用域
 */
val AnalysisContext.scopeWithAllDependencies: GlobalSearchScope
    get() {
        val allScopes = listOf(scope) + allDependencies.map { it.scope }
        return GlobalSearchScope.union(allScopes)
    }

/**
 * 判断是否为库上下文
 *
 * 这是 [isSourceContext] 的反向便捷方法。
 *
 * @return true 表示库上下文，false 表示源码或其他类型
 */
val AnalysisContext.isLibraryContext: Boolean
    get() = !isSourceContext

/**
 * 获取项目源码模块
 *
 * 返回当前分析上下文对应的源码模块列表。这是从旧的 `ModuleInfo.projectSourceModules()` 迁移而来的兼容方法。
 *
 * ## 行为说明
 *
 * - 如果当前上下文是源码上下文 ([isSourceContext] = true)，返回包含自身的列表
 * - 如果当前上下文不是源码上下文（如库上下文），返回空列表
 *
 * ## 使用场景
 *
 * 主要用于需要区分源码模块和库模块的场景，例如：
 * - 包缓存服务中的模块过滤
 * - 索引构建时的作用域限定
 * - 诊断信息收集
 *
 * ## 迁移说明
 *
 * 旧代码：
 * ```kotlin
 * val sourceModules = moduleInfo.projectSourceModules()
 * ```
 *
 * 新代码：
 * ```kotlin
 * val sourceModules = context.projectSourceModules()
 * ```
 *
 * @return 源码上下文列表，如果当前是源码上下文则包含自身，否则为空列表
 */
fun AnalysisContext.projectSourceModules(): List<AnalysisContext> {
    return when {
        isSourceContext -> listOf(this)
        else -> emptyList()
    }
}

/**
 * 不在内容根下的文件的分析上下文
 *
 * 用于表示那些不属于任何正常模块的文件，例如：
 * - Scratch 文件
 * - 临时文件
 * - 从外部打开的独立文件
 *
 * 这些文件没有正常的模块结构，因此提供一个空的依赖列表和空作用域。
 *
 * @property project IntelliJ 项目实例
 * @property file 可选的 CjFile 实例（可能为 null）
 */
class NotUnderContentRootModuleInfo(
    override val project: Project,
    val file:  CjFile? = null
) : AnalysisContext {
    override val contextId: String = "NotUnderContentRoot"

    override val scope: GlobalSearchScope = file?.let {
        GlobalSearchScope.fileScope(project, it.virtualFile)
    } ?: GlobalSearchScope.EMPTY_SCOPE

    override val dependencies: List<AnalysisContext> = listOf(this)

    override val isSourceContext: Boolean = false

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is NotUnderContentRootModuleInfo) return false
        return project == other.project && file == other.file
    }

    override fun hashCode(): Int {
        var result = project.hashCode()
        result = 31 * result + (file?.hashCode() ?: 0)
        return result
    }

    override fun toString(): String = "NotUnderContentRootModuleInfo(file=$file)"
}

