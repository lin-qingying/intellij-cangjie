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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.types.*

/**
 * 类型检查过程
 *
 * 实现仓颉语言的类型检查算法，包括：
 * - 类型相等性检查
 * - 子类型关系检查
 * - 类型参数处理
 *
 * ## 仓颉语言特性
 * 与 Kotlin 不同，仓颉语言：
 * - **不支持类型投影** (argument kind)：没有 `in`/`out` 修饰符
 * - **类型参数不变** (invariant)：所有泛型类型参数都是不变的
 * - **简化的类型捕获**：不需要处理协变/逆变投影
 *
 * 核心功能：
 * - equalsIgnoringGenerics：检查类型是否相等（忽略泛型）
 * - isSubtypeOf：检查子类型关系
 * - equalTypes：完整的类型相等性检查
 *
 * 示例：
 * ```kotlin
 * val procedure = TypeCheckingProcedure(callbacks)
 *
 * // 检查类型相等性
 * val isEqual = procedure.equalsIgnoringGenerics(type1, type2)
 *
 * // 检查子类型关系
 * val isSubtype = procedure.isSubtypeOf(subtype, supertype)
 * ```
 */
@Deprecated(
    "Use CangJieTypeChecker with ConstraintCheckContext instead",
    ReplaceWith("CangJieTypeChecker.DEFAULT"),
    level = DeprecationLevel.WARNING
)
class TypeCheckingProcedure(private val constraints: TypeCheckingProcedureCallbacks) {

    /**
     * 检查两个类型是否相等（忽略泛型参数的具体类型）
     *
     * 比较两个类型是否相等，但忽略泛型参数的具体类型。
     * 对于灵活类型，使用异构等价性检查。
     *
     * 示例：
     * ```kotlin
     * val type1: CangJieType = List<Int>
     * val type2: CangJieType = List<String>
     * val isEqual = procedure.equalsIgnoringGenerics(type1, type2) // true（忽略泛型参数）
     *
     * val type3: CangJieType = Int
     * val type4: CangJieType = String
     * val isEqual = procedure.equalsIgnoringGenerics(type3, type4) // false
     * ```
     *
     * @param type1 第一个类型
     * @param type2 第二个类型
     * @return true 如果类型相等（忽略泛型），false 否则
     */
    fun equalsIgnoringGenerics(type1: CangJieType, type2: CangJieType): Boolean {
        if (type1 === type2) return true
        if (type1.isFlexible()) {
            if (type2.isFlexible()) {
                return !type1.isError && !type2.isError &&
                        isSubtypeOf(type1, type2) && isSubtypeOf(type2, type1)
            }
            return heterogeneousEquivalence(type2, type1)
        } else if (type2.isFlexible()) {
            return heterogeneousEquivalence(type1, type2)
        }

        if (type1.isOption != type2.isOption) {
            return false
        }

        if (type1.isOption) {
            return constraints.assertEqualTypes(
                type1.unwrapOption(),
                type2.unwrapOption(),
                this
            )
        }

        val constructor1 = type1.constructor
        val constructor2 = type2.constructor

        if (!constraints.assertEqualTypeConstructors(constructor1, constructor2)) {
            return false
        }

        val type1Arguments  = type1.arguments
        val type2Arguments  = type2.arguments
        if (type1Arguments.size != type2Arguments.size) {
            return false
        }

        for (i in type1Arguments.indices) {
            val typeArg1 = type1Arguments[i]
            val typeArg2 = type2Arguments[i]

            if (!constraints.assertEqualTypes(typeArg1.type, typeArg2.type, this)) {
                return false
            }
        }
        return true
    }

