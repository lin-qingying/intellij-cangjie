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
package org.cangnova.cangjie.resolve.binding

import com.intellij.util.SmartFMap
import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.resolve.binding.slicedMap.*
import org.cangnova.cangjie.types.CangJieType

class ObservableBindingTrace(private val originalTrace: BindingTrace) : BindingTrace {
    override fun recordType(expression: CjExpression, type: CangJieType?) {
        originalTrace.recordType(expression, type)
    }

    fun interface RecordHandler<K : Any, V: Any> {
        fun handleRecord(slice: WritableSlice<K, V>, key: K, value: V?)
    }


    private var handlers: SmartFMap<WritableSlice<*, *>, RecordHandler<*, *>?> =
        SmartFMap.emptyMap<WritableSlice<*, *>, RecordHandler<*, *>?>()

    override val bindingContext: BindingContext
        get() = originalTrace.bindingContext

    fun <K : Any, V: Any> addHandler(slice: WritableSlice<K, V>, handler: RecordHandler<K, V>): ObservableBindingTrace {
        handlers = handlers.plus(slice, handler)
        return this
    }

    override fun <K : Any, V : Any> getKeys(slice: WritableSlice<K, V>): Collection<K> {
        return originalTrace.getKeys(slice)
    }


    override fun getType(expression: CjExpression): CangJieType? {
        return originalTrace.getType(expression)
    }

    override fun <K : Any, V : Any> record(slice: WritableSlice<K, V>, key: K, value: V) {
        originalTrace.record(slice, key, value)
        @Suppress("UNCHECKED_CAST")
        val recordHandler = handlers.get(slice) as? RecordHandler<K, V>
        recordHandler?.handleRecord(slice, key, value)
    }

    override fun <K : Any> record(slice: WritableSlice<K, Boolean>, key: K) {
        record(slice, key, true)
    }

    override fun <K : Any, V : Any> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        return originalTrace[slice, key]
    }

    override fun report(diagnostic: Diagnostic) {
        originalTrace.report(diagnostic)
    }

    override fun toString(): String {
        return "ObservableTrace over $originalTrace"
    }

    override fun wantsDiagnostics(): Boolean {
        return originalTrace.wantsDiagnostics()
    }
}
