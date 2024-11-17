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

package com.linqingying.cangjie.storage


import com.intellij.openapi.project.Project
import java.util.concurrent.ConcurrentMap

interface StorageManager {
    val project: Project

    fun <K : Any, V : Any> createMemoizedFunction(
        compute: (K) -> V,
        onRecursiveCall: (K, Boolean) -> V
    ): MemoizedFunctionToNotNull<K, V>

    fun <T : Any> createNullableLazyValue(computable: () -> T?): NullableLazyValue<T>

    fun <K, V : Any> createCacheWithNotNullValues(): CacheWithNotNullValues<K, V>

    fun <T : Any> createRecursionTolerantLazyValue(computable: () -> T, onRecursiveCall: T): NotNullLazyValue<T>
    fun <K, V : Any> createCacheWithNullableValues(): CacheWithNullableValues<K, V>

    fun <T : Any> createRecursionTolerantNullableLazyValue(
        computable: () -> T?,
        onRecursiveCall: T?
    ): NullableLazyValue<T>

    /**
     * @param onRecursiveCall is called if the computation calls itself recursively.
     *                        The parameter to it is {@code true} for the first call, {@code false} otherwise.
     *                        If {@code onRecursiveCall} is {@code null}, an exception will be thrown on a recursive call,
     *                        otherwise it's executed and its result is returned
     *
     * @param postCompute is called after the value is computed AND published (and some clients rely on that
     *                    behavior - notably, AbstractTypeConstructor). It means that it is up to particular implementation
     *                    to provide (or not to provide) thread-safety guarantees on writes made in postCompute -- see javadoc for
     *                    LockBasedLazyValue for details.
     */
    fun <T : Any> createLazyValueWithPostCompute(
        computable: () -> T,
        onRecursiveCall: ((Boolean) -> T)?,
        postCompute: (T) -> Unit
    ): NotNullLazyValue<T>

    fun <T : Any> createLazyValue(computable: () -> T, onRecursiveCall: (Boolean) -> T): NotNullLazyValue<T>

    fun <T : Any> createLazyValue(computable: () -> T): NotNullLazyValue<T>
    fun <K, V : Any> createMemoizedFunctionWithNullableValues(compute: (K) -> V?): MemoizedFunctionToNullable<K, V>

    fun <K, V : Any> createMemoizedFunctionWithNullableValues(
        compute: (K) -> V,
        map: ConcurrentMap<K, Any>
    ): MemoizedFunctionToNullable<K, V>

    fun <K, V : Any> createMemoizedFunction(
        compute: (K) -> V,
        onRecursiveCall: (K, Boolean) -> V,
        map: ConcurrentMap<K, Any>
    ): MemoizedFunctionToNotNull<K, V>

    fun <K, V : Any> createMemoizedFunction(
        compute: (K) -> V,
        map: ConcurrentMap<K, Any>
    ): MemoizedFunctionToNotNull<K, V>

    fun <K, V : Any> createMemoizedFunction(compute: (K) -> V): MemoizedFunctionToNotNull<K, V>

    fun <T> compute(computable: () -> T): T


}
