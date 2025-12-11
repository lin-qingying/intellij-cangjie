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

import com.google.common.collect.ImmutableMap
import com.intellij.util.containers.ContainerUtil
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.diagnostics.Diagnostics
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.slicedMap.ReadOnlySlice
import org.cangnova.cangjie.resolve.binding.slicedMap.WritableSlice
import org.cangnova.cangjie.types.CangJieType
import org.jetbrains.annotations.TestOnly

/**
 * 使用锁机制来管理懒解析存储的类。它包装了另一个StorageManager，并提供线程安全的操作。
 *
 * @param storageManager 被包装的存储管理器，负责实际的数据存储。
 */
class LockBasedLazyResolveStorageManager(private val storageManager: StorageManager) : StorageManager by storageManager,
    LazyResolveStorageManager {
    /**
     * 创建一个软引用保留的备忘函数。
     *
     * @param compute 计算函数，用于生成备忘录中的值。
     * @return 备忘函数，其值被软引用保留。
     */
    override fun <K : Any, V : Any> createSoftlyRetainedMemoizedFunction(compute: Function1<K, V>) =
        storageManager.createMemoizedFunction(compute, ContainerUtil.createConcurrentSoftValueMap<K, Any>())

    /**
     * 创建一个软引用保留的备忘函数，允许值为null。
     *
     * @param compute 计算函数，用于生成备忘录中的值。
     * @return 备忘函数，其值被软引用保留，允许null值。
     */
    override fun <K : Any, V : Any> createSoftlyRetainedMemoizedFunctionWithNullableValues(compute: Function1<K, V>) =
        storageManager.createMemoizedFunctionWithNullableValues(
            compute,
            ContainerUtil.createConcurrentSoftValueMap<K, Any>()
        )

    /**
     * 使用锁保护的上下文类。所有对上下文的操作都在存储管理器的计算中进行，以确保线程安全。
     *
     * @param storageManager 被包装的存储管理器。
     * @param context 实际的绑定上下文。
     */
    private class LockProtectedContext(
        private val storageManager: StorageManager,
        private val context: BindingContext
    ) : BindingContext {
        override fun getType(expression: CjExpression): CangJieType? =
            storageManager.compute { context.getType(expression) }

        override val diagnostics: Diagnostics
            get() = storageManager.compute { context.diagnostics }

        override fun <K : Any, V> get(slice: ReadOnlySlice<K, V>, key: K): V? =
            storageManager.compute { context[slice, key] }

        override fun <K : Any, V> getKeys(slice: WritableSlice<K, V>): Collection<K> =
            storageManager.compute { context.getKeys(slice) }

        override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) {
            storageManager.compute { context.addOwnDataTo(trace, commitDiagnostics) }
        }

        @TestOnly
        override fun <K : Any, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> =
            storageManager.compute { context.getSliceContents(slice) }
    }

    /**
     * 使用锁保护的追踪类。所有对追踪的操作都在存储管理器的计算中进行，以确保线程安全。
     *
     * @param storageManager 被包装的存储管理器。
     * @param trace 实际的绑定追踪。
     */
    private class LockProtectedTrace(private val storageManager: StorageManager, private val trace: BindingTrace) :
        BindingTrace {

        private val context: BindingContext = LockProtectedContext(storageManager, trace.bindingContext)

        override val bindingContext: BindingContext
            get() = context

        override val size: Int
            get() = trace.size

        override fun <K : Any, V> getKeys(slice: WritableSlice<K, V>): Collection<K> =
            storageManager.compute { trace.getKeys(slice) }

        override fun getType(expression: CjExpression): CangJieType? =
            storageManager.compute { context.getType(expression) }


        override fun <K : Any, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
            storageManager.compute { trace.record(slice, key, value) }
        }

        override fun <K : Any> record(slice: WritableSlice<K, Boolean>, key: K) {
            storageManager.compute { trace.record(slice, key) }
        }

        override fun recordType(expression: CjExpression, type: CangJieType?) {
            storageManager.compute { trace.recordType(expression, type) }
        }

        override fun <K : Any, V> get(slice: ReadOnlySlice<K, V>, key: K): V? =
            storageManager.compute { trace[slice, key] }


        override fun report(diagnostic: Diagnostic) {
            storageManager.compute { trace.report(diagnostic) }
        }


        override fun wantsDiagnostics() = trace.wantsDiagnostics()
        override fun toString(): String {
            return "Lock-protected trace of LockBasedLazyResolveStorageManager $storageManager"
        }
    }

    /**
     * 创建一个安全的追踪对象，该对象的所有操作都在存储管理器的计算中进行，以确保线程安全。
     *
     * @param originalTrace 原始的绑定追踪。
     * @return 使用锁保护的绑定追踪。
     */
    override fun createSafeTrace(originalTrace: BindingTrace): BindingTrace =
        LockProtectedTrace(storageManager, originalTrace)

}
