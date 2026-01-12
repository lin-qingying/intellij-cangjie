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

import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor

/**
 * 类型推导专用替换器工厂
 *
 * 这个文件提供与类型推导相关的替换器创建方法，依赖于 analysis 模块中的类型变量。
 *
 * ## 架构说明
 *
 * analysis 模块扩展了 descriptors 模块的基础替换器功能，添加了类型推导相关的支持：
 * - 新类型变量（Fresh Type Variables）的替换
 * - 约束求解器的替换
 * - 推导会话（Inference Session）的替换
 *
 * ## 使用场景
 *
 * ### 函数调用推导
 * ```kotlin
 * // 给定函数: fun <T> foo(x: T): List<T>
 * // 调用: foo(42)
 *
 * val freshVariables = listOf(TypeVariableFromCallableDescriptor(TParameter))
 * val substitutor = InferenceSubstitutors.forInference(freshVariables)
 *
 * // 推导后得到 T -> Int
 * val resolvedSubstitutor = InferenceSubstitutors.forInference(
 *     mapOf(TConstructor to IntType),
 *     freshVariables
 * )
 * ```
 *
 * ### 构建器推导（Builder Inference）
 * ```kotlin
 * val builderVariables = listOf(...)
 * val substitutor = InferenceSubstitutors.forInference(builderVariables)
 *     .withOptions { copy(maxRecursionDepth = 50) }
 * ```
 *
 * ## 与 TypeSubstitutors 的关系
 *
 * - `TypeSubstitutors` (descriptors 模块) - 基础替换功能，不依赖推导
 * - `InferenceSubstitutors` (analysis 模块) - 推导专用功能，依赖类型变量
 *
 * 两者可以组合使用：
 * ```kotlin
 * val knownTypes = TypeSubstitutors.create(knownMap)
 * val freshTypes = InferenceSubstitutors.forInference(freshVariables)
 * val combined = knownTypes.compose(freshTypes)
 * ```
 *
 * @see TypeSubstitutors 基础替换器工厂
 * @see TypeVariableFromCallableDescriptor 类型变量定义
 */
object InferenceSubstitutors {

    /**
     * 从类型变量列表创建推导替换器
     *
     * 为类型推导创建一个替换器，将类型参数映射到新的类型变量。
     * 这是类型推导的第一步，创建"新鲜"的类型变量用于约束收集。
     *
     * ## 工作原理
     *
     * 给定类型变量 `[TypeVariable(T), TypeVariable(U)]`，创建映射：
     * ```
     * T.typeConstructor -> TypeVariable(T).defaultType
     * U.typeConstructor -> TypeVariable(U).defaultType
     * ```
     *
     * ## 示例
     *
     * ```kotlin
     * // 给定泛型函数: fun <T> identity(x: T): T
     * val TParameter: TypeParameterDescriptor = ...
     * val freshVariable = TypeVariableFromCallableDescriptor(TParameter)
     *
     * val substitutor = InferenceSubstitutors.forInference(listOf(freshVariable))
     *
     * // 替换函数签名
     * val paramType = substitutor.safeSubstitute(TParameter.defaultType)
     * // paramType 现在是 TypeVariable(T) 而不是 T
     * ```
     *
     * ## 配置
     *
     * 使用 `SubstitutionOptions.INFERENCE` 配置：
     * - `approximateCapturedTypes = true` - 近似捕获类型
     * - `maxRecursionDepth = 50` - 较小的递归深度（推导通常不需要深度递归）
     * - `runCapturedChecks = true` - 启用捕获类型检查
     *
     * @param freshVariables 新类型变量列表（从类型参数创建）
     * @return 推导专用的替换器
     * @see TypeVariableFromCallableDescriptor 类型变量定义
     * @see SubstitutionOptions.INFERENCE 推导配置
     */
    @JvmStatic
    fun forInference(
        freshVariables: List<TypeVariableFromCallableDescriptor>
    ): ComposableTypeSubstitutor {
        if (freshVariables.isEmpty()) {
            return TypeSubstitutors.EMPTY
        }

        return ComposableTypeSubstitutor.create(
            SubstitutorFunction.fromFreshVariables(freshVariables),
            SubstitutionOptions.INFERENCE
        )
    }

