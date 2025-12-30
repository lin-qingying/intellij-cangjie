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
 */

package org.cangnova.cangjie.types.expressions.match.exhaustive.inria

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.Constructor
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind
import org.cangnova.cangjie.types.checker.CangJieTypeChecker

/**
 * 模式行特化器（Row Specializer）
 *
 * 特化（Specialization）是 Maranget 算法的核心操作之一。
 * 它将模式矩阵相对于某个构造器进行"投影"，
 * 保留与该构造器兼容的行并展开其子模式。
 *
 * ## 特化操作定义
 *
 * 给定构造器 c（元数为 a）和模式行 [p₁, p₂, ..., pₙ]，
 * 特化操作 S(c, row) 的结果取决于 p₁：
 *
 * | p₁ 类型 | S(c, row) 结果 |
 * |---------|----------------|
 * | c(q₁,...,qₐ) | [q₁, ..., qₐ, p₂, ..., pₙ] |
 * | c'(...) 其中 c' ≠ c | ∅ (不兼容) |
 * | _ 或 x | [_, ..., _, p₂, ..., pₙ] (a 个通配符) |
 *
 * ## 示例
 *
 * 对于枚举 `Option<Int64>`：
 *
 * ```
 * 原始矩阵:
 * [Some(x), y]
 * [None, z]
 * [_, w]
 *
 * S(Some, matrix):
 * [x, y]      // Some(x) 展开为 x
 * [_, w]      // _ 对 Some 兼容
 *
 * S(None, matrix):
 * [z]         // None 无参数
 * [w]         // _ 对 None 兼容
 * ```
 *
 * ## 在穷举性检查中的作用
 *
 * 特化操作通过递归地处理每个构造器，
 * 将高维的穷举性问题分解为更简单的子问题。
 *
 * @see MarangetChecker
 * @see Constructor
 */
object RowSpecializer {

    /**
     * 对给定的一行模式进行特化
     *
     * @param row 当前的模式行
     * @param constructor 用于特化的构造器
     * @param type 当前构造器应用的类型
     * @return 特化后的模式行，如果不兼容则返回 null
     *
     * ## 算法步骤
     *
     * 1. 取出行的第一个模式 p₁
     * 2. 根据 p₁ 的类型决定如何特化
     * 3. 将特化后的头部与剩余模式拼接
     */
    fun specializeRow(
        row: List<Pattern>,
        constructor: Constructor,
        type: CangJieType
    ): List<Pattern>? {
        // 获取行中的第一个模式，如果行为空，则返回空列表
        val firstPattern = row.firstOrNull() ?: return emptyList()

        // 使用构造器的子类型创建通配模式列表
        val wildPatterns = constructor
            .subTypes(type)
            .map { subType -> Pattern.wild(subType) }
            .toMutableList()

        // 根据第一个模式的类型进行特化
        val head: List<Pattern>? = when (val kind = firstPattern.kind) {
            is PatternKind.Enum -> {
                // 枚举变体模式：检查构造器是否匹配
                if (constructor == firstPattern.constructors?.first()) {
                    wildPatterns.apply { fillWithSubPatterns(kind.subPatterns) }
                } else {
                    null
                }
            }

            is PatternKind.Tuple -> {
                // 元组模式：直接填充子模式
                wildPatterns.apply { fillWithSubPatterns(kind.subPatterns) }
            }

            is PatternKind.Const -> {
                // 常量模式：检查常量值是否被构造器覆盖
                when {
                    constructor.coveredByRange(kind.value, kind.value, true) -> emptyList()
                    else -> null
                }
            }

            is PatternKind.Type -> {
                // 类型模式：检查类型是否匹配
                if (CangJieTypeChecker.DEFAULT.equalTypes(kind.type, type)) {
                    wildPatterns
                } else if (CangJieTypeChecker.DEFAULT.isSubtypeOf(kind.type, type)) {
                    // 支持子类型关系
                    wildPatterns
                } else {
                    null
                }
            }

            PatternKind.Wild, is PatternKind.Binding -> {
                // 通配符或绑定模式：直接返回通配符列表
                wildPatterns
            }

            PatternKind.Error -> null
        }

        // 返回特化后的模式行：特化的头部 + 剩余模式
        return head?.plus(row.subList(1, row.size))
    }

    /**
     * 用子模式填充通配符列表
     *
     * 当特化枚举或元组模式时，需要将子模式填入预先创建的通配符列表中。
     * 如果子模式数量少于预期，剩余位置保持通配符。
     *
     * @param subPatterns 子模式列表
     */
    private fun MutableList<Pattern>.fillWithSubPatterns(subPatterns: List<Pattern>) {
        for ((index, pattern) in subPatterns.withIndex()) {
            // 确保列表足够长
            while (size <= index) {
                add(Pattern.wild())
            }
            this[index] = pattern
        }
    }
}
