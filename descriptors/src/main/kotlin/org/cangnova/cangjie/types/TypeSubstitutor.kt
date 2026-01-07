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
import org.cangnova.cangjie.descriptors.TypeParameterDescriptor
import org.cangnova.cangjie.types.CangJieTypeFactory.flexibleType
import org.cangnova.cangjie.types.DisjointKeysUnionTypeSubstitution.Companion.create
import org.cangnova.cangjie.types.ErrorUtils.createErrorType
import org.cangnova.cangjie.types.TypeConstructorSubstitution.Companion.create
import org.cangnova.cangjie.types.TypeConstructorSubstitution.Companion.createByConstructorsMap

import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.model.TypeSubstitutorMarker
import org.cangnova.cangjie.utils.isProcessCanceledException

/**
 * 类型替换器
 *
 * 负责将类型参数替换为具体类型，是泛型类型系统的核心组件。
 *
 * ## 核心概念
 *
 * 类型替换器管理类型参数到具体类型的映射关系，并在类型表达式中应用这些替换。
 * 例如：
 * - 将 `Array<T>` 中的 `T` 替换为 `Int`，得到 `Array<Int>`
 * - 将 `Map<K, V>` 中的 `K -> String, V -> Int`，得到 `Map<String, Int>`
 *
 * ## 工作原理
 *
 * ### 替换规则
 *
 * 类型替换器持有一个 [TypeSubstitution]，它定义了类型参数到类型投影的映射：
 * ```kotlin
 * // 概念示例（简化）
 * class A<T> {
 *     val list: List<T>
 * }
 *
 * // 当我们有 A<Int> 时，替换器持有映射: T -> Int
 * val substitutor = TypeSubstitutor.create(mapOf(
 *     T.typeConstructor -> TypeProjection(Variance.INVARIANT, IntType)
 * ))
 *
 * // 应用替换: List<T> -> List<Int>
 * val substitutedType = substitutor.substitute(listTType, Variance.INVARIANT)
 * ```
 *
 * ### 递归替换
 *
 * 替换器递归处理复杂类型表达式：
 * 1. **简单类型参数**: 直接从替换映射中查找（如 `T -> Int`）
 * 2. **泛型类型**: 递归替换类型参数（如 `List<T> -> List<Int>`）
 * 3. **嵌套泛型**: 深度递归替换（如 `Map<K, List<V>> -> Map<String, List<Int>>`）
 * 4. **函数类型**: 替换参数和返回类型（如 `(T) -> T` 变成 `(Int) -> Int`）
 *
 * ### 型变处理
 *
 * 替换器正确处理协变和逆变：
 * - **协变** (`out T`): 只能出现在返回位置
 * - **逆变** (`in T`): 只能出现在参数位置
 * - **不变** (`T`): 可以出现在任何位置
 *
 * 使用 [combine] 方法合并型变，防止冲突。
 *
 * ## 与 LazySubstitutingClassDescriptor 的协作
 *
 * `TypeSubstitutor` 和 `LazySubstitutingClassDescriptor` 协同工作实现泛型：
 *
 * ```
 * 泛型类 A<T>
 *     ↓
 * 原始 ClassDescriptor（定义 T）
 *     ↓
 * TypeSubstitutor（T -> Int）
 *     ↓
 * LazySubstitutingClassDescriptor（包装原始描述符 + 替换器）
 *     ↓
 * 访问成员时，动态应用替换
 *     ↓
 * A<Int> 的成员类型（T 被替换为 Int）
 * ```
 *
 * ## 创建替换器的方式
 *
 * ### 从 CangJieType 创建
 * ```kotlin
 * // 从类型实例（如 A<Int>）创建替换器
 * val type: CangJieType = ... // A<Int>
 * val substitutor = TypeSubstitutor.create(type)
 * // 自动提取类型参数映射: T -> Int
 * ```
 *
 * ### 从映射创建
 * ```kotlin
 * // 显式指定映射
 * val substitutor = TypeSubstitutor.create(mapOf(
 *     TConstructor to TypeProjectionImpl(Variance.INVARIANT, IntType)
 * ))
 * ```
 *
 * ### 链式替换
 * ```kotlin
 * // 组合多个替换器
 * val first = TypeSubstitutor.create(mapOf(T -> A<U>))
 * val second = TypeSubstitutor.create(mapOf(U -> Int))
 * val chained = TypeSubstitutor.createChainedSubstitutor(
 *     first.substitution,
 *     second.substitution
 * )
 * // 最终效果: T -> A<Int>
 * ```
 *
 * ## 替换方法
 *
 * ### substitute - 基本替换
 * ```kotlin
 * // 替换类型投影（包含型变信息）
 * fun substitute(typeProjection: TypeProjection): TypeProjection?
 *
 * // 替换类型（指定使用位置的型变）
 * fun substitute(type: CangJieType, howThisTypeIsUsed: Variance): CangJieType?
 * ```
 *
 * ### safeSubstitute - 安全替换
 * ```kotlin
 * // 替换失败时返回错误类型而非 null
 * fun safeSubstitute(type: CangJieType, howThisTypeIsUsed: Variance): CangJieType
 * ```
 *
 * ### substituteWithoutApproximation - 不进行近似的替换
 * ```kotlin
 * // 用于内部处理，不对捕获类型进行近似
 * fun substituteWithoutApproximation(typeProjection: TypeProjection): TypeProjection?
 * ```
 *
 * ## 性能优化
 *
 * - **空替换检测**: [isEmpty] 检查避免不必要的替换操作
 * - **递归深度限制**: [MAX_RECURSION_DEPTH] 防止无限递归
 * - **懒加载**: 配合 `LazySubstitutingClassDescriptor` 延迟替换
 *
 * ## 使用示例
 *
 * ### 示例1: 简单类型参数替换
 * ```kotlin
 * // 给定: class Box<T> { val value: T }
 * val boxDescriptor: ClassDescriptor = ... // Box<T>
 * val tParam: TypeParameterDescriptor = boxDescriptor.declaredTypeParameters[0]
 *
 * // 创建替换器: T -> String
 * val substitutor = TypeSubstitutor.create(mapOf(
 *     tParam.typeConstructor to TypeProjectionImpl(Variance.INVARIANT, stringType)
 * ))
 *
 * // 应用替换
 * val boxIntType = substitutor.substitute(
 *     boxDescriptor.defaultType,
 *     Variance.INVARIANT
 * )
 * // 结果: Box<String>
 * ```
 *
 * ### 示例2: 从类型实例创建
 * ```kotlin
 * // 给定类型 List<Int>
 * val listIntType: CangJieType = ... // List<Int>
 *
 * // 创建替换器（自动提取 E -> Int）
 * val substitutor = TypeSubstitutor.create(listIntType)
 *
 * // 假设有函数 func foo<T>(list: List<T>): T
 * val fooDescriptor: FunctionDescriptor = ...
 * val tParam = fooDescriptor.typeParameters[0]
 *
 * // 如果用 List<Int> 调用 foo，T 应该被推断为 Int
 * // （这是类型推断器的工作，但它会用到类似的替换逻辑）
 * ```
 *
 * ### 示例3: 嵌套泛型替换
 * ```kotlin
 * // 给定: class Container<T> { val items: List<T> }
 * val containerDescriptor: ClassDescriptor = ...
 *
 * // 创建 Container<String>
 * val substitutor = TypeSubstitutor.create(mapOf(
 *     TConstructor to TypeProjectionImpl(Variance.INVARIANT, stringType)
 * ))
 *
 * // 获取 items 属性的类型
 * val itemsType = containerDescriptor.getMemberScope()
 *     .getProperty("items")
 *     .returnType // List<T>
 *
 * // 应用替换
 * val substitutedType = substitutor.substitute(itemsType, Variance.INVARIANT)
 * // 结果: List<String>
 * ```
 *
 * ## 错误处理
 *
 * - 替换过程中的异常被捕获为 [SubstitutionException]
 * - [safeSubstitute] 返回错误类型而非抛出异常
 * - 递归深度超限时抛出 [IllegalStateException]
 *
 * @property substitution 类型替换规则
 *
 * @see TypeSubstitution 替换规则接口
 * @see LazySubstitutingClassDescriptor 使用替换器的类描述符
 * @see TypeProjection 类型投影（类型 + 型变）
 */