    /**
     * 创建组合推导替换器（已知类型 + 新类型变量）
     *
     * 在类型推导中，我们经常需要组合两种替换：
     * 1. 已知的类型实参（外层泛型实例化）
     * 2. 新的类型变量（当前函数的类型参数）
     *
     * ## 示例
     *
     * ```kotlin
     * // 给定: class Box<T> { fun <U> map(f: (T) -> U): Box<U> }
     * // 实例: val box = Box<Int>()
     * // 调用: box.map { it.toString() }
     *
     * // T 已知为 Int
     * val knownTypes = mapOf(TConstructor to IntType)
     *
     * // U 是新的类型变量
     * val freshU = TypeVariableFromCallableDescriptor(UParameter)
     *
     * val substitutor = InferenceSubstitutors.forInference(knownTypes, listOf(freshU))
     *
     * // 替换: Box<T> -> Box<Int>
     * // 替换: (T) -> U -> (Int) -> TypeVariable(U)
     * ```
     *
     * ## 组合顺序
     *
     * 已知类型优先于新类型变量：
     * ```
     * substitutor(constructor) = knownTypes[constructor] ?: freshVariables[constructor]
     * ```
     *
     * 这确保了外层泛型实例已经具体化，只有内层类型参数是待推导的。
     *
     * @param knownTypes 已知的类型实参映射
     * @param freshVariables 新类型变量列表
     * @return 组合后的推导替换器
     */
    @JvmStatic
    fun forInference(
        knownTypes: Map<TypeConstructor, UnwrappedType>,
        freshVariables: List<TypeVariableFromCallableDescriptor>
    ): ComposableTypeSubstitutor {
        // 如果没有已知类型，直接使用新变量
        if (knownTypes.isEmpty()) {
            return forInference(freshVariables)
        }

        // 如果没有新变量，直接使用已知类型
        if (freshVariables.isEmpty()) {
            return TypeSubstitutors.create(knownTypes, SubstitutionOptions.INFERENCE)
        }

        // 组合两者：已知类型 orElse 新变量
        val knownFunction = SubstitutorFunction.fromMap(knownTypes)
        val freshFunction = SubstitutorFunction.fromFreshVariables(freshVariables)

        return ComposableTypeSubstitutor.create(
            knownFunction orElse freshFunction,
            SubstitutionOptions.INFERENCE
        )
    }

    /**
     * 创建推导替换器（DSL 配置）
     *
     * 允许自定义配置选项的推导替换器。
     *
     * ## 示例
     *
     * ```kotlin
     * val substitutor = InferenceSubstitutors.forInference(freshVariables) {
     *     approximateCapturedTypes = false  // 保留精确的捕获类型
     *     maxRecursionDepth = 100           // 增加递归深度限制
     *     throwOnError = true               // 遇到错误立即抛异常（调试用）
     * }
     * ```
     *
     * @param freshVariables 新类型变量列表
     * @param configure 配置函数
     * @return 配置后的推导替换器
     */
    @JvmStatic
    inline fun forInference(
        freshVariables: List<TypeVariableFromCallableDescriptor>,
        configure: SubstitutionOptions.() -> SubstitutionOptions
    ): ComposableTypeSubstitutor {
        if (freshVariables.isEmpty()) {
            return TypeSubstitutors.EMPTY
        }

        val options = SubstitutionOptions.INFERENCE.configure()
        return ComposableTypeSubstitutor.create(
            SubstitutorFunction.fromFreshVariables(freshVariables),
            options
        )
    }
}

/**
 * 扩展函数 - 为类型变量列表提供更流畅的 API
 */

/**
 * 将类型变量列表转换为推导替换器
 *
 * 提供更自然的调用方式。
 *
 * ## 示例
 *
 * ```kotlin
 * val freshVariables = listOf(
 *     TypeVariableFromCallableDescriptor(TParameter),
 *     TypeVariableFromCallableDescriptor(UParameter)
 * )
 *
 * // 使用扩展方法
 * val substitutor = freshVariables.toInferenceSubstitutor()
 *
 * // 等价于
 * val substitutor = InferenceSubstitutors.forInference(freshVariables)
 * ```
 */
