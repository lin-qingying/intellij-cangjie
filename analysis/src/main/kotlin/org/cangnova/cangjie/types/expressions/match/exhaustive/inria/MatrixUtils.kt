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

import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.expressions.match.Matrix
import org.cangnova.cangjie.types.expressions.match.Pattern
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.deccriptorClass

/**
 * 模式矩阵工具函数
 *
 * 提供对模式矩阵的各种操作，支持 Maranget 算法的实现。
 *
 * ## 模式矩阵概念
 *
 * 模式矩阵是 match 表达式的抽象表示：
 *
 * ```cangjie
 * match (expr1, expr2) {
 *     case (Some(x), true) => ...   // 行 1: [Some(x), true]
 *     case (None, _) => ...          // 行 2: [None, _]
 *     case (_, false) => ...         // 行 3: [_, false]
 * }
 * ```
 *
 * 矩阵表示：
 * ```
 * [Some(x), true ]
 * [None,    _    ]
 * [_,       false]
 * ```
 *
 * ## 提供的功能
 *
 * - **列提取**: 获取矩阵的第一列
 * - **类型计算**: 计算列的类型
 * - **类型检查**: 验证矩阵类型一致性
 * - **去重**: 使用自定义比较器去重
 *
 * @see Matrix
 * @see MarangetChecker
 */

/**
 * 获取矩阵的第一列
 *
 * 在 Maranget 算法中，第一列用于：
 * 1. 确定需要检查的构造器集合
 * 2. 决定特化操作的目标
 *
 * @return 矩阵第一列的模式列表
 */
val Matrix.firstColumn: List<Pattern>
    get() = mapNotNull { row -> row.firstOrNull() }

/**
 * 获取矩阵第一列的类型
 *
 * 所有模式应该具有相同的类型。如果类型不一致，抛出异常。
 *
 * @return 第一列的类型，如果矩阵为空则返回 null
 * @throws MarangetException 如果第一列中的模式类型不一致
 */
val Matrix.firstColumnType: CangJieType?
    get() {
        val firstColumnTypes = firstColumn
            .map { it.type }
            .takeIf { it.isNotEmpty() }
            ?: return null

        return firstColumnTypes
            .customDistinct { a, b -> CangJieTypeChecker.DEFAULT.equalTypes(a, b) }
            .singleOrNull()
            ?: throw MarangetException("矩阵第一列的类型不一致")
    }

/**
 * 检查矩阵中所有模式的类型是否一致
 *
 * 类型一致性是 Maranget 算法正确运行的前提条件。
 * 此方法检查：
 * 1. 枚举模式的类型是否与枚举声明匹配
 * 2. 所有模式的类型是否相同
 *
 * @return 如果矩阵类型良好则返回 true
 */
fun Matrix.isWellTyped(): Boolean {
    // 检查枚举变体模式的类型是否与声明的类型匹配
    val variantPatternsTypesAreValid = flatten().all { (type, kind) ->
        when (kind) {
            is org.cangnova.cangjie.types.expressions.match.PatternKind.Enum -> {
                kind.enum == type.deccriptorClass?.source?.getPsi()
            }
            else -> true
        }
    }
    if (!variantPatternsTypesAreValid) return false

    // 检查所有模式的类型是否相同
    val types = flatten().map { it.type }
    return types.isEmpty() ||
            types.customDistinct { a, b ->
                CangJieTypeChecker.DEFAULT.equalTypes(a, b)
            }.size == 1
}

/**
 * 自定义去重函数
 *
 * 使用自定义比较器对列表进行去重。
 * 用于处理类型比较（使用 CangJieTypeChecker）。
 *
 * @param comparator 比较器函数，返回 true 表示两个元素相等
 * @return 去重后的列表，保持原始顺序
 *
 * ## 示例
 *
 * ```kotlin
 * listOf(type1, type2, type3).customDistinct { a, b ->
 *     CangJieTypeChecker.DEFAULT.equalTypes(a, b)
 * }
 * ```
 */
fun <T> List<T>.customDistinct(comparator: (T, T) -> Boolean): List<T> {
    val result = mutableListOf<T>()
    for (item in this) {
        if (result.none { comparator(it, item) }) {
            result.add(item)
        }
    }
    return result
}





