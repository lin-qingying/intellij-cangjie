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

import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.psi.CjEnumConstructor
import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.deccriptorClass
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind
import org.cangnova.cangjie.types.expressions.match.exhaustive.CheckSource
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.types.isEnum

/**
 * 第二层：小枚举位向量检查器
 *
 * 针对小型枚举（≤64 个变体）的快速穷举性检查。
 * 使用位向量（64位整数）表示覆盖情况，是最高效的枚举检查算法。
 *
 * ## 算法原理
 *
 * 用一个 64 位整数表示覆盖情况，每个 bit 对应一个枚举变体：
 *
 * ```
 * enum Color { Red, Green, Blue }
 *
 * 变体映射:
 *   Red   → bit 0 (0b001)
 *   Green → bit 1 (0b010)
 *   Blue  → bit 2 (0b100)
 *
 * 全覆盖掩码: allVariantsMask = 0b111
 *
 * 模式处理:
 *   case Red   => ... → covered |= 0b001 → covered = 0b001
 *   case Blue  => ... → covered |= 0b100 → covered = 0b101
 *   case Green => ... → covered |= 0b010 → covered = 0b111 ✓ 穷举完整
 * ```
 *
 * ## 仓颉语言示例
 *
 * ```cangjie
 * enum Direction {
 *     North | South | East | West
 * }
 *
 * // 穷举完整
 * match (dir) {
 *     case North => "上"
 *     case South => "下"
 *     case East => "左"
 *     case West => "右"
 * }
 *
 * // 不穷举：缺少 West
 * match (dir) {
 *     case North | South => "垂直"
 *     case East => "水平"
 *     // error: match 表达式不穷举，缺少: West
 * }
 * ```
 *
 * ## 时间复杂度
 *
 * O(n)，其中 n 是分支数量
 *
 * ## 为什么快
 *
 * - 位运算极快（CPU 单指令完成 OR 操作）
 * - 不需要动态内存分配
 * - 检查穷举性只需一次整数比较
 * - 可以提前终止（covered == allVariantsMask）
 *
 * ## 适用范围
 *
 * - 约 85% 的枚举都 ≤64 个变体
 * - 覆盖 Option, Result, Ordering 等常用类型
 * - 大部分用户定义的枚举
 *
 * @see ExhaustivenessChecker
 * @see CheckSource.ENUM_BITVECTOR
 */
class SmallEnumBitVectorChecker : ExhaustivenessChecker {

    override val source: CheckSource = CheckSource.ENUM_BITVECTOR

    override val priority: Int = 20

    /**
     * 最大支持的枚举变体数量（64 位整数的位数）
     */
    private val maxVariants = 64

    override fun isApplicable(type: CangJieType, patterns: List<Pattern>): Boolean {
        if (!type.isEnum) return false

        // 获取枚举变体数量
        val enumClass = type.deccriptorClass?.source?.getPsi() as? CjEnum ?: return false
        val variantCount = enumClass.constructor.size

        // 只处理小型枚举（≤64 个变体）
        return variantCount in 1..maxVariants
    }

    override fun check(
        matrix: Matrix,
        type: CangJieType,
        crateRoot: CjFile?
    ): ExhaustivenessResult {
        // 获取枚举信息
        val enumClass = type.deccriptorClass?.source?.getPsi() as? CjEnum
            ?: return ExhaustivenessResult.Skipped

        val variants = enumClass.constructor
        val variantCount = variants.size

        if (variantCount == 0 || variantCount > maxVariants) {
            return ExhaustivenessResult.Skipped
        }

        // 构建变体到索引的映射
        val variantToIndex = variants.withIndex().associate { (index, variant) ->
            variant.name to index
        }

        // 计算全覆盖掩码
        val allVariantsMask = (1L shl variantCount) - 1

        // 位向量表示已覆盖的变体
        var covered = 0L

        // 遍历矩阵
        for (row in matrix) {
            val pattern = row.firstOrNull() ?: continue

            when (val kind = pattern.kind) {
                // 通配符覆盖所有变体
                is PatternKind.Wild, is PatternKind.Binding -> {
                    covered = allVariantsMask
                }

                // 枚举变体模式
                is PatternKind.Enum -> {
                    val variantName = kind.entry.name
                    val index = variantToIndex[variantName]
                    if (index != null) {
                        covered = covered or (1L shl index)
                    }
                }

                else -> {
                    // 其他模式类型不处理
                }
            }

            // 提前终止：如果已全部覆盖
            if (covered == allVariantsMask) {
                return ExhaustivenessResult.Exhaustive
            }
        }

        // 检查是否穷举
        return if (covered == allVariantsMask) {
            ExhaustivenessResult.Exhaustive
        } else {
            // 找出缺失的变体
            val missingPatterns = mutableListOf<Pattern>()
            for ((index, variant) in variants.withIndex()) {
                if ((covered and (1L shl index)) == 0L) {
                    missingPatterns.add(createEnumPattern(type, enumClass, variant))
                }
            }
            ExhaustivenessResult.NonExhaustive(missingPatterns, source)
        }
    }

    /**
     * 创建枚举变体模式
     */
    private fun createEnumPattern(
        type: CangJieType,
        enumClass: CjEnum,
        variant: CjEnumConstructor
    ): Pattern {
        // 如果变体有参数，用通配符填充
        val subPatterns = variant.typeReferences.map { Pattern.wild() }
        return Pattern(type, PatternKind.Enum(enumClass, variant, subPatterns))
    }

    companion object {
        /** 单例实例 */
        val INSTANCE = SmallEnumBitVectorChecker()
    }
}
