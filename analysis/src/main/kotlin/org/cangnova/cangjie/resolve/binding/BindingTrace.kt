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

import org.cangnova.cangjie.diagnostics.DiagnosticSink
import org.cangnova.cangjie.psi.CjExpression
import org.cangnova.cangjie.resolve.binding.slicedMap.ReadOnlySlice
import org.cangnova.cangjie.resolve.binding.slicedMap.WritableSlice
import org.cangnova.cangjie.types.CangJieType

interface BindingTrace : DiagnosticSink {


    val bindingContext: BindingContext

    fun <K : Any, V> getKeys(slice: WritableSlice<K, V>): Collection<K>

    /**
     * Expression type should be taken from EXPRESSION_TYPE_INFO slice
     */
    fun getType(expression: CjExpression): CangJieType?

    fun <K : Any, V> record(slice: WritableSlice<K, V>, key: K, value: V)

    // Writes TRUE for a bool value
    fun <K : Any> record(slice: WritableSlice<K, Boolean>, key: K)

    /**
     * Expression type should be recorded into EXPRESSION_TYPE_INFO slice
     * (either updated old or a new one)
     */
    fun recordType(expression: CjExpression, type: CangJieType?)
    operator fun <K : Any, V> get(slice: ReadOnlySlice<K, V>, key: K): V?
    val size: Int get() = 0

}
