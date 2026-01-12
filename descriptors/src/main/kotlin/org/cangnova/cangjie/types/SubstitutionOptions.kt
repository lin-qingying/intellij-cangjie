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
 * 类型替换选项配置
 *
 * 控制类型替换器的行为细节，包括捕获类型近似、注解保留、递归深度限制等。
 *
 * ## 设计理念
 *
 * 替换选项采用数据类设计，支持函数式修改：
 * ```kotlin
 * val base = SubstitutionOptions.DEFAULT
 * val custom = base.copy(
 *     approximateCapturedTypes = true,
 *     maxRecursionDepth = 50
 * )
 * ```
 *
 * ## 选项说明
 *
 * ### approximateCapturedTypes
 * 是否对捕获类型进行近似。捕获类型是型变（variance）和通配符的中间表示。
 *
 * **示例：**
 * ```kotlin
 * // Java 代码: List<? extends Number>
 * // Kotlin 捕获: List<Captured(out Number)>
 *
 * // approximateCapturedTypes = true:  近似为 List<Number>
 * // approximateCapturedTypes = false: 保持 List<Captured(out Number)>
 * ```
 *
 * **何时启用：**
 * - 类型推导（需要更通用的类型）
 * - 类型检查（避免过于具体的类型）
 *
 * **何时禁用：**
 * - 泛型实例化（需要精确的类型信息）
 * - 反射和序列化（需要保留完整类型结构）
 *
 * ### keepAnnotations
 * 是否保留类型注解（Annotations）。
 *
 * **示例：**
 * ```kotlin
 * @UnsafeVariance T  // 注解
 *
 * // keepAnnotations = true:  替换后保留 @UnsafeVariance
 * // keepAnnotations = false: 替换后丢弃注解
 * ```
 *
 * **何时启用：**
 * - 需要保留元信息（大多数场景）
 * - 代码生成和反射
 *
 * **何时禁用：**
 * - 性能关键路径（注解处理有开销）
 * - 纯内部类型计算
 *
 * ### runCapturedChecks
 * 是否运行捕获类型的完整性检查。
 *
 * **检查内容：**
 * - 捕获类型的上下界一致性
 * - 超类型替换的正确性
 * - 类型安全性验证
 *
 * **何时启用：**
 * - 用户代码的类型替换（安全第一）
 * - 调试和测试
 *
 * **何时禁用：**
 * - 编译器内部计算（假设类型正确）
 * - 性能敏感场景
 *
 * ### maxRecursionDepth
 * 最大递归深度，防止无限递归。
 *
 * **示例：**
 * ```kotlin
 * // 递归类型定义
 * class Tree<T> {
 *     val children: List<Tree<T>>
 * }
 *
 * // 替换 T -> Tree<T> 会导致无限递归
 * // maxRecursionDepth 限制递归深度，防止栈溢出
 * ```
 *
 * **推荐值：**
 * - 默认: 100（足够大部分场景）
 * - 类型推导: 50（推导通常层次较浅）
 * - 测试: 10（快速失败）
 *
 * ### throwOnError
 * 遇到错误时是否抛出异常。
 *
 * **行为：**
 * - `true`: 抛出 SubstitutionException
 * - `false`: 返回原类型或错误类型
 *
 * **何时启用：**
 * - 测试（快速失败）
 * - 需要明确错误处理的场景
 *
 * **何时禁用：**
 * - 生产代码（容错性）
 * - 探索性类型替换
 *
 * ## 预设配置
 *
 * ### DEFAULT - 默认配置
 * ```kotlin
 * SubstitutionOptions.DEFAULT
 * ```
 * - 适用场景：通用类型替换
 * - 特点：保守、安全、适合大多数场景
 *
 * ### INFERENCE - 类型推导配置
 * ```kotlin
 * SubstitutionOptions.INFERENCE
 * ```
 * - 适用场景：类型推导、约束求解
 * - 特点：启用捕获类型近似，较小的递归深度
 *
 * ### DESCRIPTOR - 描述符配置
 * ```kotlin
 * SubstitutionOptions.DESCRIPTOR
 * ```
 * - 适用场景：泛型类实例化、描述符替换
 * - 特点：禁用近似，保留完整类型信息
 *
 * ## 使用示例
 *
 * ### 示例 1: 使用预设配置
 * ```kotlin
 * val substitutor = ComposableTypeSubstitutor.create(
 *     SubstitutorFunction.fromMap(map),
 *     options = SubstitutionOptions.INFERENCE
 * )
 * ```
 *
 * ### 示例 2: 自定义配置
 * ```kotlin
 * val substitutor = ComposableTypeSubstitutor.create(
 *     SubstitutorFunction.fromMap(map),
 *     options = SubstitutionOptions(
 *         approximateCapturedTypes = true,
 *         keepAnnotations = false,  // 性能优化
 *         maxRecursionDepth = 50
 *     )
 * )
 * ```
 *
 * ### 示例 3: 基于现有配置修改
 * ```kotlin
 * val base = SubstitutionOptions.DEFAULT
 * val custom = base.copy(
 *     throwOnError = true,  // 测试时启用
 *     maxRecursionDepth = 10
 * )
 * ```
 *
 * ### 示例 4: 运行时修改
 * ```kotlin
 * val substitutor = ComposableTypeSubstitutor.create(...)
 * val withApproximation = substitutor.withOptions {
 *     copy(approximateCapturedTypes = true)
 * }
 * ```
 *
 * ## 选项合并
 *
 * 当组合多个替换器时，它们的选项会被合并：
 *
 * ```kotlin
 * val sub1 = ComposableTypeSubstitutor.create(..., options1)
 * val sub2 = ComposableTypeSubstitutor.create(..., options2)
 * val combined = sub1.compose(sub2)
 * // combined 的选项 = options1.merge(options2)
 * ```
 *
 * ### 合并规则
 * - `approximateCapturedTypes`: OR（任一启用则启用）
 * - `keepAnnotations`: AND（都启用才启用）
 * - `runCapturedChecks`: AND（都启用才启用）
 * - `maxRecursionDepth`: MIN（取较小值）
 * - `throwOnError`: OR（任一启用则启用）
 *
 * ## 性能影响
 *
 * | 选项 | 性能影响 | 建议 |
 * |------|---------|------|
 * | approximateCapturedTypes | 中等（额外计算） | 只在需要时启用 |
 * | keepAnnotations | 小（属性复制） | 默认启用 |
 * | runCapturedChecks | 大（完整验证） | 用户代码启用，内部禁用 |
 * | maxRecursionDepth | 无（仅检查） | 设置合理值 |
 * | throwOnError | 无 | 根据场景选择 |
 *
 * @property approximateCapturedTypes 是否近似捕获类型
 * @property keepAnnotations 是否保留类型注解
 * @property runCapturedChecks 是否运行捕获类型检查
 * @property maxRecursionDepth 最大递归深度
 * @property throwOnError 遇到错误时是否抛出异常
 */
