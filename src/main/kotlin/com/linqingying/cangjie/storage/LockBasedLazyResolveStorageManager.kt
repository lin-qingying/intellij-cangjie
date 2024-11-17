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

import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.diagnostics.Diagnostic
import com.linqingying.cangjie.diagnostics.Diagnostics
import com.linqingying.cangjie.psi.CjExpression
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.utils.slicedMap.ReadOnlySlice
import com.linqingying.cangjie.utils.slicedMap.WritableSlice
import com.intellij.util.containers.ContainerUtil
import org.jetbrains.annotations.TestOnly


class LockBasedLazyResolveStorageManager(private val storageManager: StorageManager) : StorageManager by storageManager,
    LazyResolveStorageManager {
    override fun <K : Any, V : Any> createSoftlyRetainedMemoizedFunction(compute: Function1<K, V>) =
        storageManager.createMemoizedFunction<K, V>(compute, ContainerUtil.createConcurrentSoftValueMap<K, Any>())

    override fun <K : Any, V : Any> createSoftlyRetainedMemoizedFunctionWithNullableValues(compute: Function1<K, V>) =
        storageManager.createMemoizedFunctionWithNullableValues<K, V>(
            compute,
            ContainerUtil.createConcurrentSoftValueMap<K, Any>()
        )


    private class LockProtectedContext(
        private val storageManager: StorageManager,
        private val context: BindingContext
    ) : BindingContext {
        override fun getType(expression: CjExpression): CangJieType? =
            storageManager.compute { context.getType(expression) }

        override fun getDiagnostics(): Diagnostics = storageManager.compute { context.diagnostics }

        override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K) =
            storageManager.compute { context.get<K, V>(slice, key) }

        override fun <K, V> getKeys(slice: WritableSlice<K, V>) =
            storageManager.compute { context.getKeys<K, V>(slice) }

        override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) {
            storageManager.compute { context.addOwnDataTo(trace, commitDiagnostics) }
        }

        @TestOnly
        override fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>) =
            storageManager.compute { context.getSliceContents<K, V>(slice) }
    }

    private class LockProtectedTrace(private val storageManager: StorageManager, private val trace: BindingTrace) :
        BindingTrace {

        private val context: BindingContext = LockProtectedContext(storageManager, trace.bindingContext)

        override val bindingContext: BindingContext
            get() = context

        override val size: Int
            get() = trace.size
        override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> =
            storageManager.compute { trace.getKeys<K, V>(slice) }

        override fun getType(expression: CjExpression): CangJieType? =
            storageManager.compute { context.getType(expression) }


        override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
            storageManager.compute { trace.record<K, V>(slice, key, value) }
        }

        override fun <K> record(slice: WritableSlice<K, Boolean>, key: K) {
            storageManager.compute { trace.record<K>(slice, key) }
        }

        override fun recordType(expression:CjExpression, type: CangJieType?) {
            storageManager.compute { trace.recordType(expression, type) }
        }

        override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? =
            storageManager.compute { trace.get<K, V>(slice, key) }


        override fun report(diagnostic: Diagnostic) {
            storageManager.compute { trace.report(diagnostic) }
        }


        override fun wantsDiagnostics() = trace.wantsDiagnostics()
        override fun toString(): String {
            return "Lock-protected trace of LockBasedLazyResolveStorageManager $storageManager"
        }
    }

    override fun createSafeTrace(originalTrace: BindingTrace): BindingTrace =
        LockProtectedTrace(storageManager, originalTrace)

}
