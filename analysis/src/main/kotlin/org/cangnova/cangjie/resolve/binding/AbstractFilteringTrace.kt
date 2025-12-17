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

import org.cangnova.cangjie.diagnostics.Diagnostic
import org.cangnova.cangjie.resolve.binding.slicedMap.WritableSlice

/**
 * Trace which allows to keep some slices hidden from the parent trace.
 *
 * Compared with TemporaryBindingTrace + TraceEntryFilter, FilteringTrace doesn't
 * make extra moves for slices that should be definitely recorded into parent
 * (like storing them in the local map, later re-committing into parent's, etc.)
 */
abstract class AbstractFilteringTrace(
    private val parentTrace: BindingTrace,
    name: String
) : DelegatingBindingTrace(parentTrace.bindingContext, name, true, BindingTraceFilter.ACCEPT_ALL, false) {
    protected abstract fun <K : Any, V: Any> shouldBeHiddenFromParent(slice: WritableSlice<K, V>, key: K): Boolean

    override fun <K : Any, V : Any> record(slice: WritableSlice<K, V>, key: K, value: V) {
        if (shouldBeHiddenFromParent(slice, key)) super.record(slice, key, value) else parentTrace.record(
            slice,
            key,
            value
        )
    }

    override fun report(diagnostic: Diagnostic) {
        diagnosticsCallback?.callback(diagnostic)

        parentTrace.report(diagnostic)
    }

    override fun wantsDiagnostics(): Boolean = parentTrace.wantsDiagnostics()
}