/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.utils

import java.util.*

/**
 * 深度优先搜索工具类
 */
object DFS {
    /**
     * 对一组节点执行深度优先搜索
     *
     * @param nodes 需要遍历的节点集合
     * @param neighbors 获取节点邻居的函数
     * @param visited 用于标记已访问节点的对象
     * @param handler 处理节点的处理器
     * @return 处理器的结果
     */
    fun <N, R> dfs(
        nodes: Collection<N>,
        neighbors: Neighbors<N>,
        visited: Visited<N>,
        handler: NodeHandler<N, R>
    ): R {
        for (node in nodes) {
            doDfs(node, neighbors, visited, handler)
        }
        return handler.result()
    }

    /**
     * 对一组节点执行深度优先搜索，使用默认的访问标记集合
     *
     * @param nodes 需要遍历的节点集合
     * @param neighbors 获取节点邻居的函数
     * @param handler 处理节点的处理器
     * @return 处理器的结果
     */
    fun <N, R> dfs(
        nodes: Collection<N>,
        neighbors: Neighbors<N>,
        handler: NodeHandler<N, R>
    ): R {
        return dfs(nodes, neighbors, VisitedWithSet<N>(), handler)
    }

    /**
     * 检查是否存在满足条件的节点
     *
     * @param nodes 需要遍历的节点集合
     * @param neighbors 获取节点邻居的函数
     * @param predicate 判断节点是否满足条件的函数
     * @return 如果存在满足条件的节点则返回true，否则返回false
     */
    fun <N> ifAny(
        nodes: Collection<N>,
        neighbors: Neighbors<N>,
        predicate: (N) -> Boolean
    ): Boolean {
        val result = BooleanArray(1)

        return dfs(nodes, neighbors, object : AbstractNodeHandler<N, Boolean>() {
            override fun beforeChildren(current: N): Boolean {
                if (predicate(current)) {
                    result[0] = true
                }
                return !result[0]
            }

            override fun result(): Boolean {
                return result[0]
            }
        })
    }

    /**
     * 从单个节点开始执行深度优先搜索
     *
     * @param node 起始节点
     * @param neighbors 获取节点邻居的函数
     * @param visited 用于标记已访问节点的对象
     * @param handler 处理节点的处理器
     * @return 处理器的结果
     */
    fun <N, R> dfsFromNode(
        node: N,
        neighbors: Neighbors<N>,
        visited: Visited<N>,
        handler: NodeHandler<N, R>
    ): R {
        doDfs(node, neighbors, visited, handler)
        return handler.result()
    }

    /**
     * 从单个节点开始执行深度优先搜索，不返回结果
     *
     * @param node 起始节点
     * @param neighbors 获取节点邻居的函数
     * @param visited 用于标记已访问节点的对象
     */
    fun <N> dfsFromNode(
        node: N,
        neighbors: Neighbors<N>,
        visited: Visited<N>
    ) {
        dfsFromNode(node, neighbors, visited, object : AbstractNodeHandler<N, Void?>() {
            override fun result(): Void? {
                return null
            }
        })
    }

    /**
     * 获取节点的拓扑排序
     *
     * @param nodes 需要排序的节点集合
     * @param neighbors 获取节点邻居的函数
     * @param visited 用于标记已访问节点的对象
     * @return 拓扑排序后的节点列表
     */
    fun <N> topologicalOrder(
        nodes: Iterable<N>,
        neighbors: Neighbors<N>,
        visited: Visited<N>
    ): List<N> {
        val handler = TopologicalOrder<N>()
        for (node in nodes) {
            doDfs(node, neighbors, visited, handler)
        }
        return handler.result()
    }

    /**
     * 获取节点的拓扑排序，使用默认的访问标记集合
     *
     * @param nodes 需要排序的节点集合
     * @param neighbors 获取节点邻居的函数
     * @return 拓扑排序后的节点列表
     */
    fun <N> topologicalOrder(
        nodes: Iterable<N>,
        neighbors: Neighbors<N>
    ): List<N> {
        return topologicalOrder(nodes, neighbors, VisitedWithSet<N>())
    }

    /**
     * 执行深度优先搜索的核心方法
     *
     * @param current 当前节点
     * @param neighbors 获取节点邻居的函数
     * @param visited 用于标记已访问节点的对象
     * @param handler 处理节点的处理器
     */
    fun <N> doDfs(
        current: N,
        neighbors: Neighbors<N>,
        visited: Visited<N>,
        handler: NodeHandler<N, *>
    ) {
        if (!visited.checkAndMarkVisited(current)) return
        if (!handler.beforeChildren(current)) return

        for (neighbor in neighbors.getNeighbors(current)) {
            doDfs(neighbor, neighbors, visited, handler)
        }
        handler.afterChildren(current)
    }

    /**
     * 节点处理器接口
     */
    interface NodeHandler<N, R> {
        /**
         * 在处理子节点之前调用
         *
         * @param current 当前节点
         * @return 如果应该继续处理子节点则返回true，否则返回false
         */
        fun beforeChildren(current: N): Boolean

        /**
         * 在处理完所有子节点之后调用
         *
         * @param current 当前节点
         */
        fun afterChildren(current: N)

        /**
         * 获取处理结果
         *
         * @return 处理结果
         */
        fun result(): R
    }

    /**
     * 获取节点邻居的接口
     */
    fun interface Neighbors<N> {
        /**
         * 获取节点的所有邻居
         *
         * @param current 当前节点
         * @return 邻居节点的集合
         */
        fun getNeighbors(current: N): Iterable<N>
    }

    /**
     * 标记已访问节点的接口
     */
    interface Visited<N> {
        /**
         * 检查并标记节点为已访问
         *
         * @param current 当前节点
         * @return 如果节点之前未被访问则返回true，否则返回false
         */
        fun checkAndMarkVisited(current: N): Boolean
    }

    /**
     * 节点处理器的抽象实现
     */
    abstract class AbstractNodeHandler<N, R> : NodeHandler<N, R> {
        override fun beforeChildren(current: N): Boolean {
            return true
        }

        override fun afterChildren(current: N) {
            // 默认实现为空
        }
    }

    /**
     * 使用Set标记已访问节点的实现
     */
    class VisitedWithSet<N> @JvmOverloads constructor(
        private val visited: MutableSet<N> = HashSet()
    ) : Visited<N> {
        override fun checkAndMarkVisited(current: N): Boolean {
            return visited.add(current)
        }
    }

    /**
     * 收集结果的节点处理器抽象类
     */
    abstract class CollectingNodeHandler<N, R, C : Iterable<R>>(
        protected val result: C
    ) : AbstractNodeHandler<N, C>() {
        override fun result(): C {
            return result
        }
    }

    /**
     * 使用链表收集结果的节点处理器抽象类
     */
    abstract class NodeHandlerWithListResult<N, R> : CollectingNodeHandler<N, R, LinkedList<R>>(LinkedList())

    /**
     * 用于拓扑排序的节点处理器
     */
    class TopologicalOrder<N> : NodeHandlerWithListResult<N, N>() {
        override fun afterChildren(current: N) {
            result.addFirst(current)
        }
    }
} 