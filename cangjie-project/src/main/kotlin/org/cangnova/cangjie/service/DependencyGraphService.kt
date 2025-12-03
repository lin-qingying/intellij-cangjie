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

package org.cangnova.cangjie.service

import com.intellij.openapi.components.Service
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.model.CjDependency
import org.cangnova.cangjie.model.CyclicDependency
import org.cangnova.cangjie.model.DependencyGraph
import org.cangnova.cangjie.model.VersionConflict
import org.cangnova.cangjie.model.impl.DependencyGraphBuilderImpl
import org.cangnova.cangjie.project.model.CjModule

/**
 * 依赖图服务
 *
 * 提供依赖图的构建、分析和缓存功能
 */
@Service(Service.Level.PROJECT)
class DependencyGraphService(private val project: Project) {

    companion object {
        private val LOG = logger<DependencyGraphService>()

        /**
         * 获取服务实例
         */
        fun getInstance(project: Project): DependencyGraphService {
            return project.getService(DependencyGraphService::class.java)
        }
    }

    // 依赖图缓存（按模块缓存）
    private val graphCache = mutableMapOf<String, DependencyGraph>()

    /**
     * 为模块构建依赖图
     *
     * @param module 模块对象
     * @param forceRebuild 是否强制重新构建（默认使用缓存）
     * @return 依赖图
     */
    fun buildDependencyGraph(module: CjModule, forceRebuild: Boolean = false): DependencyGraph {
        val cacheKey = module.name

        // 检查缓存
        if (!forceRebuild && graphCache.containsKey(cacheKey)) {
            LOG.info("Using cached dependency graph for module: ${module.name}")
            return graphCache[cacheKey]!!
        }

        LOG.info("Building dependency graph for module: ${module.name}")

        val builder = DependencyGraphBuilderImpl()
        val dependencyService = CjDependencyService.getInstance()

        // 解析所有直接依赖
        for (dependency in module.allDependencies) {
            val resolved = dependencyService.resolveDependency(dependency, project)
            builder.addRootDependency(dependency, resolved)
        }

        val graph = builder.build()

        // 缓存结果
        graphCache[cacheKey] = graph

        LOG.info("Dependency graph built for module: ${module.name}, nodes: ${graph.allNodes.size}")

        return graph
    }

    /**
     * 清除缓存
     */
    fun clearCache() {
        graphCache.clear()
        LOG.info("Dependency graph cache cleared")
    }

    /**
     * 清除特定模块的缓存
     */
    fun clearCache(moduleName: String) {
        graphCache.remove(moduleName)
        LOG.info("Dependency graph cache cleared for module: $moduleName")
    }

    /**
     * 分析依赖图，返回分析报告
     *
     * @param module 模块对象
     * @return 分析报告
     */
    fun analyzeDependencies(module: CjModule): DependencyAnalysisReport {
        val graph = buildDependencyGraph(module)

        val cycles = graph.detectCycles()
        val conflicts = graph.resolveConflicts()
        val flattenedDeps = graph.getFlattenedDependencies()

        return DependencyAnalysisReport(
            module = module,
            graph = graph,
            cycles = cycles,
            conflicts = conflicts,
            flattenedDependencies = flattenedDeps
        )
    }

    /**
     * 获取依赖树的文本表示
     */
    fun getDependencyTreeString(module: CjModule): String {
        val graph = buildDependencyGraph(module)
        return graph.toDependencyTree()
    }

    /**
     * 检查是否有循环依赖
     */
    fun hasCyclicDependencies(module: CjModule): Boolean {
        val graph = buildDependencyGraph(module)
        return graph.detectCycles().isNotEmpty()
    }

    /**
     * 检查是否有版本冲突
     */
    fun hasVersionConflicts(module: CjModule): Boolean {
        val graph = buildDependencyGraph(module)
        return graph.resolveConflicts().isNotEmpty()
    }
}

/**
 * 依赖分析报告
 */
data class DependencyAnalysisReport(
    /**
     * 被分析的模块
     */
    val module: CjModule,

    /**
     * 依赖图
     */
    val graph: DependencyGraph,

    /**
     * 循环依赖列表
     */
    val cycles: List<CyclicDependency>,

    /**
     * 版本冲突映射
     */
    val conflicts: Map<String, VersionConflict>,

    /**
     * 扁平化的依赖列表（解决冲突后）
     */
    val flattenedDependencies: List<CjDependency>
) {
    /**
     * 是否有问题（循环依赖或版本冲突）
     */
    val hasIssues: Boolean
        get() = cycles.isNotEmpty() || conflicts.isNotEmpty()

    /**
     * 依赖总数（包括传递依赖）
     */
    val totalDependencies: Int
        get() = graph.allNodes.size

    /**
     * 唯一依赖数（去重后）
     */
    val uniqueDependenciesCount: Int
        get() = flattenedDependencies.size

    /**
     * 生成报告摘要
     */
    fun summary(): String {
        return buildString {
            appendLine("=== Dependency Analysis Report for ${module.name} ===")
            appendLine()
            appendLine("Total dependencies (with duplicates): $totalDependencies")
            appendLine("Unique dependencies: $uniqueDependenciesCount")
            appendLine()

            if (cycles.isNotEmpty()) {
                appendLine("⚠️  Cyclic Dependencies Detected: ${cycles.size}")
                cycles.forEach { cycle ->
                    appendLine("  - $cycle")
                }
                appendLine()
            } else {
                appendLine("✓ No cyclic dependencies")
                appendLine()
            }

            if (conflicts.isNotEmpty()) {
                appendLine("⚠️  Version Conflicts Detected: ${conflicts.size}")
                conflicts.values.forEach { conflict ->
                    appendLine("  - $conflict")
                }
                appendLine()
            } else {
                appendLine("✓ No version conflicts")
                appendLine()
            }

            if (!hasIssues) {
                appendLine("✓ All dependencies are healthy!")
            }
        }
    }

    /**
     * 生成详细报告（包括依赖树）
     */
    fun detailedReport(): String {
        return buildString {
            append(summary())
            appendLine()
            appendLine("=== Dependency Tree ===")
            appendLine(graph.toDependencyTree())
        }
    }
}
