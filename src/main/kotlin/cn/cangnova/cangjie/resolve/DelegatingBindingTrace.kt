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
import cn.cangnova.cangjie.descriptors.BindingContextSuppressCache
import cn.cangnova.cangjie.descriptors.BindingTrace
import cn.cangnova.cangjie.descriptors.CangJieSuppressCache
import cn.cangnova.cangjie.descriptors.MutableDiagnosticsWithSuppression
import cn.cangnova.cangjie.diagnostics.Diagnostic
import cn.cangnova.cangjie.diagnostics.DiagnosticSink
import cn.cangnova.cangjie.diagnostics.Diagnostics
import cn.cangnova.cangjie.psi.CjExpression
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import cn.cangnova.cangjie.utils.slicedMap.*
import com.intellij.util.keyFMap.KeyFMap
import org.jetbrains.annotations.TestOnly

open class DelegatingBindingTrace(
    private val parentContext: BindingContext,
    private val name: String,
    withParentDiagnostics: Boolean = true,
    private val filter: BindingTraceFilter = BindingTraceFilter.ACCEPT_ALL,
    allowSliceRewrite: Boolean = false,
    customSuppressCache: CangJieSuppressCache? = null,
) : BindingTrace {

    constructor(
        parentContext: BindingContext,
        debugName: String,
        resolutionSubjectForMessage: Any?,
        filter: BindingTraceFilter = BindingTraceFilter.ACCEPT_ALL,
        allowSliceRewrite: Boolean = false
    ) : this(
        parentContext,
        AnalyzingUtils.formDebugNameForBindingTrace(debugName, resolutionSubjectForMessage),
        filter = filter,
        allowSliceRewrite = allowSliceRewrite
    )

    protected val map = if (BindingTraceContext.TRACK_REWRITES && !allowSliceRewrite)
        TrackingSlicedMap(BindingTraceContext.TRACK_WITH_STACK_TRACES)
    else
        SlicedMapImpl(allowSliceRewrite,name)
    final override val bindingContext = MyBindingContext()

    fun moveAllMyDataTo(trace: BindingTrace) {
        addOwnDataTo(trace, null, true)
        clear()
    }

    @JvmOverloads
    fun addOwnDataTo(trace: BindingTrace, filter: TraceEntryFilter? = null, commitDiagnostics: Boolean = true) {
        BindingContextUtils.addOwnDataTo(trace, filter, commitDiagnostics, map, mutableDiagnostics)
    }

    @Volatile
    protected var diagnosticsCallback: DiagnosticSink.DiagnosticsCallback? = null

    inner class MyBindingContext : BindingContext {
        override fun getDiagnostics(): Diagnostics = mutableDiagnostics ?: Diagnostics.EMPTY

        override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
            return this@DelegatingBindingTrace.get(slice, key)
        }

        override fun getType(expression: CjExpression): CangJieType? {
            return this@DelegatingBindingTrace.getType(expression)
        }

        override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
            return this@DelegatingBindingTrace.getKeys(slice)
        }

        override fun addOwnDataTo(trace: BindingTrace, commitDiagnostics: Boolean) {
            BindingContextUtils.addOwnDataTo(trace, null, commitDiagnostics, map, mutableDiagnostics)
        }

        @TestOnly
        override fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
            return ImmutableMap.copyOf(parentContext.getSliceContents(slice) + map.getSliceContents(slice))
        }
    }

    /**
     * 删除某个ReadOnlySlice
     */
    fun <K, V> removeBySlice(slice: ReadOnlySlice<K, V>, key: K) {
        map.removeBySlice(slice, key)
    }

    open fun clear() {
        map.clear()
        mutableDiagnostics?.clear()
    }

    fun clearTraceCache(){
        map.clear()

    }

    open fun <K> remove(key:K){
        map.remove(key)
    }
    override fun recordType(expression: CjExpression, type: CangJieType?) {
        var typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression)
        if (typeInfo == null) {
            typeInfo = createTypeInfo(type)
        } else {
            typeInfo = typeInfo.replaceType(type)
        }
        record(BindingContext.EXPRESSION_TYPE_INFO, expression, typeInfo)
    }

    protected fun <K, V> selfGet(slice: ReadOnlySlice<K, V>, key: K): V? {
        val value = map.get(slice, key)
        return if (slice is SetSlice<*>) {
            assert(value != null)
            if (value != SetSlice.DEFAULT) value else null
        } else value
    }

    protected val mutableDiagnostics: MutableDiagnosticsWithSuppression? =
        if (filter.ignoreDiagnostics) null
        else MutableDiagnosticsWithSuppression(
            customSuppressCache ?: BindingContextSuppressCache(bindingContext),
            if (withParentDiagnostics) parentContext.diagnostics else Diagnostics.EMPTY
        )


//    override fun getBindingContext(): BindingContext = bindingContext


    override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
        val keys = map.getKeys(slice)
        val fromParent = parentContext.getKeys(slice)
        if (keys.isEmpty()) return fromParent
        if (fromParent.isEmpty()) return keys

        return keys + fromParent
    }

    override fun getType(expression: CjExpression): CangJieType? {
        val typeInfo = get(BindingContext.EXPRESSION_TYPE_INFO, expression)
        return typeInfo?.type
    }

    override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
        map.put(slice, key, value)
    }
    override fun <K> record(slice: WritableSlice<K, Boolean>, key: K) {
        record(slice, key, true)
    }
    override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? =
        selfGet(slice, key) ?: parentContext.get(slice, key)


    override fun resetCallback() {
        diagnosticsCallback = null
        mutableDiagnostics?.resetCallback()
    }
    override fun setCallbackIfNotSet(callback: DiagnosticSink.DiagnosticsCallback): Boolean {
        val callbackIfNotSet = mutableDiagnostics?.setCallbackIfNotSet(callback) ?: false
        if (callbackIfNotSet) {
            diagnosticsCallback = callback
        }
        return callbackIfNotSet
    }


    override fun report(diagnostic: Diagnostic) {
        if (mutableDiagnostics == null) {
            return
        }
        mutableDiagnostics.report(diagnostic)
    }
    override fun toString(): String = name


    override fun wantsDiagnostics(): Boolean = mutableDiagnostics != null

}
