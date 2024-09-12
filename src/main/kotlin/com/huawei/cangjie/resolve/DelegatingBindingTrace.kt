package com.huawei.cangjie.resolve

import com.google.common.collect.ImmutableMap
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.diagnostics.Diagnostic
import com.huawei.cangjie.diagnostics.DiagnosticSink
import com.huawei.cangjie.diagnostics.Diagnostics
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.huawei.cangjie.utils.slicedMap.*
import org.jetbrains.annotations.TestOnly

open class DelegatingBindingTrace(
    private val parentContext: BindingContext,
    private val name: String,
    withParentDiagnostics: Boolean = true,
    private val filter: BindingTraceFilter = BindingTraceFilter.ACCEPT_ALL,
    allowSliceRewrite: Boolean = false,
    customSuppressCache: CangJieSuppressCache? = null,
) : BindingTrace {
    protected val map = if (BindingTraceContext.TRACK_REWRITES && !allowSliceRewrite)
        TrackingSlicedMap(BindingTraceContext.TRACK_WITH_STACK_TRACES)
    else
        SlicedMapImpl(allowSliceRewrite,name)
    override val bindingContext = MyBindingContext()
    override fun <K> record(slice: WritableSlice<K, Boolean>, key: K) {
        record(slice, key, true)

    }
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

    open fun clear() {
        map.clear()
        mutableDiagnostics?.clear()
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

    override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? =
        selfGet(slice, key) ?: parentContext.get(slice, key)





    override fun report(diagnostic: Diagnostic) {
        if (mutableDiagnostics == null) {
            return
        }
        mutableDiagnostics.report(diagnostic)
    }
    override fun toString(): String = name


    override fun wantsDiagnostics(): Boolean = mutableDiagnostics != null

}
