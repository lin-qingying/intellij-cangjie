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

package cn.cangnova.cangjie.resolve

import com.google.common.collect.ImmutableMap
import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.diagnostics.Diagnostic
import cn.cangnova.cangjie.diagnostics.Diagnostics
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.resolve.BindingContext.EXPRESSION_TYPE_INFO
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import cn.cangnova.cangjie.utils.exceptions.CangJieTypeInfo
import cn.cangnova.cangjie.utils.slicedMap.*
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.annotations.TestOnly

class BindingTraceContext(
    val map: MutableSlicedMap,
    filter: BindingTraceFilter,
    private val isValidationEnabled: Boolean
) : BindingTrace {

    companion object {
        private val VALIDATION = System.getProperty("cangjie.bindingTrace.validation").toBoolean()
        const val TRACK_REWRITES = false
        const val TRACK_WITH_STACK_TRACES = true
    }


    constructor() : this(false)

    constructor(allowSliceRewrite: Boolean) : this(BindingTraceFilter.ACCEPT_ALL, allowSliceRewrite)
    constructor(filter: BindingTraceFilter, allowSliceRewrite: Boolean) : this(
        filter,
        allowSliceRewrite,
        VALIDATION
    )

    constructor(filter: BindingTraceFilter, allowSliceRewrite: Boolean, isValidationEnabled: Boolean) : this(

        if (TRACK_REWRITES && !allowSliceRewrite) {
            TrackingSlicedMap(TRACK_WITH_STACK_TRACES)
        } else {
            SlicedMapImpl(allowSliceRewrite)
        },
        filter,
        isValidationEnabled

    )

    override val size: Int
        get() = map.size
    override val bindingContext: BindingContext = object : CleanableBindingContext {


        override fun clear() {
            map.clear()

        }

        override fun getDiagnostics(): Diagnostics {
            return mutableDiagnostics ?: Diagnostics.EMPTY
        }


        override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
            return this@BindingTraceContext[slice, key]

        }

        override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
            return this@BindingTraceContext.getKeys(slice)

        }

        override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) {
            mutableDiagnostics?.let {
                BindingContextUtils.addOwnDataTo(
                    trace,
                    null,
                    commitDiagnostics,
                    map,
                    it
                )
            }

        }

        @TestOnly
        override fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
            return map.getSliceContents(slice)
        }

        override fun getType(expression: CjExpression): CangJieType? {
            return this@BindingTraceContext.getType(expression)

        }

    }
    private val mutableDiagnostics: MutableDiagnosticsWithSuppression? = if (filter.ignoreDiagnostics) {
        null
    } else {
        MutableDiagnosticsWithSuppression(
            BindingContextSuppressCache(
                bindingContext
            ), Diagnostics.EMPTY
        )
    }


    override fun report(diagnostic: Diagnostic) {
        if (mutableDiagnostics == null) {
            return
        }
        mutableDiagnostics.report(diagnostic)
    }

    override fun wantsDiagnostics(): Boolean {
        return mutableDiagnostics != null

    }

//    override fun getBindingContext(): BindingContext  = bindingContext

    override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
        return map.getKeys(slice)
    }

    override fun getType(expression: CjExpression): CangJieType? {
        return get(
            EXPRESSION_TYPE_INFO,
            expression
        )?.type
    }

    override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
        if (isValidationEnabled && value is ValidateableDescriptor && !ProgressManager.getInstance().isInNonCancelableSection) {
            (value as ValidateableDescriptor).validate()
        }
        map.put(slice, key, value)
    }

    override fun <K> record(slice: WritableSlice<K, Boolean>, key: K) {
        record(slice, key, true)

    }

    override fun recordType(expression: CjExpression, type: CangJieType?) {
        var typeInfo =
            get<CjExpression, CangJieTypeInfo>(
                EXPRESSION_TYPE_INFO,
                expression
            )
        typeInfo = typeInfo?.replaceType(type) ?: createTypeInfo(type)
        record<CjExpression, CangJieTypeInfo?>(
            EXPRESSION_TYPE_INFO,
            expression,
            typeInfo
        )
    }

    override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        return map.get(slice, key)

    }


}
