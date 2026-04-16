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

package org.cangnova.cangjie.project.service.impl

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.diagnostic.logger
import com.intellij.openapi.project.Project
import org.cangnova.cangjie.extension.CjDependencyResolver
import org.cangnova.cangjie.project.model.*
import org.cangnova.cangjie.project.service.CjDependencyService
import java.util.*
import java.util.concurrent.ConcurrentHashMap

/**
 * 依赖缓存键
 *
 * 用于缓存已解析的依赖，避免重复解析
 */
private data class DependencyCacheKey(
    val dependencyId: String,  // dependency.id
    val enabledFeatures: Set<String>
) {
    companion object {
        fun from(dependency: CjDependency, enabledFeatures: Set<String>): DependencyCacheKey {
            return DependencyCacheKey(dependency.id, enabledFeatures)
        }
    }
}

/**
 * 依赖解析器实现
 *
 * 实现完整的依赖图解析算法，使用广度优先搜索(BFS)递归解析依赖。
 */

class CjDependencyServiceImpl(val project: Project) : CjDependencyService {
    companion object {
        private val LOG: Logger = logger<CjDependencyServiceImpl>()
    }

    /**
     * 依赖解析缓存
     *
     * 键：DependencyCacheKey (dependency.id + enabledFeatures)
     * 值：Result<CjPackage>
     *
     * 使用 ConcurrentHashMap 保证线程安全
     */
    private val resolvedDependencyCache = ConcurrentHashMap<DependencyCacheKey, Result<CjPackage>>()


    override suspend fun resolveGraph(root: CjPackage): Result<ResolvedGraph> {
        LOG.info("Starting dependency resolution for root package: ${root.id}")

        val packages = mutableMapOf<PackageId, CjPackage>()
        val dependencies = mutableMapOf<PackageId, MutableList<ResolvedDependency>>()
        val enabledFeatures = mutableMapOf<PackageId, MutableSet<String>>()
        val queue = ArrayDeque<Pair<PackageId, Set<String>>>()

        // 获取根包的 replace 规则（只有根包的 replace 规则会生效）
        val replaceRules = extractReplaceRules(root)
        if (replaceRules.isNotEmpty()) {
            LOG.info("Loaded ${replaceRules.size} dependency replacement rules from root package")
        }

        // 1. 添加根包
        packages[root.id] = root
        enabledFeatures[root.id] = root.features.keys.toMutableSet()
        queue.add(root.id to root.features.keys)

        // 2. BFS 解析依赖
        val visited = mutableSetOf<String>()

        while (queue.isNotEmpty()) {
            val (pkgId, currentFeatures) = queue.removeFirst()
            val pkg = packages[pkgId] ?: continue

            LOG.debug("Processing package: $pkgId with features: $currentFeatures")

            val resolvedDeps = mutableListOf<ResolvedDependency>()

            // 3. 遍历依赖声明
            for (depDecl in pkg.dependencies) {
                // 应用 replace 规则（仅对间接依赖）
                val actualDep = if (pkgId != root.id && depDecl.name in replaceRules) {
                    val replaced = replaceRules[depDecl.name]!!
                    LOG.info("Replacing dependency ${depDecl.name} with ${replaced.name} (source: ${replaced.toDependencySource()})")
                    replaced
                } else {
                    depDecl
                }

                // 4. 检查条件编译
                if (!matchesTarget(actualDep.target)) {
                    LOG.debug("Dependency ${depDecl.name} skipped due to targetPlatform mismatch")
                    continue
                }

                // 5. 检查是否可选且未启用
                if (actualDep.optional && actualDep.name !in currentFeatures) {
                    LOG.debug("Optional dependency ${actualDep.name} not enabled, skipping")
                    continue
                }

                // 6. 解析依赖包（使用替换后的依赖）
                val resolvedPkg = resolveSingle(actualDep, currentFeatures).getOrElse { error ->
                    LOG.warn("Failed to resolve dependency ${actualDep.name}: ${error.message}")
                    // 继续处理其他依赖
                    continue
                }

                // 7. 检查版本是否冲突
                val key = "${resolvedPkg.id.name}@${resolvedPkg.id.sourceId.shortName}"
                if (key in visited) {
                    // 已经解析过此依赖，检查版本是否兼容
                    val existing = packages.values.find {
                        "${it.id.name}@${it.id.sourceId.shortName}" == key
                    }
                    if (existing != null && existing.id != resolvedPkg.id) {
                        LOG.warn("Version conflict detected for ${actualDep.name}: ${existing.id.version} vs ${resolvedPkg.id.version}")
                        // TODO: 实现更智能的版本冲突解决
                    }
                    // 使用已解析的包
                    resolvedDeps.add(
                        ResolvedDependency(
                            declaration = actualDep,  // 使用替换后的依赖
                            resolvedTo = existing!!.id,
                            enabledFeatures = computeTransitiveFeatures(actualDep, currentFeatures),
                            scope = actualDep.scope
                        )
                    )
                    continue
                }

                visited.add(key)

                // 8. 添加到图中
                if (resolvedPkg.id !in packages) {
                    packages[resolvedPkg.id] = resolvedPkg

                    // 计算传递的特性
                    val transitiveFeatures = computeTransitiveFeatures(actualDep, currentFeatures)
                    enabledFeatures[resolvedPkg.id] = transitiveFeatures.toMutableSet()

                    // 将依赖包加入队列
                    if (actualDep.transitive) {
                        queue.add(resolvedPkg.id to transitiveFeatures)
                    }
                }

                resolvedDeps.add(
                    ResolvedDependency(
                        declaration = actualDep,  // 使用替换后的依赖
                        resolvedTo = resolvedPkg.id,
                        enabledFeatures = computeTransitiveFeatures(actualDep, currentFeatures),
                        scope = actualDep.scope
                    )
                )
            }

            dependencies[pkgId] = resolvedDeps
        }

        // 9. 构建最终依赖图
        val graph = ResolvedGraph(
            packages = packages,
            dependencies = dependencies.mapValues { it.value.toList() },
            root = root.id,
            enabledFeatures = enabledFeatures.mapValues { it.value.toSet() }
        )

        // 10. 验证依赖图
        val (valid, error) = graph.validate()
        if (!valid) {
            LOG.error("Invalid dependency graph: $error")
            return Result.failure(IllegalStateException("Invalid dependency graph: $error"))
        }

        LOG.info("Dependency resolution completed successfully: ${packages.size} packages, ${dependencies.size} edges")
        return Result.success(graph)
    }

