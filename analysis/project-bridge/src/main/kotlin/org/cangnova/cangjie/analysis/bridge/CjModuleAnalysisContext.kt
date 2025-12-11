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

package org.cangnova.cangjie.analysis.bridge

import com.intellij.openapi.project.Project
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.descriptors.AnalysisContext
import org.cangnova.cangjie.project.model.CjModule
import org.cangnova.cangjie.project.model.CjDependency

/**
 * CjModule 到 AnalysisContext 的适配器
 *
 * 将 cangjie-project 模块中的 [CjModule] 适配为 analysis 模块所需的 [AnalysisContext]。
 * 这是桥接层的核心实现，负责：
 * - 提供模块的文件作用域
 * - 解析并转换依赖关系
 * - 标识模块类型（源码 vs 库）
 *
 * ## 使用示例
 *
 * ```kotlin
 * val cjModule: CjModule = ...
 * val context: AnalysisContext = cjModule.asAnalysisContext()
 *
 * // 使用上下文进行分析
 * val resolver = analyzerFacade.resolverFor(context)
 * ```
 *
 * @property cjModule 底层的 CjModule 实例
 * @see AnalysisContext
 * @see CjModule
 */
class CjModuleAnalysisContext(
    private val cjModule: CjModule
) : AnalysisContext {

    /**
     * 上下文标识符
     *
     * 使用模块名称作为唯一标识。
     */
    override val contextId: String = cjModule.name

    /**
     * 所属 IntelliJ 项目
     */
    override val project: Project = cjModule.project.intellijProject

    /**
     * 文件搜索范围
     *
     * 基于 CjModule 的所有源码集（sourceSets）构建作用域。
     * 包含：
     * - 主源码集的源文件
     * - 测试源码集的源文件
     * - 其他自定义源码集
     *
     * **延迟计算**：作用域在首次访问时计算并缓存。
     */
    override val scope: GlobalSearchScope by lazy {
        val allSourceRoots = cjModule.sourceSets.flatMap { sourceSet ->
            sourceSet.sourceRoots
        }

        if (allSourceRoots.isEmpty()) {
            // 如果没有源码根，返回空作用域
            GlobalSearchScope.EMPTY_SCOPE
        } else {
            // 创建包含所有源码根的联合作用域
            GlobalSearchScope.filesScope(
                project,
                allSourceRoots.toList()
            )
        }
    }

    /**
     * 依赖的分析上下文列表
     *
     * 将 CjModule 的依赖转换为 AnalysisContext。
     * 处理：
     * - CjDependency.Library：外部库依赖
     * - CjDependency.Path：本地路径依赖（通常是其他模块）
     * - CjDependency.Git：Git 仓库依赖
     * - CjDependency.Stdlib：标准库依赖
     * - CjDependency.Binary：二进制依赖
     *
     * **延迟计算**：依赖在首次访问时解析并缓存。
     */
    override val dependencies: List<AnalysisContext> by lazy {
        val result = mutableListOf<AnalysisContext>()

        // 获取编译期依赖
        val compileDeps = cjModule.dependencies

        for (dep in compileDeps) {
            try {
                val depContext = resolveDependencyToContext(dep)
                if (depContext != null) {
                    result.add(depContext)
                }
            } catch (e: Exception) {
                // 记录但不中断：某个依赖解析失败不应影响其他依赖
                // TODO: 添加日志记录
            }
        }

        result
    }

    /**
     * 解析单个依赖为 AnalysisContext
     *
     * @param dependency 要解析的依赖
     * @return 解析后的上下文，如果解析失败返回 null
     */
    private fun resolveDependencyToContext(dependency: CjDependency): AnalysisContext? {
        return when (dependency) {
            is CjDependency.Path -> {
                // Path 依赖通常指向另一个本地模块
                resolvePathDependency(dependency)
            }

            is CjDependency.Library -> {
                // 外部库依赖
                CjLibraryAnalysisContext(dependency, project)
            }

            is CjDependency.Git -> {
                // Git 仓库依赖
                CjLibraryAnalysisContext(dependency, project)
            }

            is CjDependency.Stdlib -> {
                // 标准库依赖
                CjLibraryAnalysisContext(dependency, project)
            }

            is CjDependency.Binary -> {
                // 二进制依赖
                CjLibraryAnalysisContext(dependency, project)
            }
        }
    }

    /**
     * 解析路径依赖为上下文
     *
     * Path 依赖通常指向工作空间中的另一个模块。
     *
     * @param pathDep 路径依赖
     * @return 解析后的上下文，如果找不到模块返回 null
     */
    private fun resolvePathDependency(pathDep: CjDependency.Path): AnalysisContext? {
        // 尝试在当前项目中查找对应的模块
        val targetModule = cjModule.project.findModule(pathDep.name)

        return if (targetModule != null) {
            // 找到了模块，创建模块上下文
            CjModuleAnalysisContext(targetModule)
        } else {
            // 没找到模块，作为库处理
            CjLibraryAnalysisContext(pathDep, project)
        }
    }

    /**
     * 是否为源码上下文
     *
     * CjModule 始终代表项目的源码模块。
     */
    override val isSourceContext: Boolean = true

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is CjModuleAnalysisContext) return false
        return cjModule == other.cjModule
    }

    override fun hashCode(): Int {
        return cjModule.hashCode()
    }

    override fun toString(): String {
        return "CjModuleAnalysisContext(contextId='$contextId', project=${project.name})"
    }
}

/**
 * CjModule 扩展函数：转换为 AnalysisContext
 *
 * 便捷方法，将 CjModule 转换为 AnalysisContext，用于分析器调用。
 *
 * ## 使用示例
 *
 * ```kotlin
 * val cjModule: CjModule = project.cjProject.findModule("mymodule")
 * val context = cjModule.asAnalysisContext()
 *
 * // 传递给分析器
 * val resolver = AnalyzerFacade.resolverFor(context)
 * ```
 *
 * @return 适配后的 AnalysisContext
 */
fun CjModule.asAnalysisContext(): AnalysisContext {
    return CjModuleAnalysisContext(this)
}