class TypeSubstitutor(val substitution: TypeSubstitution) : TypeSubstitutorMarker {
    fun substituteWithoutApproximation(typeProjection: TypeProjection): TypeProjection? {
        if (isEmpty) {
            return typeProjection
        }

        return try {
            unsafeSubstitute(typeProjection, null, 0)
        } catch (e: SubstitutionException) {
            null
        }
    }

    fun substitute(typeProjection: TypeProjection): TypeProjection? {
        val substitutedTypeProjection = substituteWithoutApproximation(typeProjection)
        if (!substitution.approximateCapturedTypes() && !substitution.approximateContravariantCapturedTypes()) {
            return substitutedTypeProjection
        }
        return approximateCapturedTypesIfNecessary(
            substitutedTypeProjection, substitution.approximateContravariantCapturedTypes()
        )
    }

    fun substitute(type: CangJieType, howThisTypeIsUsed: Variance): CangJieType? {
        val projection =
            substitute(TypeProjectionImpl(howThisTypeIsUsed, substitution.prepareTopLevelType(type, howThisTypeIsUsed)))
        return projection?.type
    }


    @Throws(SubstitutionException::class)
    private fun unsafeSubstitute(
        originalProjection: TypeProjection,
        typeParameter: TypeParameterDescriptor?,
        recursionDepth: Int
    ): TypeProjection {

        assertRecursionDepth(
            recursionDepth,
            originalProjection,
            substitution
        )


        // The type is within the substitution range, i.e. T or T?
        val type: CangJieType = originalProjection.type
        if (type is TypeWithEnhancement) {
            val origin: CangJieType =
                (type as TypeWithEnhancement).origin
            val enhancement: CangJieType =
                (type as TypeWithEnhancement).enhancement

            val substitution: TypeProjection = unsafeSubstitute(
                TypeProjectionImpl(originalProjection.projectionKind, origin),
                typeParameter,
                recursionDepth + 1
            )

            val substitutedEnhancement: CangJieType? =
                substitute(enhancement, originalProjection.projectionKind)
            val resultingType: CangJieType = substitution.type.unwrap()
                .wrapEnhancement(
                    substitutedEnhancement
                )

            return TypeProjectionImpl(substitution.projectionKind, resultingType)
        }

        if (type.isDynamic() || type.unwrap() is RawType) {
            return originalProjection // todo investigate
        }

        val substituted: TypeProjection? = substitution[type]
        val replacement: TypeProjection? =
            if (substituted != null) projectedTypeForConflictedTypeWithUnsafeVariance(
                type,
                substituted,
                typeParameter,
                originalProjection
            ) else null

        val originalProjectionKind: Variance = originalProjection.projectionKind
        if (replacement == null && type.isFlexible() && !type.isCustomTypeParameter()) {
            val flexibleType: FlexibleType = type.asFlexibleType()
            val substitutedLower: TypeProjection =
                unsafeSubstitute(
                    TypeProjectionImpl(originalProjectionKind, flexibleType.lowerBound),
                    typeParameter,
                    recursionDepth + 1
                )
            val substitutedUpper: TypeProjection =
                unsafeSubstitute(
                    TypeProjectionImpl(originalProjectionKind, flexibleType.upperBound),
                    typeParameter,
                    recursionDepth + 1
                )

            val substitutedProjectionKind: Variance = substitutedLower.projectionKind
            assert(
                (substitutedProjectionKind == substitutedUpper.projectionKind) &&
                        originalProjectionKind == Variance.INVARIANT || originalProjectionKind == substitutedProjectionKind
            ) { "Unexpected substituted projection kind: $substitutedProjectionKind; original: $originalProjectionKind" }

            if (substitutedLower.type === flexibleType.lowerBound && substitutedUpper.type === flexibleType.upperBound) return originalProjection

            val substitutedFlexibleType: CangJieType = flexibleType(
                substitutedLower.type.asSimpleType(), substitutedUpper.type.asSimpleType()
            )
            return TypeProjectionImpl(substitutedProjectionKind, substitutedFlexibleType)
        }

        if (CangJieBuiltIns.isNothing(type) || type.isError) return originalProjection

        if (replacement != null) {
            val varianceConflict: VarianceConflictType =
                conflictType(
                    originalProjectionKind,
                    replacement.projectionKind
                )


            val customTypeParameter = type.getCustomTypeParameter()
            val substitutedType: CangJieType = customTypeParameter?.substitutionResult(replacement.type)
                ?: replacement.type

            // substitutionType.annotations = replacement.annotations ++ type.annotations
//            if (!type.annotations.isEmpty()) {
//                val typeAnnotations:  Annotations =
//                    TypeSubstitutor.filterOutUnsafeVariance(
//                        substitution.filterAnnotations(type.annotations)
//                    )
//                substitutedType = TypeUtilsCj.replaceAnnotations(
//                    substitutedType,
//                     CompositeAnnotations(
//                        substitutedType.annotations,
//                        typeAnnotations
//                    )
//                )
//            }

            val resultingProjectionKind: Variance =
                if (varianceConflict == VarianceConflictType.NO_CONFLICT
                ) combine(
                    originalProjectionKind,
                    replacement.projectionKind
                )
                else originalProjectionKind
            return TypeProjectionImpl(resultingProjectionKind, substitutedType)
        }

        // The type is not within the substitution range, i.e. Foo, Bar<T> etc.
        return substituteCompoundType(originalProjection, recursionDepth)

    }