    /**
     * 检查子类型关系
     *
     * 检查 subtype 是否为 supertype 的子类型。
     * 这是类型检查的核心方法，处理各种类型关系。
     *
     * 示例：
     * ```kotlin
     * val intType: CangJieType = Int类型
     * val numberType: CangJieType = Number类型
     * val isSubtype = procedure.isSubtypeOf(intType, numberType) // true
     *
     * val stringType: CangJieType = String类型
     * val isSubtype = procedure.isSubtypeOf(stringType, numberType) // false
     * ```
     *
     * @param subtype 子类型
     * @param supertype 超类型
     * @return true 如果 subtype 是 supertype 的子类型，false 否则
     */
    fun isSubtypeOf(subtype: CangJieType, supertype: CangJieType): Boolean {
        if (sameTypeConstructors(subtype, supertype)) {
            // Option类型的子类型关系
            if (subtype.isOption && supertype.isOption) {
                return isSubtypeOf(subtype.unwrapOption(), supertype.unwrapOption())
            }
            return true
        }

        val subtypeRepresentative: CangJieType = subtype.getSubtypeRepresentative()
        val supertypeRepresentative: CangJieType = supertype.getSupertypeRepresentative()
        if (subtypeRepresentative !== subtype || supertypeRepresentative !== supertype) {
            return isSubtypeOf(subtypeRepresentative, supertypeRepresentative)
        }
        return isSubtypeOfForRepresentatives(subtype, supertype)
    }

    /**
     * 检查异构类型的等价性
     *
     * 处理灵活类型与非灵活类型之间的比较。
     *
     * @param inflexibleType 非灵活类型
     * @param flexibleType 灵活类型
     * @return true 如果类型等价
     */
    protected fun heterogeneousEquivalence(inflexibleType: CangJieType, flexibleType: CangJieType): Boolean {
        assert(!inflexibleType.isFlexible()) { "Only inflexible types are allowed here: $inflexibleType" }
        return isSubtypeOf(flexibleType.asFlexibleType().lowerBound, inflexibleType)
                && isSubtypeOf(inflexibleType, flexibleType.asFlexibleType().upperBound)
    }

    /**
     * 检查完整的类型相等性
     *
     * 与 equalsIgnoringGenerics 不同，这个方法会检查泛型参数的具体类型。
     *
     * @param type1 第一个类型
     * @param type2 第二个类型
     * @return true 如果类型完全相等
     */
    fun equalTypes(type1: CangJieType, type2: CangJieType): Boolean {
        if (type1 === type2) return true
        if (type1.isFlexible()) {
            if (type2.isFlexible()) {
                return !type1.isError && !type2.isError &&
                        isSubtypeOf(type1, type2) && isSubtypeOf(type2, type1)
            }
            return heterogeneousEquivalence(type2, type1)
        } else if (type2.isFlexible()) {
            return heterogeneousEquivalence(type1, type2)
        }

        // 检查Option类型
        if (type1.isOption != type2.isOption) {
            return false
        }

        if (type1.isOption) {
            return constraints.assertEqualTypes(
                type1.unwrapOption(),
                type2.unwrapOption(),
                this
            )
        }

        val constructor1 = type1.constructor
        val constructor2 = type2.constructor

        if (!constraints.assertEqualTypeConstructors(constructor1, constructor2)) {
            return false
        }

        val type1Arguments  = type1.arguments
        val type2Arguments  = type2.arguments
        if (type1Arguments.size != type2Arguments.size) {
            return false
        }

        for (i in type1Arguments.indices) {
            val typeArg1 = type1Arguments[i]
            val typeArg2 = type2Arguments[i]

            if (!constraints.assertEqualTypes(typeArg1.type, typeArg2.type, this)) {
                return false
            }
        }
        return true
    }

