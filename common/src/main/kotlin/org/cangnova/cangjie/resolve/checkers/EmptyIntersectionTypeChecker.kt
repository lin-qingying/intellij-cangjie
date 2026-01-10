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

package org.cangnova.cangjie.resolve.checkers

import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.EmptyIntersectionTypeKind
import org.cangnova.cangjie.types.TypeCheckerState
import org.cangnova.cangjie.types.model.*

/**
 * 空交集类型检查器
 *
 * 用于检测多个类型的交集是否为空。当两个或多个类型没有公共子类型时，它们的交集被认为是空的。
 *
 * 例如：
 * - 两个不相关的 final 类的交集一定为空
 * - final 类与不相关接口的交集可能为空
 * - 两个不相关的类（非接口）的交集一定为空
 *
 * @see EmptyIntersectionTypeKind 空交集类型的种类
 * @see EmptyIntersectionTypeInfo 空交集类型的信息
 */
object EmptyIntersectionTypeChecker {

    /**
     * 计算类型集合的交集空性
     *
     * 检查给定的类型集合是否存在空交集，即是否存在两个类型没有公共子类型。
     * 如果存在确定为空的交集，立即返回；否则返回可能为空的交集信息。
     *
     * @param context 类型系统推导扩展上下文
     * @param types 待检查的类型集合
     * @return 空交集类型信息，如果不存在空交集则返回 null
     */
    fun computeEmptyIntersectionEmptiness(
        context: TypeSystemInferenceExtensionContext,
        types: Collection<CangJieTypeMarker>
    ): EmptyIntersectionTypeInfo? = with(context) {
        if (types.isEmpty()) return null

        @Suppress("NAME_SHADOWING")
        val types = types.toList()
        var possibleEmptyIntersectionTypeInfo: EmptyIntersectionTypeInfo? = null

        for (i in types.indices) {
            val firstType = types[i]

            if (!mayCauseEmptyIntersection(firstType)) continue

            val firstSubstitutedType by lazy { firstType.eraseContainingTypeParameters() }

            for (j in i + 1 until types.size) {
                val secondType = types[j]

                if (!mayCauseEmptyIntersection(secondType)) continue

                val secondSubstitutedType = secondType.eraseContainingTypeParameters()

                if (!mayCauseEmptyIntersection(secondSubstitutedType) && !mayCauseEmptyIntersection(firstSubstitutedType)) continue

                val typeInfo = computeByHavingCommonSubtype(firstSubstitutedType, secondSubstitutedType) ?: continue

                if (typeInfo.kind.isDefinitelyEmpty) {
                    return typeInfo
                }

                if (!typeInfo.kind.isDefinitelyEmpty) {
                    possibleEmptyIntersectionTypeInfo = typeInfo
                }
            }
        }

        return possibleEmptyIntersectionTypeInfo
    }

    /**
     * 通过检查是否有公共子类型来计算空交集类型信息
     *
     * 算法说明：
     * 1. 展开交集类型的组件（如果类型本身是交集类型）
     * 2. 对展开后的类型两两比较，检查是否存在以下情况：
     *    - 两个不相关的类（确定为空）
     *    - final 类与接口（可能为空）
     *
     * @param first 第一个类型
     * @param second 第二个类型
     * @return 空交集类型信息，如果不存在空交集则返回 null
     */
    private fun TypeSystemInferenceExtensionContext.computeByHavingCommonSubtype(
        first: CangJieTypeMarker, second: CangJieTypeMarker
    ): EmptyIntersectionTypeInfo? {
        fun extractIntersectionComponentsIfNeeded(type: CangJieTypeMarker) =
            if (type.typeConstructor() is IntersectionTypeConstructorMarker) {
                type.typeConstructor().supertypes().toList()
            } else listOf(type)

        val expandedTypes = extractIntersectionComponentsIfNeeded(first) + extractIntersectionComponentsIfNeeded(second)
        val typeCheckerState by lazy { newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true) }
        var possibleEmptyIntersectionKind: EmptyIntersectionTypeInfo? = null