    override suspend fun resolveSingle(
        dependency: CjDependency,
        enabledFeatures: Set<String>
    ): Result<CjPackage> {
        // 1. 检查缓存
        val cacheKey = DependencyCacheKey.from(dependency, enabledFeatures)
        resolvedDependencyCache[cacheKey]?.let { cachedResult ->
            LOG.debug("Cache hit for dependency: ${dependency.name} with features: $enabledFeatures")
            return cachedResult
        }

        // 2. 缓存未命中，执行实际解析
        LOG.debug("Cache miss for dependency: ${dependency.name}, resolving...")

        val resolve = CjDependencyResolver.EP_NAME.extensionList.firstOrNull {
            it.canResolve(dependency)
        }

        if (resolve == null) {
            val failure = Result.failure<CjPackage>(
                IllegalArgumentException("No resolver found for dependency: ${dependency.name}")
            )
            // 缓存失败结果，避免重复尝试
            resolvedDependencyCache[cacheKey] = failure
            return failure
        }

        val result = resolve.resolve(dependency, project)?.let {
            Result.success(it)
        } ?: Result.failure(
            IllegalArgumentException("Resolver returned null for dependency: ${dependency.name}")
        )

        // 3. 缓存结果
        resolvedDependencyCache[cacheKey] = result
        LOG.debug("Cached resolution result for dependency: ${dependency.name}")

        return result
    }

    override fun clearCache() {
        LOG.info("Clearing dependency resolution cache (${resolvedDependencyCache.size} entries)")
        resolvedDependencyCache.clear()
    }


    /**
     * 计算传递到依赖的特性集合
     */
    private fun computeTransitiveFeatures(
        dep: CjDependency,
        parentFeatures: Set<String>
    ): Set<String> {
        val features = mutableSetOf<String>()

        // 1. 添加依赖声明中指定的特性
        features.addAll(dep.features)

        // 2. 添加默认特性
        if (dep.defaultFeatures) {
            features.add("default")
        }

        // 3. TODO: 处理特性传播（DependencyFeature）
        // 例如：如果父包启用了 "full" 特性，且 "full" 包含 "dep/feature1"
        // 则应该将 "feature1" 传递给 dep

        return features
    }

    /**
     * 检查目标平台是否匹配
     */
    private fun matchesTarget(target: String?): Boolean {
        if (target == null) return true

        // TODO: 实现完整的条件编译检查
        // 支持：cfg(unix), cfg(windows), cfg(target_os = "linux") 等

        // 简化实现：假设所有目标都匹配
        LOG.debug("Target check not fully implemented, assuming match for: $target")
        return true
    }

    /**
     * 从根包提取 replace 规则
     *
     * 只有根模块的 replace 字段会生效。
     * 此方法委托给各个 CjDependencyResolver 扩展点来实现。
     *
     * @param root 根包
     * @return 依赖名称到替换依赖的映射
     */
    private fun extractReplaceRules(root: CjPackage): Map<String, CjDependency> {
        val allRules = mutableMapOf<String, CjDependency>()

        // 向所有注册的 DependencyResolver 请求 replace 规则
        for (resolver in CjDependencyResolver.EP_NAME.extensionList) {
            val rules = resolver.extractReplaceRules(root, project)
            if (rules.isNotEmpty()) {
                LOG.debug("Got ${rules.size} replace rules from ${resolver.resolverName}")
                allRules.putAll(rules)
            }
        }

        return allRules
    }

}