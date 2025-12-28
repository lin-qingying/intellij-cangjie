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

import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.Constructor
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind
import org.cangnova.cangjie.types.isBuiltinTupleType
import org.cangnova.cangjie.types.isEnum
import org.cangnova.cangjie.types.source

/**
 * 非穷尽性的证据（Witness）
 *
 * 在 Maranget 算法中，Witness 用于构造一个未被匹配的具体示例，
 * 帮助用户理解哪些情况没有被 match 表达式覆盖。
 *
 * ## 概念说明
 *
 * Witness（证据）是证明 match 表达式不穷举的具体反例。
 * 例如，对于以下不穷举的 match：
 *
 * ```cangjie
 * match (opt) {
 *     case Some(x) => ...
 *     // 缺少 None 分支
 * }
 * ```
 *
 * Witness 将构造出 `None` 作为反例。
 *
 * ## 工作原理
 *
 * Witness 使用栈式结构来递归地构造反例模式：
 *
 * 1. **递归下降**：算法深入模式结构时，向栈中推入通配符
 * 2. **构造器应用**：回溯时，从栈顶弹出子模式并合并为构造器模式
 * 3. **最终结果**：算法结束时，栈中只剩一个完整的反例模式
 *
 * ## 示例
 *
 * 对于 `Option<(Int64, Bool)>` 类型，构造 `Some((_, false))` 反例：
 *
 * ```
 * 初始: []
 * pushWild(Bool): [_: Bool]
 * applyConstructor(false): [false]
 * pushWild(Int64): [false, _: Int64]
 * applyConstructor(Tuple): [(_, false)]
 * applyConstructor(Some): [Some((_, false))]
 * ```
 *
 * @property patterns 证据中的模式列表（可变，作为栈使用）
 *
 * @see Usefulness
 * @see MarangetChecker
 */
class Witness(val patterns: MutableList<Pattern> = mutableListOf()) {

    override fun toString(): String = patterns.toString()

    /**
     * 克隆当前 Witness
     *
     * 创建一个独立的副本，用于在分支探索时保留当前状态。
     *
     * @return 包含相同模式的新 Witness 实例
     */
    fun clone(): Witness = Witness(patterns.toMutableList())

    /**
     * 用通配符子模式填充构造器，然后应用构造器
     *
     * 这是构造完整反例的主要方法。首先为构造器的每个参数
     * 推入通配符模式，然后应用构造器将它们合并。
     *
     * @param constructor 要应用的构造器
     * @param type 构造器对应的类型
     * @return 修改后的 Witness（支持链式调用）
     *
     * ## 示例
     *
     * 对于 `Option<Int64>` 的 `Some` 构造器：
     * - 推入 `_: Int64`（Some 的参数）
     * - 应用 Some 构造器得到 `Some(_)`
     */
    fun pushWildConstructor(constructor: Constructor, type: CangJieType): Witness {
        val subPatternTypes = constructor.subTypes(type)
        for (ty in subPatternTypes) {
            patterns.add(Pattern.wild(ty))
        }
        return applyConstructor(constructor, type)
    }

    /**
     * 将栈顶的几个模式合并为一个构造器模式
     *
     * 这是 Witness 构造的核心操作。根据构造器的元数（arity），
     * 从栈顶弹出相应数量的子模式，然后创建新的构造器模式。
     *
     * @param constructor 要应用的构造器
     * @param type 构造器对应的类型
     * @return 修改后的 Witness（支持链式调用）
     *
     * ## 示例
     *
     * 对于元组类型 `(Int64, Bool)`：
     * - 栈: [_, false]（两个子模式）
     * - 应用 Tuple 构造器
     * - 结果栈: [(_, false)]
     */
    fun applyConstructor(constructor: Constructor, type: CangJieType): Witness {
        val arity = constructor.arity(type)
        val len = patterns.size

        // 从栈顶取出 arity 个模式
        val oldPatterns = patterns.subList(len - arity, len)
        val pats = oldPatterns.reversed().toList()
        oldPatterns.clear()

        // 根据类型和构造器创建新模式
        val kind = when {
            type.isBuiltinTupleType -> {
                PatternKind.Tuple(pats)
            }

            type.isEnum -> {
                PatternKind.Enum(
                    type.source as org.cangnova.cangjie.psi.CjEnum,
                    (constructor as Constructor.Enum).entry,
                    pats
                )
            }

            else -> if (constructor is Constructor.ConstantValue) {
                PatternKind.Const(constructor.value)
            } else {
                PatternKind.Wild
            }
        }

        patterns.add(Pattern(type, kind))
        return this
    }
}

/**
 * 有用性（Usefulness）
 *
 * 表示一个模式相对于已有模式矩阵是否"有用"，
 * 即是否覆盖了新的匹配情况。这是 Maranget 算法的核心概念。
 *
 * ## 概念说明
 *
 * 在模式匹配理论中，"有用性"定义如下：
 *
 * > 给定模式矩阵 P 和模式向量 q，如果存在某个值 v 使得：
 * > 1. v 匹配 q
 * > 2. v 不匹配 P 中的任何行
 * > 则称 q 相对于 P 是"有用的"。
 *
 * ## 在穷举性检查中的应用
 *
 * 穷举性检查通过检查通配符模式 `_` 的有用性来工作：
 *
 * - 如果 `_` 相对于现有模式矩阵有用，说明存在未覆盖的值
 * - 如果 `_` 无用，说明所有可能的值都已被覆盖
 *
 * ## 结果类型
 *
 * | 类型 | 含义 | 使用场景 |
 * |------|------|----------|
 * | [UsefulWithWitness] | 有用，带反例 | 需要报告缺失模式时 |
 * | [Useful] | 有用，无反例 | 只需知道是否有用时 |
 * | [Useless] | 无用 | 模式已被完全覆盖 |
 *
 * @see Witness
 * @see MarangetChecker
 */
sealed class Usefulness {

    /**
     * 模式有用，且包含非穷尽性证据
     *
     * @property witnesses 证据列表，每个证据是一个未被覆盖的具体示例
     */
    class UsefulWithWitness(val witnesses: List<Witness>) : Usefulness() {
        companion object {
            /** 空的有用性证据（用于初始化递归） */
            val Empty: UsefulWithWitness get() = UsefulWithWitness(listOf(Witness()))
        }
    }

    /**
     * 模式有用（但不需要构造证据）
     */
    data object Useful : Usefulness()

    /**
     * 模式无用（已被之前的模式完全覆盖）
     */
    data object Useless : Usefulness()

    /**
     * 是否有用
     */
    val isUseful: Boolean get() = this !== Useless
}