    private fun replaceWithNonApproximatingSubstitution(): TypeSubstitutor {
        if (substitution !is IndexedParametersSubstitution || !substitution.approximateContravariantCapturedTypes()) return this

        return TypeSubstitutor(
            IndexedParametersSubstitution(
                substitution.parameters,
                substitution.arguments,
                false
            )
        )
    }

    private fun substituteCompoundType(
        originalProjection: TypeProjection,
        recursionDepth: Int
    ): TypeProjection {
        val type = originalProjection.type
        val projectionKind = originalProjection.projectionKind

        if (type.constructor.declarationDescriptor is TypeParameterDescriptor) {
            // substitution can't change type parameter
            // todo substitute bounds
            return originalProjection
        }

        val substitutedAbbreviation = type.getAbbreviation()?.let { abbreviation ->
            // We shouldn't approximate abbreviation at the top-level as they can't be projected: below we substitute this always as invariant
            val substitutorForAbbreviation = replaceWithNonApproximatingSubstitution()
            substitutorForAbbreviation.substitute(abbreviation, Variance.INVARIANT)
        }

        val substitutedArguments = substituteTypeArguments(
            type.constructor.parameters, type.arguments, recursionDepth
        )

        var substitutedType =
            type.replace(substitutedArguments, substitution.filterAnnotations(type.annotations))
        if (substitutedType is SimpleType && substitutedAbbreviation is SimpleType) {
            substitutedType = substitutedType.withAbbreviation(substitutedAbbreviation)
        }

        return TypeProjectionImpl(projectionKind, substitutedType)
    }

