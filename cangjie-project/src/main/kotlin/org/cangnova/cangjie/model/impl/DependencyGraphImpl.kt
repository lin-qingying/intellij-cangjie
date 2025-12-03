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

package org.cangnova.cangjie.model.impl

import com.intellij.openapi.diagnostic.logger
import org.cangnova.cangjie.model.*

/**
 * 依赖图实现
 */
class DependencyGraphImpl(
    override val rootNodes: List<DependencyNode>
) : DependencyGraph {

    companion object {
        private val LOG = logger<DependencyGraphImpl>()
    }

    override val allNodes: Set<DependencyNode> by lazy {
        buildAllNodes()
    }

    override val uniqueDependencies: Map<String, List<DependencyNode>> by lazy {
        buildUniqueDependencies()
    }

    /**
     * 构建所有节点的扁平集合
     */
    private fun buildAllNodes(): Set<DependencyNode> {
        val result = mutableSetOf<DependencyNode>()
        val queue = ArrayDeque(rootNodes)

        while (queue.isNotEmpty()) {
            val node = queue.removeFirst()
            if (result.add(node)) {
                queue.addAll(node.children)
            }
        }

        return result
    }

    /**
     * 按依赖标识分组（用于冲突检测）
     */
    private fun buildUniqueDependencies(): Map<String, List<DependencyNode>> {
        return allNodes.groupBy { it.key }
    }

    override fun detectCycles(): List<CyclicDependency> {
        val cycles = mutableListOf<CyclicDependency>()
        val visited = mutableSetOf<String>()
        val recursionStack = mutableSetOf<String>()
        val currentPath = mutableListOf<DependencyNode>()

        fun dfs(node: DependencyNode) {
            val key = node.key

            visited.add(key)
            recursionStack.add(key)
            currentPath.add(node)

            for (child in node.children) {
                val childKey = child.key

                if (!visited.contains(childKey)) {
                    dfs(child)
                } else if (recursionStack.contains(childKey)) {
                    // 发现循环
                    val cycleStartIndex = currentPath.indexOfFirst { it.key == childKey }
                    if (cycleStartIndex >= 0) {
                        val cyclePath = currentPath.subList(cycleStartIndex, currentPath.size)
                            .map { it.dependency }
                            .toMutableList()
                        cyclePath.add(child.dependency) // 添加回到起点的依赖

                        cycles.add(CyclicDependency(cyclePath))
                    }
                }
            }

            recursionStack.remove(key)
            currentPath.removeAt(currentPath.size - 1)
        }

        // 从每个根节点开始 DFS
        for (root in rootNodes) {
            if (!visited.contains(root.key)) {
                dfs(root)
            }
        }

        return cycles
    }

    override fun resolveConflicts(): Map<String, VersionConflict> {
        val conflicts = mutableMapOf<String, VersionConflict>()

        // 遍历所有有多个版本的依赖
        uniqueDependencies.forEach { (key, nodes) ->
            if (nodes.size > 1) {
                // 有冲突，需要解决
                val selected = selectVersion(nodes)
                val reason = determineConflictReason(nodes, selected)

                conflicts[key] = VersionConflict(
                    key = key,
                    conflictingDependencies = nodes.map { it.dependency },
                    selected = selected.dependency,
                    reason = reason
                )

                LOG.info("Resolved conflict for $key: selected ${selected.dependency.version} ($reason)")
            }
        }

        return conflicts
    }

    /**
     * 选择版本（冲突解决策略）
     */
    private fun selectVersion(nodes: List<DependencyNode>): DependencyNode {
        // 策略1: 最近优先 (Nearest Wins) - 选择深度最小的
        val nearestNode = nodes.minByOrNull { it.depth }
        if (nearestNode != null && nodes.count { it.depth == nearestNode.depth } == 1) {
            return nearestNode
        }

        // 策略2: 最新版本优先 (Newest Version)
        return nodes.maxByOrNull { it.dependency.version } ?: nodes.first()
    }

    /**
     * 确定冲突解决原因
     */
    private fun determineConflictReason(nodes: List<DependencyNode>, selected: DependencyNode): ConflictReason {
        val minDepth = nodes.minOfOrNull { it.depth } ?: 0

        return when {
            // 如果选中的是最近的（深度最小）
            selected.depth == minDepth && nodes.count { it.depth == minDepth } == 1 ->
                ConflictReason.NEAREST_WINS

            // 如果选中的是最新版本
            selected.dependency.version == nodes.maxOfOrNull { it.dependency.version } ->
                ConflictReason.NEWEST_VERSION

            else -> ConflictReason.FIRST_WINS
        }
    }

    override fun getTransitiveDependencies(dependency: CjDependency): Set<DependencyNode> {
        val result = mutableSetOf<DependencyNode>()
        val key = "${dependency.group ?: ""}:${dependency.name}"

        // 找到对应的节点
        val nodes = uniqueDependencies[key] ?: return emptySet()

        // 收集所有传递依赖
        for (node in nodes) {
            collectTransitive(node, result)
        }

        return result
    }

    private fun collectTransitive(node: DependencyNode, result: MutableSet<DependencyNode>) {
        for (child in node.children) {
            if (result.add(child)) {
                collectTransitive(child, result)
            }
        }
    }

    override fun toDependencyTree(): String {
        val builder = StringBuilder()

        for ((index, root) in rootNodes.withIndex()) {
            val isLast = index == rootNodes.size - 1
            appendNode(builder, root, "", isLast)
        }

        return builder.toString()
    }

    private fun appendNode(builder: StringBuilder, node: DependencyNode, prefix: String, isLast: Boolean) {
        // 打印当前节点
        builder.append(prefix)
        builder.append(if (isLast) "└── " else "├── ")
        builder.append(node.dependency.name)
        builder.append(":")
        builder.append(node.dependency.version)

        // 如果有冲突，标记
        val key = node.key
        val nodesWithSameKey = uniqueDependencies[key] ?: emptyList()
        if (nodesWithSameKey.size > 1) {
            val selected = selectVersion(nodesWithSameKey)
            if (selected.dependency.version != node.dependency.version) {
                builder.append(" (conflict: will use ${selected.dependency.version})")
            }
        }

        builder.append("\n")

        // 打印子节点
        val newPrefix = prefix + if (isLast) "    " else "│   "
        for ((childIndex, child) in node.children.withIndex()) {
            val childIsLast = childIndex == node.children.size - 1
            appendNode(builder, child, newPrefix, childIsLast)
        }
    }

    override fun getFlattenedDependencies(): List<CjDependency> {
        val conflicts = resolveConflicts()
        val result = mutableMapOf<String, CjDependency>()

        // 遍历所有节点，应用冲突解决策略
        for ((key, nodes) in uniqueDependencies) {
            val selected = if (conflicts.containsKey(key)) {
                conflicts[key]!!.selected
            } else {
                nodes.first().dependency
            }
            result[key] = selected
        }

        return result.values.toList()
    }
}

