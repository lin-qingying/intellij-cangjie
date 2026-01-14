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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.types.checker.CapturedType
import org.cangnova.cangjie.types.checker.CapturedTypeConstructor
import org.cangnova.cangjie.types.checker.isCaptured

/**
 * 类型近似边界
 *
 * 表示类型近似的上下界。在仓颉语言中，由于所有用户自定义泛型类型参数都是不变的（invariant），
 * 类型近似相对简单，主要用于处理捕获类型（Captured Types）。
 *
 * @param T 边界类型
 * @property lower 下界
 * @property upper 上界
 */
data class ApproximationBounds<out T>(
    val lower: T,
    val upper: T
)

/**
 * 如果必要，近似类型参数中的捕获类型
 *
 * ## 仓颉语言特性
 * 仓颉语言中所有用户自定义泛型类型参数都是不变的（invariant），因此：
 * - 不需要处理协变/逆变投影
 * - 不需要 `approximateContravariant` 参数
 * - 类型近似逻辑大大简化
 *
 * ## 使用场景
 * 主要用于类型替换时处理捕获类型，将捕获类型替换为其边界类型。
 *
 * @param typeArgument 待近似的类型参数
 * @return 近似后的类型参数，如果不包含捕获类型则返回原类型参数
 */
fun approximateCapturedTypesIfNecessary(
    typeArgument: TypeArgument?
): TypeArgument? {
    if (typeArgument == null) return null

    val type = typeArgument.type
    // 检查是否包含捕获类型
    if (!TypeUtils.contains(type, { it.isCaptured() })) {
        return typeArgument
    }

    // 近似捕获类型，使用上界
    val approximation = approximateCapturedTypes(type).upper
    return TypeArgumentImpl(approximation)
}

/**
 * 近似类型中的捕获类型
 *
 * ## 仓颉语言简化逻辑
 * 由于仓颉语言的类型参数都是不变的，近似过程相对简单：
 * - 捕获类型直接使用其内部类型或上界
 * - 不需要根据 variance 调整上下界
 * - 递归处理嵌套的泛型类型参数
 *
 * ## 处理逻辑
 * 1. **Flexible 类型**：递归处理上下界
 * 2. **捕获类型**：使用内部类型作为边界
 * 3. **泛型类型**：递归近似所有类型参数
 * 4. **简单类型**：直接返回自身
 *
 * @param type 待近似的类型
 * @return 近似边界（上下界）
 */
fun approximateCapturedTypes(type: CangJieType): ApproximationBounds<CangJieType> {
    // 处理 Flexible 类型
    if (type.isFlexible()) {
        val boundsForFlexibleLower = approximateCapturedTypes(type.lowerIfFlexible())
        val boundsForFlexibleUpper = approximateCapturedTypes(type.upperIfFlexible())

        return ApproximationBounds(
            CangJieTypeFactory.flexibleType(
                boundsForFlexibleLower.lower.lowerIfFlexible(),
                boundsForFlexibleUpper.lower.upperIfFlexible()
            ).inheritEnhancement(type),
            CangJieTypeFactory.flexibleType(
                boundsForFlexibleLower.upper.lowerIfFlexible(),
                boundsForFlexibleUpper.upper.upperIfFlexible()
            ).inheritEnhancement(type)
        )
    }

    // 处理捕获类型
    if (type.isCaptured()) {
        val capturedType = type as CapturedType
        val typeConstructor = capturedType.constructor

        // 获取捕获类型的内部类型
        // 优先使用 lowerType，否则使用 argument 的类型


        // 仓颉语言中类型参数不变，直接使用内部类型作为上下界
        val approximatedType = capturedType.lowerType ?: typeConstructor.argument.type

        return ApproximationBounds(approximatedType, approximatedType)
    }

    val typeConstructor = type.constructor

    // 处理没有类型参数或类型参数不匹配的情况
    if (type.arguments.isEmpty() || type.arguments.size != typeConstructor.parameters.size) {
        return ApproximationBounds(type, type)
    }

    // 递归近似所有类型参数
    val approximatedArguments = type.arguments.map { argument ->
        approximateCapturedTypes(argument.type)
    }

    // 检查是否所有下界都有效（不是 Nothing）
    val lowerBoundIsTrivial = approximatedArguments.any { bounds ->
        CangJieBuiltIns.isNothing(bounds.lower)
    }

    return ApproximationBounds(
        // 如果下界无效，使用 Nothing
        lower = if (lowerBoundIsTrivial) type.builtIns.nothingType
        else replaceTypeArguments(type, approximatedArguments.map { it.lower }),
        // 上界总是有效的
        upper = replaceTypeArguments(type, approximatedArguments.map { it.upper })
    )
}

/**
 * 替换类型的类型参数
 *
 * @param type 原始类型
 * @param newTypes 新的类型参数类型列表
 * @return 替换后的类型
 */
private fun replaceTypeArguments(type: CangJieType, newTypes: List<CangJieType>): CangJieType {
    assert(type.arguments.size == newTypes.size) {
        "Incorrect type arguments count: expected ${type.arguments.size}, got ${newTypes.size}"
    }

    // 将类型列表转换为 TypeArgument 列表
    val newArguments = newTypes.map { TypeArgumentImpl(it) }
    val unwrapped = type.unwrap()
    return if (unwrapped is SimpleType) {
        CangJieTypeFactory.simpleType(unwrapped, arguments = newArguments)
    } else {
        type  // 如果不是 SimpleType,保持原样
    }
}

/**
 * 将捕获类型替换为其投影类型
 *
 * ## 仓颉语言实现
 * 由于仓颉语言不支持类型投影（in/out variance），这个函数主要用于：
 * - 将捕获类型替换回其原始类型参数
 * - 用于类型显示和错误报告
 *
 * @param typeArgument 包含捕获类型的类型参数
 * @return 替换后的类型参数，如果没有捕获类型则返回 null
 */
fun substituteCapturedTypesWithProjections(typeArgument: TypeArgument): TypeArgument? {
    val type = typeArgument.type

    // 创建一个替换器，将捕获类型替换为其原始投影
    val substitutorFunction = SubstitutorFunction { constructor ->
        // 检查是否为捕获类型构造器
        val capturedTypeConstructor = constructor as? CapturedTypeConstructor ?: return@SubstitutorFunction null

        // 返回捕获的原始类型参数的类型
        capturedTypeConstructor.argument.type.unwrap()
    }

    val typeSubstitutor = ComposableTypeSubstitutor.create(substitutorFunction)
    val substitutedType = typeSubstitutor.safeSubstitute(type.unwrap())

    // 如果类型没有改变，说明没有捕获类型需要替换
    if (substitutedType === type.unwrap()) return null

    return TypeArgumentImpl(substitutedType)
}
