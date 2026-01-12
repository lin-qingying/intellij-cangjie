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

/**
 * 类型替换器构建器和便捷工厂
 *
 * 提供创建类型替换器的便捷方法，是组合子模式系统的高层 API。
 *
 * ## 架构说明
 *
 * descriptors 模块提供基础的类型替换功能，不依赖 analysis 模块。
 * 与类型推导相关的功能应该在 analysis 模块的 InferenceSubstitutors 中扩展。
 *
 * ## 基本使用
 *
 * ```kotlin
 * // 从 Map 创建
 * val substitutor = TypeSubstitutors.create(typeMap)
 *
 * // 从类型提取参数
 * val substitutor = TypeSubstitutors.create(listIntType)
 *
 * // 空替换器
 * val empty = TypeSubstitutors.EMPTY
 * ```
 *
 * ## 高级用法
 *
 * ```kotlin
 * // 描述符场景
 * val substitutor = TypeSubstitutors.forDescriptor(map)
 *
 * // 自定义配置
 * val substitutor = TypeSubstitutors.create(map, SubstitutionOptions.DESCRIPTOR)
 *
 * // 组合替换器
 * val combined = sub1.compose(sub2)
 * ```
 */
object TypeSubstitutors {

    /**
     * 空替换器（单例）
     *
     * 不执行任何替换，直接返回原类型。
     */
    @JvmField
    val EMPTY: ComposableTypeSubstitutor = ComposableTypeSubstitutor.EMPTY

    /**
     * 从 Map 创建替换器（基础方法）
     *
     * 最常用的创建方式，适合已知的类型参数映射。
     *
     * ## 示例
     * ```kotlin
     * val map = mapOf(
     *     TConstructor to IntType,
     *     UConstructor to StringType
     * )
     * val substitutor = TypeSubstitutors.create(map)
     * ```
     */
    @JvmStatic
    @JvmOverloads
    fun create(
        map: Map<TypeConstructor, UnwrappedType>,
        options: SubstitutionOptions = SubstitutionOptions.DEFAULT
    ): ComposableTypeSubstitutor {
        if (map.isEmpty()) return EMPTY
        return ComposableTypeSubstitutor.create(map, options)
    }

    /**
     * 从 Map 创建替换器（DSL 配置）
     *
     * 使用 DSL 风格配置选项。
     *
     * ## 示例
     * ```kotlin
     * val substitutor = TypeSubstitutors.create(map) {
     *     approximateCapturedTypes = true
     *     keepAnnotations = false
     *     maxRecursionDepth = 50
     * }
     * ```
     */
    @JvmStatic
    inline fun create(
        map: Map<TypeConstructor, UnwrappedType>,
        configure: SubstitutionOptions.() -> SubstitutionOptions
    ): ComposableTypeSubstitutor {
        val options = SubstitutionOptions.DEFAULT.configure()
        return create(map, options)
    }

    /**
     * 从类型创建替换器
     *
     * 自动从类型实例提取类型参数映射。
     *
     * ## 示例
     * ```kotlin
     * // 给定类型 List<Int>
     * val listIntType: CangJieType = ...
     * val substitutor = TypeSubstitutors.create(listIntType)
     * // 自动创建映射: E -> Int
     * ```
     */
    @JvmStatic
    @JvmOverloads
    fun create(
        type: CangJieType,
        options: SubstitutionOptions = SubstitutionOptions.DEFAULT
    ): ComposableTypeSubstitutor =
        ComposableTypeSubstitutor.create(type, options)

    /**
     * 从 SubstitutorFunction 创建
     *
     * 直接使用函数式替换器。
     *
     * ## 示例
     * ```kotlin
     * val function: SubstitutorFunction = { constructor ->
     *     // 自定义替换逻辑
     *     customLogic(constructor)
     * }
     * val substitutor = TypeSubstitutors.create(function)
     * ```
     */
    @JvmStatic
    @JvmOverloads
    fun create(
        function: SubstitutorFunction,
        options: SubstitutionOptions = SubstitutionOptions.DEFAULT
    ): ComposableTypeSubstitutor =
        ComposableTypeSubstitutor.create(function, options)

