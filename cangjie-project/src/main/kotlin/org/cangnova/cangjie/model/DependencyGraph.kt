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

package org.cangnova.cangjie.model

/**
 * 依赖图节点
 *
 * 表示依赖图中的一个节点，包含依赖及其解析信息
 */
data class DependencyNode(
    /**
     * 依赖对象
     */
    val dependency: CjDependency,

    /**
     * 解析后的依赖信息
     */
    val resolved: CjResolvedDependency?,

    /**
     * 依赖深度（距离根节点的距离）
     */
    val depth: Int = 0,

    /**
     * 父节点（依赖路径）
     */
    val parent: DependencyNode? = null
) {
    /**
     * 子节点（传递依赖）
     */
    val children: MutableList<DependencyNode> = mutableListOf()

    /**
     * 依赖路径（从根到当前节点）
     */
    val path: List<CjDependency>
        get() {
            val result = mutableListOf<CjDependency>()
            var current: DependencyNode? = this
            while (current != null) {
                result.add(0, current.dependency)
                current = current.parent
            }
            return result
        }

    /**
     * 是否是叶子节点
     */
    val isLeaf: Boolean
        get() = children.isEmpty()

    /**
     * 依赖的唯一标识
     */
    val key: String
        get() = "${dependency.group ?: ""}:${dependency.name}"

    override fun toString(): String {
        return "DependencyNode(${dependency.name}:${dependency.version}, depth=$depth, children=${children.size})"
    }
}

/**
 * 版本冲突信息
 */
data class VersionConflict(
    /**
     * 依赖标识（group:name）
     */
    val key: String,

    /**
     * 冲突的依赖列表
     */
    val conflictingDependencies: List<CjDependency>,

    /**
     * 选中的依赖
     */
    val selected: CjDependency,

    /**
     * 冲突解决原因
     */
    val reason: ConflictReason
) {
    override fun toString(): String {
        val versions = conflictingDependencies.joinToString(", ") { it.version.toString() }
        return "Conflict for $key: [$versions] -> selected ${selected.version} ($reason)"
    }
}

/**
 * 冲突解决原因
 */
enum class ConflictReason {
    /**
     * 最近优先（最短依赖路径）
     */
    NEAREST_WINS,

    /**
     * 最新版本优先
     */
    NEWEST_VERSION,

    /**
     * 显式覆盖
     */
    EXPLICIT_OVERRIDE,

    /**
     * 第一个遇到的版本
     */
    FIRST_WINS
}

/**
 * 循环依赖信息
 */
data class CyclicDependency(
    /**
     * 循环依赖路径
     */
    val cycle: List<CjDependency>
) {
    /**
     * 循环中的依赖名称列表
     */
    val dependencyNames: List<String>
        get() = cycle.map { it.name }

    override fun toString(): String {
        return "Cyclic: ${dependencyNames.joinToString(" -> ")}"
    }
}

/**
 * 依赖图接口
 *
 * 管理项目的依赖关系图，提供循环检测、冲突解决等功能
 */
interface DependencyGraph {
    /**
     * 根节点（项目本身的直接依赖）
     */
    val rootNodes: List<DependencyNode>

    /**
     * 所有依赖节点（扁平化）
     */
    val allNodes: Set<DependencyNode>

    /**
     * 所有唯一依赖（去重后）
     */
    val uniqueDependencies: Map<String, List<DependencyNode>>

    /**
     * 检测循环依赖
     *
     * @return 发现的循环依赖列表，如果没有循环则返回空列表
     */
    fun detectCycles(): List<CyclicDependency>

    /**
     * 解决版本冲突
     *
     * @return 冲突及其解决方案的映射
     */
    fun resolveConflicts(): Map<String, VersionConflict>

    /**
     * 获取某个依赖的传递依赖
     *
     * @param dependency 依赖对象
     * @return 传递依赖的节点列表
     */
    fun getTransitiveDependencies(dependency: CjDependency): Set<DependencyNode>

    /**
     * 获取依赖树（以树形结构表示）
     *
     * @return 依赖树的字符串表示
     */
    fun toDependencyTree(): String

    /**
     * 获取扁平依赖列表（解决冲突后的最终依赖）
     *
     * @return 解决冲突后的依赖列表
     */
    fun getFlattenedDependencies(): List<CjDependency>
}

/**
 * 依赖图构建器
 */
interface DependencyGraphBuilder {
    /**
     * 添加根依赖
     */
    fun addRootDependency(dependency: CjDependency, resolved: CjResolvedDependency?)

    /**
     * 构建依赖图
     */
    fun build(): DependencyGraph
}