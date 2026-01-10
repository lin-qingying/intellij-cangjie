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

package org.cangnova.cangjie.types.checker

import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.error.ErrorType

/**
 * 仓颉类型相等性检查器
 *
 * 提供类型相等性比较的多种模式：
 * - 严格相等：所有类型细节必须完全匹配
 * - 忽略泛型参数：仅比较类型构造器
 * - 结构相等：基于类型结构比较
 */
object CangJieTypeEquality {

    /**
     * 检查两个类型是否相等
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @param typeConstructorEquality 自定义的类型构造器相等性判断（可选）
     * @return 两个类型相等时返回 true
     */
    fun areEqual(
        a: CangJieType,
        b: CangJieType,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): Boolean {
        val unwrappedA = a.unwrap()
        val unwrappedB = b.unwrap()
        return areEqualUnwrapped(unwrappedA, unwrappedB, typeConstructorEquality)
    }

    /**
     * 检查两个未包装类型是否相等
     */
    private fun areEqualUnwrapped(
        a: UnwrappedType,
        b: UnwrappedType,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): Boolean {
        // 1. 同一对象
        if (a === b) return true

        // 2. 错误类型不相等
        if (a is ErrorType || b is ErrorType) {
            return false
        }

        // 3. FlexibleType 处理
        if (a is FlexibleType && b is FlexibleType) {
            return areEqualUnwrapped(a.lowerBound, b.lowerBound, typeConstructorEquality) &&
                    areEqualUnwrapped(a.upperBound, b.upperBound, typeConstructorEquality)
        }

        // 4. 一个是 FlexibleType，另一个不是
        if (a is FlexibleType || b is FlexibleType) {
            return false
        }

        // 5. 都是 SimpleType
        if (a is SimpleType && b is SimpleType) {
            return areEqualSimple(a, b, typeConstructorEquality)
        }

        return false
    }

    /**
     * 检查两个简单类型是否相等
     */
    private fun areEqualSimple(
        a: SimpleType,
        b: SimpleType,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): Boolean {
        // 1. Option 状态必须相同
        if (a.isOption != b.isOption) {
            return false
        }

        // 2. 类型构造器必须相同
        if (!CangJieSubtypeChecker.areEqualTypeConstructors(a.constructor, b.constructor, typeConstructorEquality)) {
            return false
        }

        // 3. 特殊类型检查
        when {
            // 函数类型
            a is FunctionType && b is FunctionType -> {
                return areEqualFunctionTypes(a, b, typeConstructorEquality)
            }
            // 元组类型
            a is TupleType && b is TupleType -> {
                return areEqualTupleTypes(a, b, typeConstructorEquality)
            }
            // 其他类型
            else -> {
                return areEqualTypeArguments(a.arguments, b.arguments, typeConstructorEquality)
            }
        }
    }

    /**
     * 检查两个函数类型是否相等
     */
    private fun areEqualFunctionTypes(
        a: FunctionType,
        b: FunctionType,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): Boolean {
        // 参数数量相同
        if (a.parameterTypes.size != b.parameterTypes.size) {
            return false
        }

        // 参数类型相等
        for (i in a.parameterTypes.indices) {
            if (!areEqual(a.parameterTypes[i], b.parameterTypes[i], typeConstructorEquality)) {
                return false
            }
        }

        // 返回类型相等
        return areEqual(a.returnType, b.returnType, typeConstructorEquality)
    }

    /**
     * 检查两个元组类型是否相等
     */
    private fun areEqualTupleTypes(
        a: TupleType,
        b: TupleType,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): Boolean {
        // 元素数量相同
        if (a.elementTypes.size != b.elementTypes.size) {
            return false
        }

        // 所有元素类型相等
        for (i in a.elementTypes.indices) {
            if (!areEqual(a.elementTypes[i], b.elementTypes[i], typeConstructorEquality)) {
                return false
            }
        }

        return true
    }

    /**
     * 检查类型参数列表是否相等
     */
    private fun areEqualTypeArguments(
        a: List<TypeArgument>,
        b: List<TypeArgument>,
        typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null
    ): Boolean {
        if (a.size != b.size) {
            return false
        }

        for (i in a.indices) {
            val argA = a[i]
            val argB = b[i]

            // 比较投影的类型
            if (!areEqual(argA.type, argB.type, typeConstructorEquality)) {
                return false
            }
        }

        return true
    }

    /**
     * 检查两个类型是否相等（忽略泛型参数）
     *
     * 仅比较类型构造器，不比较类型参数
     * 例如：List<Int> 和 List<String> 在此比较下相等
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return 忽略泛型参数后两个类型相等时返回 true
     */
    fun areEqualIgnoringGenerics(a: CangJieType, b: CangJieType,
                                 typeConstructorEquality: CangJieTypeChecker.TypeConstructorEquality? = null): Boolean {
        val unwrappedA = a.unwrap()
        val unwrappedB = b.unwrap()
        return areEqualIgnoringGenericsUnwrapped(unwrappedA, unwrappedB)
    }

    /**
     * 检查未包装类型是否相等（忽略泛型参数）
     */
    private fun areEqualIgnoringGenericsUnwrapped(a: UnwrappedType, b: UnwrappedType): Boolean {
        // 1. 同一对象
        if (a === b) return true

        // 2. 错误类型
        if (a is ErrorType || b is ErrorType) {
            return false
        }

        // 3. FlexibleType
        if (a is FlexibleType && b is FlexibleType) {
            return areEqualIgnoringGenericsUnwrapped(a.lowerBound, b.lowerBound) &&
                    areEqualIgnoringGenericsUnwrapped(a.upperBound, b.upperBound)
        }

        if (a is FlexibleType || b is FlexibleType) {
            return false
        }

        // 4. SimpleType
        if (a is SimpleType && b is SimpleType) {
            // Option 状态必须相同
            if (a.isOption != b.isOption) {
                return false
            }

            // 仅比较类型构造器
            return CangJieSubtypeChecker.areEqualTypeConstructors(a.constructor, b.constructor)
        }

        return false
    }

    /**
     * 严格类型相等检查
     *
     * 与 areEqual 不同，严格相等会区分以下情况：
     * - 平台类型 vs 非平台类型
     * - 不同的型变
     * - 星投影 vs 显式边界
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return 两个类型严格相等时返回 true
     */
    fun areStrictlyEqual(a: CangJieType, b: CangJieType): Boolean {
        val unwrappedA = a.unwrap()
        val unwrappedB = b.unwrap()

        // 检查类型类别
        if (unwrappedA::class != unwrappedB::class) {
            return false
        }

        return areEqualUnwrapped(unwrappedA, unwrappedB)
    }

    /**
     * 检查类型是否与任意给定类型相等
     *
     * @param type 待检查的类型
     * @param candidates 候选类型列表
     * @return 如果 type 与任一候选类型相等则返回 true
     */
    fun isEqualToAny(type: CangJieType, candidates: List<CangJieType>): Boolean {
        return candidates.any { areEqual(type, it) }
    }

    /**
     * 检查类型列表是否完全相等
     *
     * @param a 第一个类型列表
     * @param b 第二个类型列表
     * @return 两个列表完全相等时返回 true
     */
    fun areListsEqual(a: List<CangJieType>, b: List<CangJieType>): Boolean {
        if (a.size != b.size) return false
        return a.indices.all { areEqual(a[it], b[it]) }
    }
}
