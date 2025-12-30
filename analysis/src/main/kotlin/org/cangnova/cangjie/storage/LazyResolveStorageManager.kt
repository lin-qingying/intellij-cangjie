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

package org.cangnova.cangjie.storage

import org.cangnova.cangjie.resolve.binding.BindingTrace

/**
 * 延迟解析存储管理器
 *
 * 提供带有软引用保留策略的缓存函数创建，以及安全的绑定追踪功能。
 * 继承自 [StorageManager]，增加了延迟解析特定的存储能力。
 */
interface LazyResolveStorageManager : StorageManager {

    /**
     * 创建一个带软引用保留的记忆化函数（返回值不为null）
     *
     * 该函数会缓存计算结果，并使用软引用策略，允许在内存压力下释放缓存。
     * 适用于计算结果不为null的场景。
     *
     * @param K 输入键的类型
     * @param V 输出值的类型（非null）
     * @param compute 计算函数，将输入键转换为输出值
     * @return 记忆化的函数，保证返回值不为null
     *
     * @sample
     * ```kotlin
     * val memoized = createSoftlyRetainedMemoizedFunction<String, Int> { key ->
     *     expensiveComputation(key)
     * }
     * val result = memoized("input") // 首次计算
     * val cached = memoized("input")  // 从缓存获取
     * ```
     */
    fun <K : Any, V : Any> createSoftlyRetainedMemoizedFunction(
        compute: (K) -> V
    ): MemoizedFunctionToNotNull<K, V>

    /**
     * 创建一个带软引用保留的记忆化函数（支持null值）
     *
     * 与 [createSoftlyRetainedMemoizedFunction] 类似，但支持null输入和null输出。
     * 适用于计算结果可能为null的场景。
     *
     * @param K 输入键的类型（可为null）
     * @param V 输出值的类型（可为null）
     * @param compute 计算函数，接受可null的键，返回可null的值
     * @return 记忆化的函数，支持null值处理
     *
     * @sample
     * ```kotlin
     * val memoized = createSoftlyRetainedMemoizedFunctionWithNullableValues<String?, Int?> { key ->
     *     key?.let { expensiveComputation(it) }
     * }
     * val result = memoized(null) // 支持null输入
     * ```
     */
    fun <K : Any, V : Any> createSoftlyRetainedMemoizedFunctionWithNullableValues(
        compute: (K) -> V
    ): MemoizedFunctionToNullable<K, V>

    /**
     * 创建一个安全的绑定追踪包装器
     *
     * 将原始的 [BindingTrace] 包装成线程安全的版本，确保在并发环境下的正确性。
     * 通常用于延迟解析过程中的类型绑定记录。
     *
     * @param originalTrace 原始的绑定追踪对象
     * @return 线程安全的绑定追踪包装器
     *
     * @sample
     * ```kotlin
     * val safeTrace = createSafeTrace(originalTrace)
     * // 在并发环境下安全使用
     * safeTrace.record(key, value)
     * ```
     */
    fun createSafeTrace(originalTrace: BindingTrace): BindingTrace
}
