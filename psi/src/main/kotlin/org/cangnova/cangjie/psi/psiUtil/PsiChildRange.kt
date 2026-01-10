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

package org.cangnova.cangjie.psi.psiUtil

import com.intellij.psi.PsiElement

/**
 * PSI 子元素范围
 *
 * ## 核心概念
 * 表示父元素中从 first 到 last 之间的连续子元素范围，类似于一个左闭右闭区间 [first, last]。
 *
 * ## 设计原理
 * - 实现 Sequence<PsiElement>，可以像序列一样遍历范围内的所有子元素
 * - 使用首尾指针表示范围，避免创建完整的元素列表，节省内存
 * - 懒加载迭代器，只在遍历时才访问元素
 *
 * ## 使用场景
 * 1. 表示函数的参数列表（从第一个参数到最后一个参数）
 * 2. 表示类的成员列表（从第一个成员到最后一个成员）
 * 3. 表示代码块的语句范围
 * 4. 批量操作连续的子元素（如删除、移动）
 *
 * ## 示例
 * ```kotlin
 * // 获取函数的所有参数
 * val paramRange = PsiChildRange(firstParam, lastParam)
 * paramRange.forEach { param -> println(param.text) }
 *
 * // 检查范围是否为空
 * if (!paramRange.isEmpty) {
 *     // 处理参数
 * }
 * ```
 *
 * @param first 范围的第一个元素（可以为 null，表示空范围）
 * @param last 范围的最后一个元素（可以为 null，表示空范围）
 */
data class PsiChildRange(val first: PsiElement?, val last: PsiElement?) : Sequence<PsiElement> {
    /**
     * 初始化检查
     *
     * ## 约束条件
     * 1. 如果 first 为 null，last 也必须为 null（要么都为空，要么都不为空）
     * 2. 如果不为空，first 和 last 必须有相同的父元素（必须是兄弟元素）
     *
     * 这些检查确保范围的语义正确性，避免出现无效的范围定义。
     */
    init {
        if (first == null) {
            assert(last == null)
        } else {
            assert(first.parent == last!!.parent)
        }
    }

    /**
     * 检查范围是否为空
     *
     * 空范围表示没有任何子元素（first 为 null）
     */
    val isEmpty: Boolean
        get() = first == null

    /**
     * 创建范围迭代器
     *
     * ## 迭代逻辑
     * - 如果范围为空，返回空序列的迭代器
     * - 如果范围不为空，从 first 开始遍历兄弟元素，直到遇到 last 的下一个兄弟
     *
     * ## 实现细节
     * - 使用 `siblings()` 获取兄弟元素序列（从 PsiElement.kt 扩展函数）
     * - 使用 `takeWhile { it != afterLast }` 截取范围内的元素
     * - afterLast 是 last 的下一个兄弟，作为终止条件
     *
     * ## 为什么不直接比较到 last？
     * - 使用 `takeWhile { it != afterLast }` 可以包含 last 元素本身
     * - 如果写成 `takeWhile { it != last }`，会排除 last 元素
     */
    override fun iterator(): Iterator<PsiElement> {
        val sequence = if (first == null) {
            emptySequence<PsiElement>()
        } else {
            val afterLast = last!!.nextSibling
            first.siblings().takeWhile { it != afterLast }
        }
        return sequence.iterator()
    }

    companion object {
        /**
         * 空范围的单例
         *
         * 用于表示不包含任何元素的范围，避免重复创建空范围对象
         */
        val EMPTY: PsiChildRange = PsiChildRange(null, null)

        /**
         * 创建只包含单个元素的范围
         *
         * ## 使用场景
         * - 将单个元素包装为范围，统一处理逻辑
         * - 作为范围操作的基础情况
         *
         * @param element 唯一的元素（同时作为 first 和 last）
         * @return 包含该元素的范围
         */
        fun singleElement(element: PsiElement): PsiChildRange = PsiChildRange(element, element)
    }
}