    private fun substituteTypeArguments(
        typeParameters: List<TypeParameterDescriptor>,
        typeArguments: List<TypeProjection>,
        recursionDepth: Int
    ): List<TypeProjection> {
        val substitutedArguments = ArrayList<TypeProjection>(typeParameters.size)

        var wereChanges = false

        for (i in typeParameters.indices) {
            val typeParameter = typeParameters[i]
            val typeArgument = typeArguments[i]

            var substitutedTypeArgument = unsafeSubstitute(typeArgument, typeParameter, recursionDepth + 1)

            when (conflictType(typeParameter.variance, substitutedTypeArgument.projectionKind)) {
                VarianceConflictType.NO_CONFLICT -> {
                    // if the corresponding type parameter is already co/contra-variant, there's no need for an explicit projection
                    if (typeParameter.variance != Variance.INVARIANT) {
                        substitutedTypeArgument = TypeProjectionImpl(Variance.INVARIANT, substitutedTypeArgument.type)
                    }
                }

            }

            if (substitutedTypeArgument != typeArgument) {
                wereChanges = true
            }

            substitutedArguments.add(substitutedTypeArgument)
        }

        return if (!wereChanges) typeArguments else substitutedArguments
    }

    fun safeSubstitute(type: CangJieType, howThisTypeIsUsed: Variance): CangJieType {
        if (isEmpty) {
            return type
        }

        return try {
            unsafeSubstitute(TypeProjectionImpl(howThisTypeIsUsed, type), null, 0).type
        } catch (e: SubstitutionException) {
            createErrorType(
                ErrorTypeKind.UNABLE_TO_SUBSTITUTE_TYPE,
                e.message!!
            )
        }
    }