data class SubstitutionOptions(
    val approximateCapturedTypes: Boolean = false,
    val keepAnnotations: Boolean = true,
    val runCapturedChecks: Boolean = true,
    val maxRecursionDepth: Int = MAX_RECURSION_DEPTH,
    val throwOnError: Boolean = false
) {
    /**
     * 合并两个选项配置
     *
     * 合并策略确保组合后的替换器行为保守且安全。
     *
     * ## 示例
     * ```kotlin
     * val options1 = SubstitutionOptions(approximateCapturedTypes = true)
     * val options2 = SubstitutionOptions(keepAnnotations = false)
     * val merged = options1.merge(options2)
     * // merged.approximateCapturedTypes = true (OR)
     * // merged.keepAnnotations = false (AND)
     * ```
     *
     * @param other 另一个选项配置
     * @return 合并后的选项
     */
    fun merge(other: SubstitutionOptions): SubstitutionOptions =
        SubstitutionOptions(
            // OR: 任一启用则启用（更宽松）
            approximateCapturedTypes = this.approximateCapturedTypes || other.approximateCapturedTypes,

            // AND: 都启用才启用（更严格）
            keepAnnotations = this.keepAnnotations && other.keepAnnotations,
            runCapturedChecks = this.runCapturedChecks && other.runCapturedChecks,

            // MIN: 取较小值（更安全）
            maxRecursionDepth = minOf(this.maxRecursionDepth, other.maxRecursionDepth),

            // OR: 任一启用则启用（更早失败）
            throwOnError = this.throwOnError || other.throwOnError
        )

    /**
     * 检查选项是否等价于默认选项
     */
    fun isDefault(): Boolean = this == DEFAULT

    /**
     * 检查是否启用了严格模式（所有检查都启用）
     */
    fun isStrict(): Boolean =
        approximateCapturedTypes && keepAnnotations && runCapturedChecks && throwOnError

    /**
     * 检查是否为性能模式（禁用大部分检查）
     */
    fun isPerformance(): Boolean =
        !approximateCapturedTypes && !keepAnnotations && !runCapturedChecks && !throwOnError

    companion object {
        /**
         * 最大递归深度常量
         *
         * 选择 100 的理由：
         * - 足够大，覆盖正常的类型嵌套
         * - 不会太大，能快速检测无限递归
         * - 经验值，来自 Kotlin 编译器
         */
        private const val MAX_RECURSION_DEPTH = 100

        /**
         * 默认配置 - 保守且安全
         *
         * 适用场景：
         * - 不确定的通用场景
         * - 用户代码的类型替换
         * - 需要完整类型信息的场景
         *
         * 特点：
         * - 不近似捕获类型（保留精确信息）
         * - 保留注解（保留元信息）
         * - 运行检查（确保安全）
         * - 标准递归深度（100）
         * - 不抛异常（容错）
         */
        @JvmField
        val DEFAULT = SubstitutionOptions()

        /**
         * 类型推导配置
         *
         * 适用场景：
         * - 类型推导系统
         * - 约束求解
         * - 泛型函数调用
         *
         * 特点：
         * - 近似捕获类型（推导需要更通用的类型）
         * - 运行检查（确保推导正确）
         * - 较小递归深度（推导通常层次较浅）
         *
         * 优化：
         * - maxRecursionDepth = 50（减少栈空间）
         * - approximateCapturedTypes = true（简化类型）
         */
        @JvmField
        val INFERENCE = SubstitutionOptions(
            approximateCapturedTypes = true,
            keepAnnotations = true,
            runCapturedChecks = true,
            maxRecursionDepth = 50,
            throwOnError = false
        )

        /**
         * 描述符配置
         *
         * 适用场景：
         * - 泛型类实例化
         * - LazySubstitutingClassDescriptor
         * - 描述符相关的类型替换
         *
         * 特点：
         * - 不近似捕获类型（保留完整结构）
         * - 保留注解（描述符需要完整信息）
         * - 不运行捕获检查（假设描述符正确）
         *
         * 优化：
         * - runCapturedChecks = false（性能优化）
         */
        @JvmField
        val DESCRIPTOR = SubstitutionOptions(
            approximateCapturedTypes = false,
            keepAnnotations = true,
            runCapturedChecks = false,
            maxRecursionDepth = MAX_RECURSION_DEPTH,
            throwOnError = false
        )

        /**
         * 测试配置 - 严格且快速失败
         *
         * 适用场景：
         * - 单元测试
         * - 集成测试
         * - 调试
         *
         * 特点：
         * - 所有检查启用
         * - 遇到错误立即抛异常
         * - 较小递归深度（快速检测问题）
         */
        @JvmField
        val TEST = SubstitutionOptions(
            approximateCapturedTypes = true,
            keepAnnotations = true,
            runCapturedChecks = true,
            maxRecursionDepth = 20,
            throwOnError = true
        )

        /**
         * 性能配置 - 禁用所有非必要功能
         *
         * 适用场景：
         * - 编译器内部计算
         * - 性能关键路径
         * - 大规模类型处理
         *
         * 特点：
         * - 禁用所有检查和近似
         * - 最小化处理开销
         * - 假设输入类型正确
         *
         * 警告：
         * - 不适合用户代码
         * - 可能产生不安全的类型
         * - 仅在确保输入正确时使用
         */
        @JvmField
        val PERFORMANCE = SubstitutionOptions(
            approximateCapturedTypes = false,
            keepAnnotations = false,
            runCapturedChecks = false,
            maxRecursionDepth = MAX_RECURSION_DEPTH,
            throwOnError = false
        )
    }
}

/**
 * DSL 扩展 - 更流畅的选项配置
 */

/**
 * 启用近似
 */
fun SubstitutionOptions.withApproximation(): SubstitutionOptions =
    copy(approximateCapturedTypes = true)

/**
 * 禁用注解
 */
fun SubstitutionOptions.withoutAnnotations(): SubstitutionOptions =
    copy(keepAnnotations = false)

/**
 * 禁用检查
 */
fun SubstitutionOptions.withoutChecks(): SubstitutionOptions =
    copy(runCapturedChecks = false)

/**
 * 设置递归深度
 */
fun SubstitutionOptions.withMaxDepth(depth: Int): SubstitutionOptions =
    copy(maxRecursionDepth = depth)

/**
 * 启用抛异常
 */
fun SubstitutionOptions.withThrowOnError(): SubstitutionOptions =
    copy(throwOnError = true)

/**
 * 组合多个配置修改
 *
 * ```kotlin
 * val options = SubstitutionOptions.DEFAULT.apply {
 *     withApproximation()
 *     withMaxDepth(50)
 *     withThrowOnError()
 * }
 * ```
 */
inline fun SubstitutionOptions.apply(block: SubstitutionOptions.() -> SubstitutionOptions): SubstitutionOptions =
    this.block()
