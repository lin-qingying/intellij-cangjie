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

// 注意: TypeVariableFromCallableDescriptor 在 analysis 模块中
// 在 descriptors 模块中我们不能直接依赖它
// 相关的工厂方法将在 SubstitutorBuilders.kt 中提供

/**
 * 函数式类型替换器
 *
 * 这是组合子模式类型替换系统的基础构建块。它表示从类型构造器到替换类型的纯函数映射。
 *
 * ## 设计哲学
 *
 * 传统的 OOP 替换器（DefaultTypeSubstitutor、AbstractTypeSubstitutor）存在的问题：
 * - 有状态、可变
 * - 组合困难（需要特殊的组合器类）
 * - 类型转换开销大
 * - 难以测试和理解
 * - 线程不安全
 *
 * 函数式替换器的优势：
 * - **无状态**: 纯函数，无副作用
 * - **易组合**: 使用 orElse、andThen 等组合子轻松组合
 * - **高性能**: 避免对象包装和中间转换
 * - **易测试**: 函数式接口易于模拟和验证
 * - **线程安全**: 无状态天然线程安全
 * - **简洁**: 代码更清晰、更易理解
 *
 * ## 核心概念
 *
 * SubstitutorFunction 是一个 SAM (Single Abstract Method) 接口，接收一个类型构造器，
 * 返回替换后的类型（如果无法替换则返回 null）。
 *
 * ```kotlin
 * // 简单示例
 * val substitutor: SubstitutorFunction = { constructor ->
 *     when (constructor) {
 *         TConstructor -> IntType
 *         UConstructor -> StringType
 *         else -> null
 *     }
 * }
 * ```
 *
 * ## 组合子操作
 *
 * ### orElse - 回退组合
 * ```kotlin
 * val primary: SubstitutorFunction = { constructor -> map1[constructor] }
 * val fallback: SubstitutorFunction = { constructor -> map2[constructor] }
 * val combined = primary orElse fallback
 * // 先尝试 primary，失败则尝试 fallback
 * ```
 *
 * ### andThen - 结果转换
 * ```kotlin
 * val substitutor: SubstitutorFunction = { constructor -> baseMap[constructor] }
 * val withOption = substitutor.andThen { type -> type.makeOption() }
 * // 替换后将结果转换为 Option 类型
 * ```
 *
 * ### filter - 条件过滤
 * ```kotlin
 * val allTypes: SubstitutorFunction = { constructor -> allMap[constructor] }
 * val publicOnly = allTypes.filter { constructor, type ->
 *     constructor.declarationDescriptor?.visibility == Visibilities.PUBLIC
 * }
 * // 只替换公开的类型参数
 * ```
 *
 * ## 使用示例
 *
 * ### 示例 1: 从 Map 创建
 * ```kotlin
 * val typeMap = mapOf(
 *     TConstructor to IntType,
 *     UConstructor to StringType
 * )
 * val substitutor = SubstitutorFunction.fromMap(typeMap)
 *
 * val result = substitutor(TConstructor) // IntType
 * ```
 *
 * ### 示例 2: 从类型变量创建
 * ```kotlin
 * val freshVariables: List<TypeVariableFromCallableDescriptor> = ...
 * val substitutor = SubstitutorFunction.fromFreshVariables(freshVariables)
 *
 * // 替换类型变量为其默认类型
 * val result = substitutor(typeVariable.originalTypeParameter.typeConstructor)
 * ```
 *
 * ### 示例 3: 链式组合
 * ```kotlin
 * val known = SubstitutorFunction.fromMap(knownTypes)
 * val fresh = SubstitutorFunction.fromFreshVariables(freshVariables)
 * val descriptors = SubstitutorFunction.fromDescriptors()
 *
 * val combined = known orElse fresh orElse descriptors
 * // 按优先级尝试三个替换源
 * ```
 *
 * ### 示例 4: 复杂组合
 * ```kotlin
 * val base = SubstitutorFunction.fromMap(baseMap)
 *     .filter { constructor, _ ->
 *         // 只替换非错误类型
 *         !constructor.declarationDescriptor?.defaultType?.isError ?: false
 *     }
 *     .andThen { type ->
 *         // 应用属性转换
 *         type.replaceAttributes(customAttributes)
 *     }
 *     .orElse(SubstitutorFunction.fromDescriptors())
 * ```
 *
 * ## 工厂方法
 *
 * ### 从不同数据源创建
 *
 * - `fromMap` - 从 Map<TypeConstructor, UnwrappedType> 创建
 * - `fromDescriptors` - 使用描述符的默认类型
 * - `fromFreshVariables` - 从类型变量列表创建
 * - `chain` - 将多个替换器链接成一个
 *
 * ## 性能特性
 *
 * - **零开销抽象**: SAM 接口编译为 invokedynamic，无额外对象分配
 * - **内联友好**: 简单的 lambda 可以被 JVM 内联
 * - **缓存友好**: 无状态意味着可以安全缓存
 * - **并发友好**: 可以在多线程环境中安全使用
 *
 * @see ComposableTypeSubstitutor 使用此函数的完整替换器
 * @see SubstitutionOptions 替换选项配置
 */