    /**
     * 创建描述符专用替换器
     *
     * 针对泛型类实例化和描述符操作优化。
     *
     * ## 特点
     * - 使用 SubstitutionOptions.DESCRIPTOR 配置
     * - 不近似捕获类型（保留精确信息）
     * - 禁用捕获类型检查（假设描述符正确）
     * - 适合 LazySubstitutingClassDescriptor
     *
     * ## 示例
     * ```kotlin
     * val substitutor = TypeSubstitutors.forDescriptor(map)
     * val substitutedClass = LazySubstitutingClassDescriptor(
     *     originalClass,
     *     substitutor
     * )
     * ```
     */
    @JvmStatic
    fun forDescriptor(
        map: Map<TypeConstructor, UnwrappedType>
    ): ComposableTypeSubstitutor {
        if (map.isEmpty()) return EMPTY
        return ComposableTypeSubstitutor.create(
            map,
            SubstitutionOptions.DESCRIPTOR
        )
    }

    /**
     * 从类型创建描述符替换器
     */
    @JvmStatic
    fun forDescriptor(type: CangJieType): ComposableTypeSubstitutor =
        ComposableTypeSubstitutor.create(type, SubstitutionOptions.DESCRIPTOR)

    /**
     * 创建测试专用替换器
     *
     * 严格模式，所有检查启用，快速失败。
     *
     * ## 特点
     * - 所有检查启用
     * - throwOnError = true（遇到错误立即抛异常）
     * - 较小递归深度（20，快速检测问题）
     *
     * ## 示例
     * ```kotlin
     * @Test
     * fun testSubstitution() {
     *     val substitutor = TypeSubstitutors.forTest(map)
     *     assertThrows<SubstitutionException> {
     *         substitutor.safeSubstitute(invalidType)
     *     }
     * }
     * ```
     */
    @JvmStatic
    fun forTest(
        map: Map<TypeConstructor, UnwrappedType>
    ): ComposableTypeSubstitutor =
        ComposableTypeSubstitutor.create(map, SubstitutionOptions.TEST)

    /**
     * 从 TypeSubstitution 创建（兼容旧 API）
     *
     * 为从旧的 DefaultTypeSubstitutor 迁移提供桥梁。
     *
     * ## 迁移路径
     * ```kotlin
     * // 旧代码
     * val oldSubstitutor = DefaultTypeSubstitutor.create(substitution)
     *
     * // 新代码
     * val newSubstitutor = TypeSubstitutors.fromSubstitution(substitution)
     * ```
     *
     * ## 注意
     * - 这是过渡性 API
     * - 推荐直接使用 Map 或 SubstitutorFunction
     * - 将来可能被弃用
     */
    @JvmStatic
    fun fromSubstitution(substitution: TypeSubstitution): ComposableTypeSubstitutor {
        // 空替换优化
        if (substitution.isEmpty()) return EMPTY

        return ComposableTypeSubstitutor.create(
            SubstitutorFunction { constructor ->
                val descriptor = constructor.declarationDescriptor ?: return@SubstitutorFunction null
                substitution[descriptor.defaultType]?.type?.unwrap()
            },
            SubstitutionOptions(
                approximateCapturedTypes = substitution.approximateCapturedTypes(),
                keepAnnotations = true,
                runCapturedChecks = true
            )
        )
    }

    /**
     * 链式创建（从多个数据源）
     *
     * 按优先级组合多个替换源。
     *
     * ## 示例
     * ```kotlin
     * val substitutor = TypeSubstitutors.chain(
     *     SubstitutorFunction.fromMap(knownTypes),
     *     SubstitutorFunction.fromDescriptors()
     * )
     * // 依次尝试：已知类型 -> 描述符
     * ```
     */
    @JvmStatic
    @JvmOverloads
    fun chain(
        vararg functions: SubstitutorFunction,
        options: SubstitutionOptions = SubstitutionOptions.DEFAULT
    ): ComposableTypeSubstitutor {
        if (functions.isEmpty()) return EMPTY
        return ComposableTypeSubstitutor.create(
            SubstitutorFunction.chain(*functions),
            options
        )
    }