/**
 * 依赖图构建器实现
 */
class DependencyGraphBuilderImpl : DependencyGraphBuilder {
    private val roots = mutableListOf<Pair<CjDependency, CjResolvedDependency?>>()

    companion object {
        private val LOG = logger<DependencyGraphBuilderImpl>()
    }

    override fun addRootDependency(dependency: CjDependency, resolved: CjResolvedDependency?) {
        roots.add(dependency to resolved)
    }

    override fun build(): DependencyGraph {
        val rootNodes = roots.map { (dep, resolved) ->
            buildNode(dep, resolved, depth = 0, parent = null, visited = mutableSetOf())
        }

        return DependencyGraphImpl(rootNodes)
    }

    /**
     * 递归构建依赖节点
     */
    private fun buildNode(
        dependency: CjDependency,
        resolved: CjResolvedDependency?,
        depth: Int,
        parent: DependencyNode?,
        visited: MutableSet<String>
    ): DependencyNode {
        val node = DependencyNode(
            dependency = dependency,
            resolved = resolved,
            depth = depth,
            parent = parent
        )

        val key = node.key

        // 防止无限递归（处理循环依赖）
        if (visited.contains(key)) {
            LOG.warn("Circular dependency detected: $key")
            return node
        }

        visited.add(key)

        // 构建子节点（传递依赖）
        if (dependency.transitive && resolved != null) {
            for (transitiveDep in resolved.transitiveDependencies) {
                val childNode = buildNode(
                    dependency = transitiveDep,
                    resolved = null, // 传递依赖的解析信息可以按需加载
                    depth = depth + 1,
                    parent = node,
                    visited = visited.toMutableSet() // 创建新的 visited 集合，避免不同分支互相影响
                )
                node.children.add(childNode)
            }
        }

        visited.remove(key)

        return node
    }
}