fun interface SubstitutorFunction {
    /**
     * 根据类型构造器查找替换类型
     *
     * @param constructor 类型构造器
     * @return 替换后的类型，如果无法替换则返回 null
     */
    operator fun invoke(constructor: TypeConstructor): UnwrappedType?

    companion object {
        /**
         * 空替换器 - 不执行任何替换
         *
         * 这是一个单例对象，可以安全地用于引用比较。
         *
         * ```kotlin
         * if (substitutor === SubstitutorFunction.EMPTY) {
         *     // 跳过替换
         * }
         * ```
         */
        @JvmField
        val EMPTY: SubstitutorFunction = SubstitutorFunction { null }

        /**
         * 从 Map 创建替换器
         *
         * 这是最常用的创建方式，适合已知的类型参数到类型的映射。
         *
         * ## 示例
         * ```kotlin
         * val map = mapOf(
         *     TConstructor to IntType,
         *     UConstructor to StringType
         * )
         * val substitutor = SubstitutorFunction.fromMap(map)
         * ```
         *
         * ## 性能
         * - Map 查找是 O(1)
         * - 适合小规模映射（< 100 个条目）
         * - 对于大规模映射，考虑使用更高效的数据结构
         *
         * @param map 类型构造器到类型的映射
         * @return 替换器函数
         */
        @JvmStatic
        fun fromMap(map: Map<TypeConstructor, UnwrappedType>): SubstitutorFunction =
            if (map.isEmpty()) {
                EMPTY
            } else {
                SubstitutorFunction { constructor -> map[constructor] }
            }

        /**
         * 从描述符的默认类型创建替换器
         *
         * 这个替换器使用类型构造器关联的描述符的默认类型作为替换结果。
         * 主要用于泛型类实例化场景。
         *
         * ## 示例
         * ```kotlin
         * val substitutor = SubstitutorFunction.fromDescriptors()
         *
         * // 对于 class Box<T>，T 的构造器会被替换为 T 的默认类型
         * val result = substitutor(TConstructor) // T 的默认类型
         * ```
         *
         * ## 适用场景
         * - 泛型类实例化
         * - LazySubstitutingClassDescriptor
         * - 需要保留原始类型参数信息的场景
         *
         * @return 替换器函数
         */
        @JvmStatic
        fun fromDescriptors(): SubstitutorFunction =
            SubstitutorFunction { constructor ->
                constructor.declarationDescriptor?.defaultType?.unwrap()
            }

        // 注意: fromFreshVariables 方法已移至 SubstitutorBuilders.kt
        // 因为它依赖 analysis 模块中的 TypeVariableFromCallableDescriptor

        /**
         * 链式组合多个替换器
         *
         * 按顺序尝试每个替换器，返回第一个非 null 结果。
         *
         * ## 示例
         * ```kotlin
         * val sub1 = SubstitutorFunction.fromMap(map1)
         * val sub2 = SubstitutorFunction.fromMap(map2)
         * val sub3 = SubstitutorFunction.fromDescriptors()
         *
         * val chained = SubstitutorFunction.chain(sub1, sub2, sub3)
         * // 依次尝试 sub1、sub2、sub3，返回第一个成功的结果
         * ```
         *
         * ## 等价于
         * ```kotlin
         * sub1 orElse sub2 orElse sub3
         * ```
         *
         * ## 性能
         * - 短路求值：找到结果后立即返回
         * - 最坏情况：遍历所有替换器
         * - 建议将最常用的替换器放在前面
         *
         * @param substitutors 替换器列表（按优先级排序）
         * @return 组合后的替换器
         */
        @JvmStatic
        fun chain(vararg substitutors: SubstitutorFunction): SubstitutorFunction {
            return when (substitutors.size) {
                0 -> EMPTY
                1 -> substitutors[0]
                else -> SubstitutorFunction { constructor ->
                    substitutors.firstNotNullOfOrNull { it(constructor) }
                }
            }
        }

        /**
         * 创建一个常量替换器（总是返回相同的类型）
         *
         * 主要用于测试和特殊场景。
         *
         * ```kotlin
         * val alwaysInt = SubstitutorFunction.constant(IntType)
         * alwaysInt(anyConstructor) // 总是返回 IntType
         * ```
         */
        @JvmStatic
        fun constant(type: UnwrappedType): SubstitutorFunction =
            SubstitutorFunction { type }

        /**
         * 创建一个条件替换器
         *
         * 只有满足条件时才进行替换。
         *
         * ```kotlin
         * val onlyPublic = SubstitutorFunction.conditional(
         *     predicate = { constructor ->
         *         constructor.declarationDescriptor?.visibility == Visibilities.PUBLIC
         *     },
         *     substitutor = SubstitutorFunction.fromMap(map)
         * )
         * ```
         */
        @JvmStatic
        fun conditional(
            predicate: (TypeConstructor) -> Boolean,
            substitutor: SubstitutorFunction
        ): SubstitutorFunction =
            SubstitutorFunction { constructor ->
                if (predicate(constructor)) substitutor(constructor) else null
            }
    }

    /**
     * 组合两个替换器（回退策略）
     *
     * 如果当前替换器返回 null，则尝试使用 other 替换器。
     *
     * ## 示例
     * ```kotlin
     * val primary = SubstitutorFunction.fromMap(primaryMap)
     * val fallback = SubstitutorFunction.fromDescriptors()
     * val combined = primary orElse fallback
     * ```
     *
     * ## 语义
     * ```kotlin
     * combined(constructor) = this(constructor) ?: other(constructor)
     * ```
     *
     * ## 结合律
     * ```kotlin
     * (a orElse b) orElse c == a orElse (b orElse c)
     * ```
     *
     * @param other 回退替换器
     * @return 组合后的替换器
     */
    infix fun orElse(other: SubstitutorFunction): SubstitutorFunction =
        if (this === EMPTY) {
            other
        } else if (other === EMPTY) {
            this
        } else {
            SubstitutorFunction { constructor ->
                this(constructor) ?: other(constructor)
            }
        }

    /**
     * 转换替换结果
     *
     * 对替换器返回的类型应用转换函数。
     *
     * ## 示例
     * ```kotlin
     * val base = SubstitutorFunction.fromMap(map)
     * val withOption = base.andThen { type ->
     *     type.makeOptionAsSpecified(true)
     * }
     * ```
     *
     * ## 语义
     * ```kotlin
     * transformed(constructor) = this(constructor)?.let(transform)
     * ```
     *
     * ## 组合律
     * ```kotlin
     * sub.andThen(f).andThen(g) == sub.andThen { g(f(it)) }
     * ```
     *
     * @param transform 类型转换函数
     * @return 转换后的替换器
     */
    fun andThen(transform: (UnwrappedType) -> UnwrappedType): SubstitutorFunction =
        if (this === EMPTY) {
            EMPTY
        } else {
            SubstitutorFunction { constructor ->
                this(constructor)?.let(transform)
            }
        }

    /**
     * 过滤替换结果
     *
     * 只有满足条件时才返回替换结果。
     *
     * ## 示例
     * ```kotlin
     * val all = SubstitutorFunction.fromMap(allMap)
     * val nonError = all.filter { constructor, type ->
     *     !type.isError
     * }
     * ```
     *
     * ## 语义
     * ```kotlin
     * filtered(constructor) =
     *     this(constructor)?.takeIf { predicate(constructor, it) }
     * ```
     *
     * @param predicate 过滤条件
     * @return 过滤后的替换器
     */
    fun filter(predicate: (TypeConstructor, UnwrappedType) -> Boolean): SubstitutorFunction =
        if (this === EMPTY) {
            EMPTY
        } else {
            SubstitutorFunction { constructor ->
                this(constructor)?.takeIf { predicate(constructor, it) }
            }
        }

    /**
     * 为替换器添加缓存
     *
     * 缓存替换结果以提高性能。适合昂贵的替换操作。
     *
     * ## 示例
     * ```kotlin
     * val expensive = SubstitutorFunction { constructor ->
     *     // 昂贵的计算
     *     complexSubstitution(constructor)
     * }
     * val cached = expensive.cached()
     * ```
     *
     * ## 注意
     * - 缓存使用 HashMap，查找是 O(1)
     * - 适合替换操作昂贵且重复的场景
     * - 不适合一次性替换或内存受限的场景
     *
     * @return 带缓存的替换器
     */
    fun cached(): SubstitutorFunction {
        if (this === EMPTY) return EMPTY

        val cache = mutableMapOf<TypeConstructor, UnwrappedType?>()
        return SubstitutorFunction { constructor ->
            cache.getOrPut(constructor) { this(constructor) }
        }
    }
}