        for (i in expandedTypes.indices) {
            val firstType = expandedTypes[i].withOption(false)
            val firstTypeConstructor = firstType.typeConstructor()

            if (!mayCauseEmptyIntersection(firstType))
                continue

            for (j in i + 1 until expandedTypes.size) {
                val secondType = expandedTypes[j].withOption(false)
                val secondTypeConstructor = secondType.typeConstructor()

                when {
                    !mayCauseEmptyIntersection(secondType) -> {
                    }
                    areEqualTypeConstructors(firstTypeConstructor, secondTypeConstructor) -> {
                    }
                    firstType.lowerBoundIfFlexible().isSubtypeOfIgnoringArguments(typeCheckerState, secondTypeConstructor) ||
                            secondType.lowerBoundIfFlexible().isSubtypeOfIgnoringArguments(typeCheckerState, firstTypeConstructor) -> {
                    }
                    !firstTypeConstructor.isInterface() && !secondTypeConstructor.isInterface() -> {
                        // Two classes can't have a common subtype if neither is a subtype of another
                        return EmptyIntersectionTypeInfo(EmptyIntersectionTypeKind.MULTIPLE_CLASSES, firstType, secondType)
                    }
                    firstTypeConstructor.isFinalClassConstructor() || secondTypeConstructor.isFinalClassConstructor() -> {
                        // don't have incompatible supertypes so can have a common subtype only if all types are interfaces
                        possibleEmptyIntersectionKind = EmptyIntersectionTypeInfo(
                            EmptyIntersectionTypeKind.FINAL_CLASS_AND_INTERFACE,
                            firstType, secondType
                        )
                    }
                }
            }
        }

        return possibleEmptyIntersectionKind
    }

    /**
     * 检查当前类型是否是另一个类型构造器的子类型（忽略类型参数）
     *
     * 例如：`List<String>` 是 `Collection` 的子类型（忽略泛型参数）
     *
     * @param typeCheckerState 类型检查器状态
     * @param otherConstructorMarker 目标类型构造器
     * @return 如果当前类型是目标类型构造器的子类型则返回 true
     */
    private fun SimpleTypeMarker.isSubtypeOfIgnoringArguments(
        typeCheckerState: TypeCheckerState,
        otherConstructorMarker: TypeConstructorMarker
    ): Boolean = AbstractTypeChecker.findCorrespondingSupertypes(
        typeCheckerState, this, otherConstructorMarker
    ).isNotEmpty()

    /**
     * 判断类型是否可能导致空交集
     *
     * 以下类型不会导致空交集：
     * - Stub 类型或错误类型
     * - Any 类型或 Nothing 类型
     * - 非类类型构造器和非类型参数构造器
     *
     * 注意：即使是两个接口也可能形成空交集，例如：
     * ```
     * interface Inv<K>
     * interface B : Inv<Int>
     * ```
     * `Inv<String> & B` 或 `Inv<String> & Inv<Int>` 都是空交集
     *
     * @param type 待检查的类型
     * @return 如果类型可能导致空交集则返回 true
     */
    private fun TypeSystemInferenceExtensionContext.mayCauseEmptyIntersection(type: CangJieTypeMarker): Boolean {
        if (type.lowerBoundIfFlexible().isStubType() || type.isError()) {
            return false
        }

        val typeConstructor = type.typeConstructor()

        if (!typeConstructor.isClassTypeConstructor() && !typeConstructor.isTypeParameterTypeConstructor())
            return false

        // Even two interfaces may be an empty intersection type:
        // interface Inv<K>
        // interface B : Inv<Int>
        // `Inv<String> & B` or `Inv<String> & Inv<Int>` are empty
        // So we don't filter out interfaces here
        return !typeConstructor.isAnyConstructor() && !typeConstructor.isNothingConstructor()
    }

}