    /**
     * 组合多个替换器
     *
     * 将多个独立的替换器组合成一个。
     *
     * ## 示例
     * ```kotlin
     * val sub1 = TypeSubstitutors.create(map1)
     * val sub2 = TypeSubstitutors.forDescriptor(map2)
     *
     * val combined = TypeSubstitutors.compose(sub1, sub2)
     * ```
     */
    @JvmStatic
    fun compose(vararg substitutors: ComposableTypeSubstitutor): ComposableTypeSubstitutor {
        if (substitutors.isEmpty()) return EMPTY
        if (substitutors.size == 1) return substitutors[0]

        var result = substitutors[0]
        for (i in 1 until substitutors.size) {
            result = result.compose(substitutors[i])
        }
        return result
    }
}

/**
 * 扩展函数 - 提供更流畅的 API
 */

/**
 * 创建描述符替换器的扩展方法
 *
 * ```kotlin
 * val type = classDescriptor.defaultType
 * val substitutor = type.toSubstitutor()
 * ```
 */
fun CangJieType.toSubstitutor(
    options: SubstitutionOptions = SubstitutionOptions.DEFAULT
): ComposableTypeSubstitutor =
    TypeSubstitutors.create(this, options)

/**
 * 创建描述符专用替换器
 */
fun CangJieType.toDescriptorSubstitutor(): ComposableTypeSubstitutor =
    TypeSubstitutors.forDescriptor(this)

/**
 * Map 的扩展方法
 */
fun Map<TypeConstructor, UnwrappedType>.toSubstitutor(
    options: SubstitutionOptions = SubstitutionOptions.DEFAULT
): ComposableTypeSubstitutor =
    TypeSubstitutors.create(this, options)

/**
 * Builder DSL
 */
@DslMarker
annotation class SubstitutorBuilderDsl

/**
 * DSL 构建器
 *
 * ```kotlin
 * val substitutor = buildSubstitutor {
 *     map(TConstructor to IntType)
 *     map(UConstructor to StringType)
 *
 *     options {
 *         approximateCapturedTypes = true
 *         maxRecursionDepth = 50
 *     }
 * }
 * ```
 */
@SubstitutorBuilderDsl
class SubstitutorBuilder {
    private val mappings = mutableMapOf<TypeConstructor, UnwrappedType>()
    private val functions = mutableListOf<SubstitutorFunction>()
    private var options = SubstitutionOptions.DEFAULT

    /**
     * 添加单个映射
     */
    fun map(pair: Pair<TypeConstructor, UnwrappedType>) {
        mappings[pair.first] = pair.second
    }

    /**
     * 添加多个映射
     */
    fun map(vararg pairs: Pair<TypeConstructor, UnwrappedType>) {
        pairs.forEach { map(it) }
    }

    /**
     * 添加 Map
     */
    fun map(map: Map<TypeConstructor, UnwrappedType>) {
        mappings.putAll(map)
    }

    /**
     * 添加替换函数
     */
    fun function(f: SubstitutorFunction) {
        functions.add(f)
    }

    /**
     * 配置选项
     */
    fun options(block: SubstitutionOptions.() -> SubstitutionOptions) {
        options = options.block()
    }

    /**
     * 构建替换器
     */
    internal fun build(): ComposableTypeSubstitutor {
        val allFunctions = mutableListOf<SubstitutorFunction>()

        // 先添加 Map
        if (mappings.isNotEmpty()) {
            allFunctions.add(SubstitutorFunction.fromMap(mappings))
        }

        // 再添加其他函数
        allFunctions.addAll(functions)

        if (allFunctions.isEmpty()) {
            return TypeSubstitutors.EMPTY
        }

        return TypeSubstitutors.chain(*allFunctions.toTypedArray(), options = options)
    }
}

/**
 * DSL 入口
 */
fun buildSubstitutor(block: SubstitutorBuilder.() -> Unit): ComposableTypeSubstitutor =
    SubstitutorBuilder().apply(block).build()

/**
 * 便捷的替换器创建
 *
 * ```kotlin
 * val substitutor = substitutor(
 *     TConstructor to IntType,
 *     UConstructor to StringType
 * )
 * ```
 */
fun substitutor(vararg mappings: Pair<TypeConstructor, UnwrappedType>): ComposableTypeSubstitutor =
    TypeSubstitutors.create(mappings.toMap())
