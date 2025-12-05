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

package org.cangnova.cangjie.project.model


/**
 * 解析后的单个依赖
 *
 * 表示依赖声明到实际包的解析结果
 *
 * @property declaration 原始依赖声明
 * @property resolvedTo 解析到的包标识
 * @property enabledFeatures 该依赖启用的特性集合
 * @property scope 依赖范围
 */
data class ResolvedDependency(
    /**
     * 原始依赖声明
     */
    val declaration: CjDependency,

    /**
     * 解析到的包 ID
     */
    val resolvedTo: PackageId,

    /**
     * 启用的特性集合
     */
    val enabledFeatures: Set<String>,

    /**
     * 依赖范围
     */
    val scope: CjDependencyScope
) {
    override fun toString(): String {
        val featuresStr = if (enabledFeatures.isNotEmpty()) {
            " [features: ${enabledFeatures.joinToString(", ")}]"
        } else ""
        return "${declaration.name} -> $resolvedTo$featuresStr"
    }
}

/**
 * 解析后的依赖图
 *
 * 包含项目的完整依赖关系，以及依赖解析的所有结果。
 * 提供拓扑排序、循环检测等功能。
 *
 * @property packages 所有包的映射（包括根包和所有依赖）
 * @property dependencies 每个包的依赖关系
 * @property root 根包 ID（通常是项目本身）
 * @property enabledFeatures 每个包启用的特性
 */
data class ResolvedGraph(
    /**
     * 所有包的映射 (PackageId -> CjPackage)
     */
    val packages: Map<PackageId, CjPackage>,

    /**
     * 依赖关系映射 (PackageId -> List<ResolvedDependency>)
     * Key 是包 ID，Value 是该包的所有直接依赖
     */
    val dependencies: Map<PackageId, List<ResolvedDependency>>,

    /**
     * 根包 ID（项目本身）
     */
    val root: PackageId,

    /**
     * 每个包启用的特性 (PackageId -> Set<Feature Names>)
     */
    val enabledFeatures: Map<PackageId, Set<String>>
) {
    /**
     * 获取构建顺序（拓扑排序）
     *
     * 使用深度优先搜索(DFS)进行拓扑排序，确保依赖先于依赖它的包构建。
     *
     * @return 成功时返回包 ID 的构建顺序列表，失败时返回异常
     */
    fun buildOrder(): Result<List<PackageId>> {
        val result = mutableListOf<PackageId>()
        val visited = mutableSetOf<PackageId>()
        val visiting = mutableSetOf<PackageId>()

        fun visit(pkgId: PackageId): Result<Unit> {
            // 已访问过，跳过
            if (pkgId in visited) return Result.success(Unit)

            // 正在访问，发现循环依赖
            if (pkgId in visiting) {
                return Result.failure(
                    IllegalStateException("Circular dependency detected at: $pkgId")
                )
            }

            visiting.add(pkgId)

            // 递归访问所有依赖
            dependencies[pkgId]?.forEach { dep ->
                visit(dep.resolvedTo).getOrElse { return Result.failure(it) }
            }

            visiting.remove(pkgId)
            visited.add(pkgId)
            result.add(pkgId)

            return Result.success(Unit)
        }

        // 从根节点开始遍历
        visit(root).getOrElse { return Result.failure(it) }

        return Result.success(result)
    }

    /**
     * 检测循环依赖
     *
     * 使用深度优先搜索检测依赖图中的所有循环。
     *
     * @return 发现的所有循环依赖路径列表
     */
    fun detectCycles(): List<List<PackageId>> {
        val cycles = mutableListOf<List<PackageId>>()
        val visited = mutableSetOf<PackageId>()
        val path = mutableListOf<PackageId>()

        fun dfs(pkgId: PackageId) {
            // 发现循环
            if (pkgId in path) {
                val cycleStart = path.indexOf(pkgId)
                val cycle = path.subList(cycleStart, path.size) + pkgId
                cycles.add(cycle)
                return
            }

            // 已访问过，不再重复访问
            if (pkgId in visited) return

            visited.add(pkgId)
            path.add(pkgId)

            // 递归访问所有依赖
            dependencies[pkgId]?.forEach { dep ->
                dfs(dep.resolvedTo)
            }

            path.removeLast()
        }

        // 遍历所有包
        packages.keys.forEach { pkgId ->
            if (pkgId !in visited) {
                dfs(pkgId)
            }
        }

        return cycles
    }

    /**
     * 获取某个包的所有传递依赖
     *
     * @param pkgId 包 ID
     * @return 所有传递依赖的包 ID 集合
     */
    fun getTransitiveDependencies(pkgId: PackageId): Set<PackageId> {
        val result = mutableSetOf<PackageId>()
        val visited = mutableSetOf<PackageId>()

        fun visit(id: PackageId) {
            if (id in visited) return
            visited.add(id)

            dependencies[id]?.forEach { dep ->
                result.add(dep.resolvedTo)
                visit(dep.resolvedTo)
            }
        }

        visit(pkgId)
        return result
    }

    /**
     * 获取所有包的扁平列表（去重）
     *
     * @return 所有包的 ID 列表
     */
    fun getAllPackages(): List<PackageId> {
        return packages.keys.toList()
    }

    /**
     * 获取某个包的直接依赖
     *
     * @param pkgId 包 ID
     * @return 直接依赖的解析结果列表
     */
    fun getDirectDependencies(pkgId: PackageId): List<ResolvedDependency> {
        return dependencies[pkgId] ?: emptyList()
    }

    /**
     * 获取依赖树的字符串表示（用于调试）
     *
     * @return 依赖树的字符串
     */
    fun toDependencyTree(): String {
        val builder = StringBuilder()
        val visited = mutableSetOf<PackageId>()

        fun printTree(pkgId: PackageId, indent: String = "", isLast: Boolean = true) {
            if (pkgId in visited) {
                builder.append("$indent${if (isLast) "└── " else "├── "}$pkgId (*)\\n")
                return
            }
            visited.add(pkgId)

            val pkg = packages[pkgId]
            val features = enabledFeatures[pkgId]?.let { " [${it.joinToString(", ")}]" } ?: ""
            builder.append("$indent${if (isLast) "└── " else "├── "}${pkg?.metadata?.name ?: pkgId.name}:${pkgId.version}$features\\n")

            val deps = dependencies[pkgId] ?: emptyList()
            deps.forEachIndexed { index, dep ->
                val newIndent = indent + if (isLast) "    " else "│   "
                printTree(dep.resolvedTo, newIndent, index == deps.size - 1)
            }
        }

        printTree(root)
        return builder.toString()
    }

    /**
     * 检查依赖图是否有效
     *
     * @return 是否有效以及错误信息
     */
    fun validate(): Pair<Boolean, String?> {
        // 1. 检查根包是否存在
        if (root !in packages) {
            return false to "Root package $root not found in packages"
        }

        // 2. 检查所有依赖的包是否存在
        dependencies.forEach { (pkgId, deps) ->
            if (pkgId !in packages) {
                return false to "Package $pkgId in dependencies but not in packages"
            }

            deps.forEach { dep ->
                if (dep.resolvedTo !in packages) {
                    return false to "Dependency ${dep.resolvedTo} not found in packages"
                }
            }
        }

        // 3. 检查是否有循环依赖
        val cycles = detectCycles()
        if (cycles.isNotEmpty()) {
            val cycleStr = cycles.joinToString("; ") { cycle ->
                cycle.joinToString(" -> ") { it.name }
            }
            return false to "Circular dependencies detected: $cycleStr"
        }

        return true to null
    }

    override fun toString(): String {
        return "ResolvedGraph(root=$root, packages=${packages.size}, dependencies=${dependencies.size})"
    }

    companion object {
        /**
         * 创建空的依赖图
         */
        fun empty(root: PackageId): ResolvedGraph {
            return ResolvedGraph(
                packages = emptyMap(),
                dependencies = emptyMap(),
                root = root,
                enabledFeatures = emptyMap()
            )
        }
    }
}