    val isEmpty: Boolean
        get() = substitution.isEmpty()

    private class SubstitutionException(message: String?) : Exception(message)
    companion object {
        @JvmField
        val EMPTY: TypeSubstitutor = create(TypeSubstitution.EMPTY)

        @JvmStatic

        fun createChainedSubstitutor(first: TypeSubstitution, second: TypeSubstitution): TypeSubstitutor {
            return create(create(first, second))
        }

        @JvmStatic

        fun create(substitutionContext: Map<TypeConstructor, TypeProjection>): TypeSubstitutor {
            return create(createByConstructorsMap(substitutionContext))
        }


        private const val MAX_RECURSION_DEPTH: Int = 100

        private fun combine(typeParameterVariance: Variance, projectionKind: Variance): Variance {
            if (typeParameterVariance == Variance.INVARIANT) return projectionKind
            if (projectionKind == Variance.INVARIANT) return typeParameterVariance
            if (typeParameterVariance == projectionKind) return projectionKind
            throw AssertionError(
                "Variance conflict: type parameter variance '" + typeParameterVariance + "' and " +
                        "projection kind '" + projectionKind + "' cannot be combined"
            )
        }

        @JvmStatic

        fun combine(typeParameterVariance: Variance, typeProjection: TypeProjection): Variance {


            return combine(typeParameterVariance, typeProjection.projectionKind)
        }

        @JvmStatic

        fun create(context: CangJieType): TypeSubstitutor {
            return create(create(context.constructor, context.arguments))
        }

        @JvmStatic
        private fun safeToString(o: Any): String {
            try {
                return o.toString()
            } catch (e: Throwable) {
                if (e.isProcessCanceledException()) {
                    throw (e as RuntimeException)
                }
                return "[Exception while computing toString(): $e]"
            }
        }

        private fun assertRecursionDepth(
            recursionDepth: Int,
            projection: TypeProjection,
            substitution: TypeSubstitution
        ) {
            check(recursionDepth <= MAX_RECURSION_DEPTH) {
                "Recursion too deep. Most likely infinite loop while substituting " + safeToString(
                    projection
                ) + "; substitution: " + safeToString(substitution)
            }
        }

        private fun projectedTypeForConflictedTypeWithUnsafeVariance(
            originalType: CangJieType,
            substituted: TypeProjection,
            typeParameter: TypeParameterDescriptor?,
            originalProjection: TypeProjection
        ): TypeProjection {


            if (typeParameter == null) return substituted


            return substituted
        }

        private fun conflictType(
            position: Variance,
            argument: Variance
        ): VarianceConflictType {

            return VarianceConflictType.NO_CONFLICT
        }

        @JvmStatic
        fun create(substitution: TypeSubstitution): TypeSubstitutor {
            return TypeSubstitutor(substitution)
        }
    }

    private enum class VarianceConflictType {
        NO_CONFLICT,

    }
}