fun List<TypeVariableFromCallableDescriptor>.toInferenceSubstitutor(): ComposableTypeSubstitutor =
    InferenceSubstitutors.forInference(this)

/**
 * 将类型变量列表转换为推导替换器（带配置）
 *
 * ## 示例
 *
 * ```kotlin
 * val substitutor = freshVariables.toInferenceSubstitutor {
 *     approximateCapturedTypes = false
 *     maxRecursionDepth = 100
 * }
 * ```
 */
inline fun List<TypeVariableFromCallableDescriptor>.toInferenceSubstitutor(
    configure: SubstitutionOptions.() -> SubstitutionOptions
): ComposableTypeSubstitutor =
    InferenceSubstitutors.forInference(this, configure)

/**
 * 从已知类型和类型变量创建推导替换器
 *
 * ## 示例
 *
 * ```kotlin
 * val knownTypes = mapOf(TConstructor to IntType)
 * val freshVariables = listOf(TypeVariableFromCallableDescriptor(UParameter))
 *
 * val substitutor = knownTypes.toInferenceSubstitutor(freshVariables)
 * ```
 */
fun Map<TypeConstructor, UnwrappedType>.toInferenceSubstitutor(
    freshVariables: List<TypeVariableFromCallableDescriptor>
): ComposableTypeSubstitutor =
    InferenceSubstitutors.forInference(this, freshVariables)

/**
 * SubstitutorFunction 扩展 - 从类型变量创建替换函数
 */

/**
 * 从新类型变量列表创建替换函数
 *
 * 这是 `SubstitutorFunction` 的扩展，用于类型推导场景。
 *
 * ## 工作原理
 *
 * 创建一个映射：类型参数的构造器 -> 类型变量的默认类型
 *
 * ```kotlin
 * val map = freshVariables.associateBy(
 *     keySelector = { it.originalTypeParameter.typeConstructor },
 *     valueTransform = { it.defaultType }
 * )
 * ```
 *
 * ## 示例
 *
 * ```kotlin
 * val freshVariables = listOf(
 *     TypeVariableFromCallableDescriptor(TParameter)
 * )
 *
 * val function = SubstitutorFunction.fromFreshVariables(freshVariables)
 *
 * val result = function(TParameter.typeConstructor)
 * // result 是 TypeVariable(T).defaultType
 * ```
 *
 * ## 性能
 *
 * - 使用 Map 查找，O(1) 时间复杂度
 * - 适合小规模类型参数列表（< 10 个）
 * - 对于大规模推导，考虑使用缓存
 *
 * @receiver SubstitutorFunction 伴生对象
 * @param freshVariables 新类型变量列表
 * @return 替换函数
 */
fun SubstitutorFunction.Companion.fromFreshVariables(
    freshVariables: List<TypeVariableFromCallableDescriptor>
): SubstitutorFunction {
    if (freshVariables.isEmpty()) {
        return EMPTY
    }

    // 构建映射: TypeConstructor -> TypeVariable.defaultType
    val map = freshVariables.associateBy(
        keySelector = { it.originalTypeParameter.typeConstructor },
        valueTransform = { it.defaultType }
    )

    return fromMap(map)
}

/**
 * 从单个类型变量创建替换函数
 *
 * 便捷方法，用于只有一个类型参数的场景。
 *
 * ## 示例
 *
 * ```kotlin
 * val freshT = TypeVariableFromCallableDescriptor(TParameter)
 * val function = SubstitutorFunction.fromFreshVariable(freshT)
 * ```
 */
fun SubstitutorFunction.Companion.fromFreshVariable(
    freshVariable: TypeVariableFromCallableDescriptor
): SubstitutorFunction {
    return fromMap(
        mapOf(freshVariable.originalTypeParameter.typeConstructor to freshVariable.defaultType)
    )
}
