package com.huawei.cangjie.resolve

import com.google.common.collect.ImmutableMap
import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.utils.slicedMap.*
import com.intellij.openapi.progress.ProgressManager
import org.jetbrains.annotations.TestOnly

class BindingTraceContext(
    val map: MutableSlicedMap,
    filter: BindingTraceFilter,
    val isValidationEnabled: Boolean
) : BindingTrace {

    companion object {
        private val VALIDATION = System.getProperty("cangjie.bindingTrace.validation").toBoolean()
        val TRACK_REWRITES = false
        val TRACK_WITH_STACK_TRACES = true
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

       override val bindingContext: BindingContext = object : CleanableBindingContext {


        override fun clear() {
            map.clear()

        }

        override fun getDiagnostics(): Diagnostics {
          return mutableDiagnostics ?: Diagnostics.EMPTY
        }




        override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
            return this@BindingTraceContext.get(slice, key)

        }

        override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
            return this@BindingTraceContext.getKeys<K, V>(slice)

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
            BindingContext.EXPRESSION_TYPE_INFO,
            expression
        )?.type
    }

    override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
        if (isValidationEnabled && value is ValidateableDescriptor && !ProgressManager.getInstance().isInNonCancelableSection) {
            (value as ValidateableDescriptor).validate()
        }
        map.put(slice, key, value)
    }

    override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
        return map.get(slice, key)

    }
}