/**
 * 依赖图构建器
 *
 * 用于构建 ResolvedGraph 的工具类
 */
class ResolvedGraphBuilder {
    private val packages = mutableMapOf<PackageId, CjPackage>()
    private val dependencies = mutableMapOf<PackageId, MutableList<ResolvedDependency>>()
    private val enabledFeatures = mutableMapOf<PackageId, MutableSet<String>>()
    private var root: PackageId? = null

    /**
     * 设置根包
     */
    fun setRoot(rootPkg: CjPackage): ResolvedGraphBuilder {
        this.root = rootPkg.id
        addPackage(rootPkg)
        return this
    }

    /**
     * 添加包
     */
    fun addPackage(pkg: CjPackage): ResolvedGraphBuilder {
        packages[pkg.id] = pkg
        return this
    }

    /**
     * 添加依赖关系
     */
    fun addDependency(from: PackageId, to: ResolvedDependency): ResolvedGraphBuilder {
        dependencies.getOrPut(from) { mutableListOf() }.add(to)
        return this
    }

    /**
     * 启用特性
     */
    fun enableFeature(pkgId: PackageId, feature: String): ResolvedGraphBuilder {
        enabledFeatures.getOrPut(pkgId) { mutableSetOf() }.add(feature)
        return this
    }

    /**
     * 启用多个特性
     */
    fun enableFeatures(pkgId: PackageId, features: Set<String>): ResolvedGraphBuilder {
        enabledFeatures.getOrPut(pkgId) { mutableSetOf() }.addAll(features)
        return this
    }

    /**
     * 构建依赖图
     */
    fun build(): ResolvedGraph {
        val rootId = root ?: throw IllegalStateException("Root package not set")
        return ResolvedGraph(
            packages = packages.toMap(),
            dependencies = dependencies.mapValues { it.value.toList() },
            root = rootId,
            enabledFeatures = enabledFeatures.mapValues { it.value.toSet() }
        )
    }
}