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

package org.cangnova.cangjie.types.expressions.match.exhaustive.specialized

import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.constants.BoolValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind
import org.cangnova.cangjie.types.expressions.match.exhaustive.CheckSource
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.types.isBoolean

/**
 * 第二层：布尔类型标记检查器
 *
 * 针对布尔类型的快速穷举性检查。
 * 布尔类型只有两个值（true 和 false），使用简单标记法即可完成检查。
 *
 * ## 算法原理
 *
 * ```
 * 初始状态: hasTrue = false, hasFalse = false
 *
 * 遍历分支:
 *   case true  => ... → hasTrue = true
 *   case false => ... → hasFalse = true
 *   case _     => ... → hasTrue = hasFalse = true
 *
 * 检查: hasTrue && hasFalse → 穷举完整
 * ```
 *
 * ## 仓颉语言示例
 *
 * ```cangjie
 * // 穷举完整
 * match (flag) {
 *     case true => println("是")
 *     case false => println("否")
 * }
 *
 * // 不穷举：缺少 false
 * match (flag) {
 *     case true => println("是")
 *     // error: match 表达式不穷举，缺少: false
 * }
 * ```
 *
 * ## 时间复杂度
 *
 * O(n)，其中 n 是分支数量。实际上最多 2 次迭代即可确定结果。
 *
 * ## 为什么快
 *
 * - 只需要两个布尔变量
 * - 不需要构造任何数据结构
 * - 不需要递归
 * - 可以提前终止
 *
 * @see ExhaustivenessChecker
 * @see CheckSource.BOOLEAN_FLAG
 */
class BooleanChecker : ExhaustivenessChecker {

    override val source: CheckSource = CheckSource.BOOLEAN_FLAG

    override val priority: Int = 10

    override fun isApplicable(type: CangJieType, patterns: List<Pattern>): Boolean {
        return type.isBoolean
    }

    override fun check(
        matrix: Matrix,
        type: CangJieType,
        crateRoot: CjFile?
    ): ExhaustivenessResult {
        if (!type.isBoolean) {
            return ExhaustivenessResult.Skipped
        }

        var hasTrue = false
        var hasFalse = false

        // 遍历矩阵的第一列
        for (row in matrix) {
            val pattern = row.firstOrNull() ?: continue

            when (val kind = pattern.kind) {
                // 通配符或绑定模式覆盖所有值
                is PatternKind.Wild, is PatternKind.Binding -> {
                    hasTrue = true
                    hasFalse = true
                }

                // 常量模式
                is PatternKind.Const -> {
                    val value = kind.value
                    if (value is BoolValue) {
                        if (value.value) {
                            hasTrue = true
                        } else {
                            hasFalse = true
                        }
                    }
                }

                else -> {
                    // 其他模式类型不处理
                }
            }

            // 提前终止：如果都已覆盖
            if (hasTrue && hasFalse) {
                return ExhaustivenessResult.Exhaustive
            }
        }

        // 检查缺失的情况
        return if (hasTrue && hasFalse) {
            ExhaustivenessResult.Exhaustive
        } else {
            val missing = mutableListOf<Pattern>()
            if (!hasTrue) {
                missing.add(Pattern(type, PatternKind.Const(BoolValue(true))))
            }
            if (!hasFalse) {
                missing.add(Pattern(type, PatternKind.Const(BoolValue(false))))
            }
            ExhaustivenessResult.NonExhaustive(missing, source)
        }
    }

    companion object {
        /** 单例实例 */
        val INSTANCE = BooleanChecker()
    }
}