    /**
     * 检查两个代表性类型之间的子类型关系
     *
     * 这是子类型检查的核心实现，处理各种特殊情况。
     *
     * ## 编译器对应逻辑
     * 参考 cangjie_compiler/src/Sema/TypeManager.cpp IsSubtype() 函数
     *
     * ### Option 类型的子类型规则
     * 根据编译器实现 (TypeManager.cpp:855-858):
     * ```cpp
     * if (root.IsCoreOptionType() && allowOptionBox &&
     *     CountOptionNestedLevel(leaf) < CountOptionNestedLevel(root)) {
     *     // T <: Option<T> (Option 自动装箱)
     *     return IsSubtype(&leaf, root.typeArgs[0], implicitBoxed);
     * }
     * ```
     *
     * **正确的规则**:
     * 1. `T <: Option<T>` - 任何类型都是其 Option 类型的子类型
     * 2. `Option<T> <: Option<Option<T>>` - Option 可以多层嵌套
     * 3. `Option<T>` 与 `T` 之间没有其他子类型关系
     *
     * ### Nothing 类型的特殊规则
     * 根据编译器实现 (TypeManager.cpp:1013-1020):
     * ```cpp
     * (leaf->IsNothing() && !root->IsPlaceholder())  // Nothing 是所有类型的子类型
     * if (root->IsNothing()) { return false; }       // Nothing 不是其他类型的父类型
     * ```
     *
     * @param subtype 子类型
     * @param supertype 超类型
     * @return true 如果 subtype 是 supertype 的子类型
     */
    private fun isSubtypeOfForRepresentatives(subtype: CangJieType, supertype: CangJieType): Boolean {
        // 快速路径 1: Nothing 是所有类型的子类型
        if (CangJieBuiltIns.isNothing(subtype)) {
            return true
        }

        // 快速路径 2: Nothing 永远不是其他类型的父类型
        if (CangJieBuiltIns.isNothing(supertype)) {
            return false
        }

        // Option 类型的子类型关系 (两个都是 Option)
        if (subtype.isOption && supertype.isOption) {
            return isSubtypeOf(subtype.unwrapOption(), supertype.unwrapOption())
        }

        // Option 自动装箱: T <: Option<T>
        if (supertype.isOption && !subtype.isOption) {
            return isSubtypeOf(subtype, supertype.unwrapOption())
        }

        // 注意: Option<T> 不是 T 的子类型！

        // 错误类型总是兼容
        if (subtype.isError || supertype.isError) {
            return true
        }

        // TODO: 实现完整的子类型检查逻辑
        // - 类继承关系检查
        // - 接口实现检查
        // - 泛型类型参数检查
        // - 元组/函数类型检查
        return false
    }

    /**
     * 检查相同构造器类型之间的子类型关系
     *
     * 当两个类型具有相同的类型构造器时，需要检查它们的类型参数。
     * 在仓颉语言中，所有类型参数都是不变的（invariant），
     * 所以必须精确匹配。
     *
     * @param subtype 子类型
     * @param supertype 超类型
     * @return true 如果满足子类型关系
     */
    private fun checkSubtypeForTheSameConstructor(subtype: CangJieType, supertype: CangJieType): Boolean {
        val constructor = subtype.constructor

        val subArguments = subtype.arguments
        val superArguments = supertype.arguments
        if (subArguments.size != superArguments.size) return false

        val parameters  = constructor.parameters
        for (i in parameters.indices) {
            val superArgument = superArguments[i]
            val subArgument = subArguments[i]

            // 在仓颉语言中，所有类型参数都是不变的（invariant）
            // 因此必须精确匹配类型参数
            val argumentIsErrorType = subArgument.type.isError || superArgument.type.isError
            if (!argumentIsErrorType) {
                if (!constraints.assertEqualTypes(subArgument.type, superArgument.type, this)) {
                    return false
                }
            }
        }
        return true
    }

    companion object {
        /**
         * 查找对应的超类型
         *
         * 查找 subtype 的超类型中与 supertype 具有相同构造器的类型，
         * 并应用类型参数替换。
         *
         * @param subtype 子类型
         * @param supertype 超类型
         * @param typeCheckingProcedureCallbacks 类型检查回调
         * @return 对应的超类型，如果不存在则返回 null
         */
        @JvmOverloads
        fun findCorrespondingSupertype(
            subtype: CangJieType,
            supertype: CangJieType,
            typeCheckingProcedureCallbacks: TypeCheckingProcedureCallbacks = TypeCheckerProcedureCallbacksImpl()
        ): CangJieType? {
            // 如果类型构造器相同，直接返回 subtype
            if (CangJieSubtypeChecker.areEqualTypeConstructors(subtype.constructor, supertype.constructor)) {
                return subtype
            }

            // 在超类型中查找
            for (immediateSupertype in subtype.constructor.supertypes) {
                val result = findCorrespondingSupertype(immediateSupertype, supertype, typeCheckingProcedureCallbacks)
                if (result != null) {
                    return result
                }
            }

            return null
        }
    }
}
