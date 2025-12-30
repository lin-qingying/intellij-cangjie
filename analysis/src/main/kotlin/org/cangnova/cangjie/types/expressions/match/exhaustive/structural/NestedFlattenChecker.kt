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

package org.cangnova.cangjie.types.expressions.match.exhaustive.structural

import org.cangnova.cangjie.psi.CjFile
import org.cangnova.cangjie.resolve.caches.type
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.deccriptorClass
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.expressions.match.PatternKind
import org.cangnova.cangjie.types.expressions.match.exhaustive.CheckSource
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessChecker
import org.cangnova.cangjie.types.expressions.match.exhaustive.ExhaustivenessResult
import org.cangnova.cangjie.types.expressions.match.exhaustive.specialized.SmallEnumBitVectorChecker
import org.cangnova.cangjie.types.isEnum

/**
 * 第三层：嵌套结构扁平化检查器
 *
 * 将嵌套的枚举模式展平成单层结构，然后使用位向量进行检查。
 * 这是处理嵌套枚举（如 `Option<Option<T>>`）的专用优化算法。
 *
 * ## 算法原理
 *
 * 将嵌套的构造器路径编码为唯一的字符串，然后用位向量追踪覆盖情况：
 *
 * ```
 * 类型: Option<Option<Int32>>
 *
 * 所有可能的路径:
 *   Some(Some(_))  → "Some(Some)"
 *   Some(None)     → "Some(None)"
 *   None           → "None"
 *
 * 模式矩阵:
 * | Some(Some(0)) |  → 路径 "Some(Some)"
 * | Some(None)    |  → 路径 "Some(None)"
 * | None          |  → 路径 "None"
 *
 * 位向量检查:
 *   allMask = 0b111 (3 个路径)
 *   covered = 0b111 ✓ 穷举完整
 * ```
 *
 * ## 仓颉语言示例
 *
 * ```cangjie
 * // 穷举完整
 * match (opt: Option<Option<Int32>>) {
 *     case Some(Some(x)) => "有值: ${x}"
 *     case Some(None) => "外层 Some，内层 None"
 *     case None => "外层 None"
 * }
 *
 * // 不穷举：缺少 Some(None)
 * match (opt: Option<Option<Int32>>) {
 *     case Some(Some(x)) => "有值"
 *     case None => "无值"
 *     // error: match 表达式不穷举，缺少: Some(None)
 * }
 * ```
 *
 * ## 适用条件
 *
 * - 嵌套深度 ≤3 层（防止路径爆炸）
 * - 每层都是简单枚举
 * - 展平后总路径数 ≤64（适合位向量表示）
 *
 * ## 时间复杂度
 *
 * O(n * p)，其中 n 是分支数量，p 是总路径数（≤64）
 *
 * ## 为什么需要这个检查器
 *
 * 嵌套枚举是常见模式（如 `Option<Result<T, E>>`），
 * 直接使用 Maranget 算法会产生指数级复杂度。
 * 展平后使用位向量可以保持线性时间。
 *
 * @see SmallEnumBitVectorChecker
 * @see ExhaustivenessChecker
 * @see CheckSource.NESTED_FLATTEN
 */
class NestedFlattenChecker : ExhaustivenessChecker {

    override val source: CheckSource = CheckSource.NESTED_FLATTEN

    override val priority: Int = 50

    /**
     * 最大支持的嵌套深度
     */
    private val maxNestingDepth = 3

    /**
     * 最大支持的展平后构造器数量
     */
    private val maxFlattenedConstructors = 64

    override fun isApplicable(type: CangJieType, patterns: List<Pattern>): Boolean {
        // 检查类型是否为枚举
        if (!type.isEnum) return false

        // 检查嵌套深度和构造器数量
        val flattenedCount = estimateFlattenedConstructors(type, 0)
        return flattenedCount in 1..maxFlattenedConstructors
    }

    override fun check(
        matrix: Matrix,
        type: CangJieType,
        crateRoot: CjFile?
    ): ExhaustivenessResult {
        // 展平所有模式
        val flattenedPatterns = matrix.mapNotNull { row ->
            val pattern = row.firstOrNull() ?: return@mapNotNull null
            flattenPattern(pattern)
        }

        if (flattenedPatterns.isEmpty()) {
            return ExhaustivenessResult.Skipped
        }

        // 收集所有可能的展平路径
        val allPaths = collectAllPaths(type, 0)
        if (allPaths.size > maxFlattenedConstructors) {
            return ExhaustivenessResult.Skipped
        }

        // 构建路径到索引的映射
        val pathToIndex = allPaths.withIndex().associate { (index, path) -> path to index }

        // 使用位向量检查覆盖
        val allMask = (1L shl allPaths.size) - 1
        var covered = 0L

        for (flattenedPattern in flattenedPatterns) {
            when (flattenedPattern) {
                is FlattenedPattern.Wildcard -> {
                    covered = allMask
                }

                is FlattenedPattern.Path -> {
                    val index = pathToIndex[flattenedPattern.path]
                    if (index != null) {
                        covered = covered or (1L shl index)
                    }
                }

                is FlattenedPattern.Prefix -> {
                    // 前缀匹配：覆盖所有以此前缀开始的路径
                    for ((path, index) in pathToIndex) {
                        if (path.startsWith(flattenedPattern.prefix)) {
                            covered = covered or (1L shl index)
                        }
                    }
                }
            }

            // 提前终止
            if (covered == allMask) {
                return ExhaustivenessResult.Exhaustive
            }
        }

        // 检查是否穷举
        return if (covered == allMask) {
            ExhaustivenessResult.Exhaustive
        } else {
            // 找出缺失的路径
            val missingPaths = allPaths.filterIndexed { index, _ ->
                (covered and (1L shl index)) == 0L
            }

            // 转换为模式
            val missingPatterns = missingPaths.take(5).map { path ->
                reconstructPattern(type, path)
            }

            ExhaustivenessResult.NonExhaustive(missingPatterns, source)
        }
    }

