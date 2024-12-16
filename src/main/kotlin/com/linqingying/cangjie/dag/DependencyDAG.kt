package com.linqingying.cangjie.dag

import com.linqingying.cangjie.name.FqName
class DependencyDAG {
    private val adjacencyList: MutableMap<FqName, MutableSet<FqName>> = mutableMapOf()
    private val inDegree: MutableMap<FqName, Int> = mutableMapOf()

    // 添加依赖边
    fun addDependency(from: FqName, to: FqName): Boolean {
        // 初始化节点
        adjacencyList.computeIfAbsent(from) { mutableSetOf() }
        adjacencyList.computeIfAbsent(to) { mutableSetOf() }
        inDegree.putIfAbsent(to, 0)
        inDegree.putIfAbsent(from, 0)

        // 添加边并更新入度
        if (adjacencyList[from]?.add(to) == false) return false // Edge already exists

        inDegree[to] = inDegree[to]?.plus(1) ?: 1

        // 检测环
        return !hasCycle()
    }

    // 移除节点及其所有边
    fun removeDependencies(from: FqName) {
        adjacencyList[from]?.forEach { to ->
            inDegree[to] = (inDegree[to]?.minus(1) ?: 0).takeIf { it > 0 } ?: 0
            if (inDegree[to] == 0) {
                inDegree.remove(to)
            }
        }
        adjacencyList.remove(from)
        inDegree.remove(from)
    }

    // 获取某节点的依赖
    fun getDependencies(from: FqName): Set<FqName> = adjacencyList[from] ?: emptySet()

    // 检查是否存在环（通过拓扑排序）
    private fun hasCycle(): Boolean {
        val inDegreeCopy = inDegree.toMutableMap()
        val queue = ArrayDeque<FqName>()

        // 入度为 0 的节点入队
        inDegreeCopy.filter { it.value == 0 }.keys.forEach { queue.add(it) }

        var visitedCount = 0

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            visitedCount++

            // 遍历当前节点的所有邻接节点
            adjacencyList[current]?.forEach { neighbor ->
                // 减少邻接节点的入度
                inDegreeCopy[neighbor] = (inDegreeCopy[neighbor] ?: 0) - 1

                // 如果入度减为 0，则加入队列
                if (inDegreeCopy[neighbor] == 0) {
                    queue.add(neighbor)
                }
            }
        }


        // 如果有节点未访问，说明存在环
        return visitedCount != inDegree.size
    }

    // 检查特定节点是否在环中
    fun isInCycle(node: FqName): Boolean {
        val visited = mutableSetOf<FqName>()
        val stack = mutableSetOf<FqName>()

        fun dfs(current: FqName): Boolean {
            if (current in stack) return true // Found a cycle
            if (current in visited) return false
            visited.add(current)
            stack.add(current)
            val hasCycle = adjacencyList[current]?.any { dfs(it) } == true
            stack.remove(current)
            return hasCycle
        }

        return dfs(node)
    }

    // 获取拓扑排序结果，如果存在环则返回 null
    fun getTopologicalOrder(): List<FqName>? {
        val inDegreeCopy = inDegree.toMutableMap()
        val queue = ArrayDeque<FqName>()
        val result = mutableListOf<FqName>()

        // 入度为 0 的节点入队
        inDegreeCopy.filter { it.value == 0 }.keys.forEach { queue.add(it) }

        while (queue.isNotEmpty()) {
            val current = queue.removeFirst()
            result.add(current)
            adjacencyList[current]?.forEach { neighbor ->
                inDegreeCopy[neighbor] = inDegreeCopy[neighbor]?.minus(1)?.takeIf { it > 0 } ?: 0
                if (inDegreeCopy[neighbor] == 0) {
                    queue.add(neighbor)
                }
            }
        }

        // 如果拓扑排序包含了所有节点，则无环
        return if (result.size == inDegree.size) result else null
    }

    // 检查某节点是否为孤立节点（无依赖也无被依赖）
    fun isIsolated(node: FqName): Boolean {
        return (adjacencyList[node]?.isEmpty() == true) && (inDegree[node] == 0)
    }
}
