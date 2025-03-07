/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.storage


import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentMap
/**
 * Interface for managing various types of storage and caching mechanisms within a project.
 * 它提供了创建备忘函数、惰性值和缓存的不同功能。
 */
interface StorageManager {
    /**
     * 当前存储管理器所属的项目。
     */
    val project: Project

    /**
     * 创建一个备忘函数，该函数会缓存 [compute] 函数的结果。
     *
     * @param compute 计算函数，接受一个参数并返回结果。
     * @param onRecursiveCall 递归调用时的处理函数。如果计算函数递归调用自身，此函数将被调用。
     *                       参数表示是否是第一次递归调用。
     * @return 备忘函数，返回类型为 [MemoizedFunctionToNotNull]。
     */
    fun <K : Any, V : Any> createMemoizedFunction(
        compute: (K) -> V,
        onRecursiveCall: (K, Boolean) -> V
    ): MemoizedFunctionToNotNull<K, V>

    /**
     * 创建一个可为空的惰性值。
     *
     * @param computable 计算函数，返回一个可为空的值。
     * @return 可为空的惰性值，返回类型为 [NullableLazyValue]。
     */
    fun <T : Any> createNullableLazyValue(computable: () -> T?): NullableLazyValue<T>

    /**
     * 创建一个不包含空值的缓存。
     *
     * @return 不包含空值的缓存，返回类型为 [CacheWithNotNullValues]。
     */
    fun <K, V : Any> createCacheWithNotNullValues(): CacheWithNotNullValues<K, V>

    /**
     * 创建一个容错递归的惰性值。
     *
     * @param computable 计算函数，返回一个非空值。
     * @param onRecursiveCall 递归调用时的默认值。
     * @return 容错递归的惰性值，返回类型为 [NotNullLazyValue]。
     */
    fun <T : Any> createRecursionTolerantLazyValue(computable: () -> T, onRecursiveCall: T): NotNullLazyValue<T>

    /**
     * 创建一个包含空值的缓存。
     *
     * @return 包含空值的缓存，返回类型为 [CacheWithNullableValues]。
     */
    fun <K, V : Any> createCacheWithNullableValues(): CacheWithNullableValues<K, V>

    /**
     * 创建一个容错递归的可为空惰性值。
     *
     * @param computable 计算函数，返回一个可为空的值。
     * @param onRecursiveCall 递归调用时的默认值。
     * @return 容错递归的可为空惰性值，返回类型为 [NullableLazyValue]。
     */
    fun <T : Any> createRecursionTolerantNullableLazyValue(
        computable: () -> T?,
        onRecursiveCall: T?
    ): NullableLazyValue<T>

    /**
     * 创建一个带有后处理的惰性值。
     *
     * @param computable 计算函数，返回一个非空值。
     * @param onRecursiveCall 递归调用时的处理函数。如果为 `null`，则在递归调用时抛出异常。
     * @param postCompute 计算完成后调用的后处理函数。
     * @return 带有后处理的惰性值，返回类型为 [NotNullLazyValue]。
     */
    fun <T : Any> createLazyValueWithPostCompute(
        computable: () -> T,
        onRecursiveCall: ((Boolean) -> T)?,
        postCompute: (T) -> Unit
    ): NotNullLazyValue<T>

    /**
     * 创建一个惰性值。
     *
     * @param computable 计算函数，返回一个非空值。
     * @param onRecursiveCall 递归调用时的处理函数。
     * @return 惰性值，返回类型为 [NotNullLazyValue]。
     */
    fun <T : Any> createLazyValue(computable: () -> T, onRecursiveCall: (Boolean) -> T): NotNullLazyValue<T>

    /**
     * 创建一个惰性值。
     *
     * @param computable 计算函数，返回一个非空值。
     * @return 惰性值，返回类型为 [NotNullLazyValue]。
     */
    fun <T : Any> createLazyValue(computable: () -> T): NotNullLazyValue<T>

    /**
     * 创建一个可为空值的备忘函数。
     *
     * @param compute 计算函数，返回一个可为空的值。
     * @return 备忘函数，返回类型为 [MemoizedFunctionToNullable]。
     */
    fun <K, V : Any> createMemoizedFunctionWithNullableValues(compute: (K) -> V?): MemoizedFunctionToNullable<K, V>

    /**
     * 创建一个可为空值的备忘函数，并指定缓存映射。
     *
     * @param compute 计算函数，返回一个非空值。
     * @param map 缓存映射。
     * @return 备忘函数，返回类型为 [MemoizedFunctionToNullable]。
     */
    fun <K, V : Any> createMemoizedFunctionWithNullableValues(
        compute: (K) -> V,
        map: ConcurrentMap<K, Any>
    ): MemoizedFunctionToNullable<K, V>

    /**
     * 创建一个备忘函数，并指定递归调用处理函数和缓存映射。
     *
     * @param compute 计算函数，返回一个非空值。
     * @param onRecursiveCall 递归调用时的处理函数。
     * @param map 缓存映射。
     * @return 备忘函数，返回类型为 [MemoizedFunctionToNotNull]。
     */
    fun <K, V : Any> createMemoizedFunction(
        compute: (K) -> V,
        onRecursiveCall: (K, Boolean) -> V,
        map: ConcurrentMap<K, Any>
    ): MemoizedFunctionToNotNull<K, V>

    /**
     * 创建一个备忘函数，并指定缓存映射。
     *
     * @param compute 计算函数，返回一个非空值。
     * @param map 缓存映射。
     * @return 备忘函数，返回类型为 [MemoizedFunctionToNotNull]。
     */
    fun <K, V : Any> createMemoizedFunction(
        compute: (K) -> V,
        map: ConcurrentMap<K, Any>
    ): MemoizedFunctionToNotNull<K, V>

    /**
     * 创建一个备忘函数。
     *
     * @param compute 计算函数，返回一个非空值。
     * @return 备忘函数，返回类型为 [MemoizedFunctionToNotNull]。
     */
    fun <K, V : Any> createMemoizedFunction(compute: (K) -> V): MemoizedFunctionToNotNull<K, V>

    /**
     * 执行计算函数。
     *
     * @param computable 计算函数。
     * @return 计算结果。
     */
    fun <T> compute(computable: () -> T): T
}