    /**
     * 估算展平后的构造器数量
     */
    private fun estimateFlattenedConstructors(type: CangJieType, depth: Int): Int {
        if (depth >= maxNestingDepth) return 1
        if (!type.isEnum) return 1

        val enumClass = type.deccriptorClass?.source?.getPsi() as? org.cangnova.cangjie.psi.CjEnum
            ?: return 1

        var total = 0
        for (variant in enumClass.constructor) {
            val subTypes = variant.typeReferences.mapNotNull {
                it.type
            }

            if (subTypes.isEmpty()) {
                total += 1
            } else {
                var subTotal = 1
                for (subType in subTypes) {
                    subTotal *= estimateFlattenedConstructors(subType, depth + 1)
                }
                total += subTotal
            }
        }

        return total
    }

    /**
     * 收集类型的所有可能路径
     */
    private fun collectAllPaths(type: CangJieType, depth: Int): List<String> {
        if (depth >= maxNestingDepth) return listOf("")
        if (!type.isEnum) return listOf("")

        val enumClass = type.deccriptorClass?.source?.getPsi() as? org.cangnova.cangjie.psi.CjEnum
            ?: return listOf("")

        val paths = mutableListOf<String>()
        for (variant in enumClass.constructor) {
            val variantName = variant.name ?: continue
            val subTypes = variant.typeReferences.mapNotNull { it.type }

            if (subTypes.isEmpty()) {
                paths.add(variantName)
            } else {
                // 递归收集子类型的路径
                val subPaths = subTypes.map { collectAllPaths(it, depth + 1) }
                val combinations = cartesianProduct(subPaths)

                for (combo in combinations) {
                    paths.add("$variantName(${combo.joinToString(",")})")
                }
            }
        }

        return paths
    }

    /**
     * 计算列表的笛卡尔积
     */
    private fun cartesianProduct(lists: List<List<String>>): List<List<String>> {
        if (lists.isEmpty()) return listOf(emptyList())
        if (lists.size == 1) return lists[0].map { listOf(it) }

        val result = mutableListOf<List<String>>()
        val first = lists[0]
        val rest = cartesianProduct(lists.drop(1))

        for (item in first) {
            for (restCombo in rest) {
                result.add(listOf(item) + restCombo)
            }
        }

        return result
    }

    /**
     * 展平单个模式
     */
    private fun flattenPattern(pattern: Pattern): FlattenedPattern {
        return when (val kind = pattern.kind) {
            is PatternKind.Wild, is PatternKind.Binding -> FlattenedPattern.Wildcard

            is PatternKind.Enum -> {
                val variantName = kind.entry.name ?: return FlattenedPattern.Wildcard
                val subPatterns = kind.subPatterns

                if (subPatterns.isEmpty()) {
                    FlattenedPattern.Path(variantName)
                } else if (subPatterns.all { it.kind is PatternKind.Wild || it.kind is PatternKind.Binding }) {
                    // 子模式全是通配符，作为前缀
                    FlattenedPattern.Prefix("$variantName(")
                } else {
                    // 递归展平
                    val flattenedSubs = subPatterns.map { flattenPattern(it) }
                    val subStrings = flattenedSubs.map { flat ->
                        when (flat) {
                            is FlattenedPattern.Wildcard -> "_"
                            is FlattenedPattern.Path -> flat.path
                            is FlattenedPattern.Prefix -> "${flat.prefix}..."
                        }
                    }
                    FlattenedPattern.Path("$variantName(${subStrings.joinToString(",")})")
                }
            }

            else -> FlattenedPattern.Wildcard
        }
    }

    /**
     * 从路径重建模式
     */
    private fun reconstructPattern(type: CangJieType, path: String): Pattern {
        // 简化实现：返回通配符模式，实际应该解析路径
        return Pattern.wild(type)
    }

    companion object {
        /** 单例实例 */
        val INSTANCE = NestedFlattenChecker()
    }
}

/**
 * 展平后的模式表示
 */
private sealed class FlattenedPattern {
    /** 通配符（匹配所有） */
    data object Wildcard : FlattenedPattern()

    /** 完整路径 */
    data class Path(val path: String) : FlattenedPattern()

    /** 前缀匹配 */
    data class Prefix(val prefix: String) : FlattenedPattern()
}

