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

package org.cangnova.cangjie.container

// ==================== 数据结构算法 ====================

/**
 * 拓扑排序算法 - 用于组件依赖关系排序
 *
 * 使用深度优先搜索（DFS）算法对具有依赖关系的元素进行拓扑排序。
 * 该算法确保依赖项在被依赖项之前出现（或之后，取决于 reverseOrder 参数）。
 *
 * **核心用途**：
 * - 组件销毁顺序：确保依赖者在被依赖者之前销毁
 * - 组件初始化顺序：确保依赖项在使用前已初始化
 * - 任务调度：根据依赖关系安排执行顺序
 *
 * **算法特点**：
 * - 时间复杂度：O(V + E)，其中 V 是元素数量，E 是依赖关系数量
 * - 空间复杂度：O(V)，用于存储访问状态
 * - 循环依赖处理：检测到循环时静默跳过（不抛出异常）
 * - 稳定性：保持输入顺序的相对稳定性
 *
 * **DFS 访问状态**：
 * - 未访问：不在任何集合中
 * - 正在访问：在 itemsInProgress 中（用于检测循环）
 * - 已完成：在 completedItems 中（避免重复访问）
 *
 * **示例**：
 * ```kotlin
 * // 组件依赖关系: A -> B, B -> C
 * val components = listOf(A, B, C)
 * val sorted = topologicalSort(components) { component ->
 *     // 返回此组件依赖的其他组件
 *     component.dependencies
 * }
 * // 结果: [C, B, A]（C 没有依赖，先处理；A 依赖最多，最后处理）
 * ```
 *
 * @param T 元素类型
 * @param items 要排序的元素集合
 * @param reverseOrder 是否反转顺序（默认 false）
 *   - false: 依赖项在前，被依赖项在后（适用于初始化）
 *   - true: 被依赖项在前，依赖项在后（适用于销毁）
 * @param dependencies 依赖关系函数，返回给定元素的所有依赖项
 * @return 排序后的元素列表
 *
 * @see ComponentStorage.getDescriptorsInDisposeOrder 使用此算法确定组件销毁顺序
 */
fun <T> topologicalSort(items: Iterable<T>, reverseOrder: Boolean = false, dependencies: (T) -> Iterable<T>): List<T> {
    /** 正在访问的元素集合（DFS 栈），用于检测循环依赖 */
    val itemsInProgress = HashSet<T>()

    /** 已完成访问的元素集合，避免重复访问 */
    val completedItems = HashSet<T>()

    /** 排序结果列表，按访问完成顺序添加 */
    val result = ArrayList<T>()

    /**
     * 深度优先搜索访问函数
     *
     * 递归地访问元素及其所有依赖项，确保依赖项先被处理。
     *
     * @param item 当前访问的元素
     */
    fun DfsVisit(item: T) {
        // 如果已完成访问，跳过
        if (item in completedItems)
            return

        // 如果正在访问，说明存在循环依赖，跳过（避免无限递归）
        if (item in itemsInProgress)
            return // 注意：这里不抛出 CycleInTopoSortException，而是静默处理循环

        // 标记为正在访问
        itemsInProgress.add(item)

        // 递归访问所有依赖项
        for (dependency in dependencies(item)) {
            DfsVisit(dependency)
        }

        // 完成访问：从进行中移除，添加到已完成集合和结果列表
        itemsInProgress.remove(item)
        completedItems.add(item)
        result.add(item)
    }

    // 访问所有元素
    for (item in items)
        DfsVisit(item)

    // 根据 reverseOrder 参数决定是否反转结果
    // DFS 后序遍历的结果是依赖项在后，reverse() 后变为依赖项在前
    return result.apply { if (!reverseOrder) reverse() }
}
