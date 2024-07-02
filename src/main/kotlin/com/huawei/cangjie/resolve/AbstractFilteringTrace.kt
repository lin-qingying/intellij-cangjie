package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.Diagnostic
import com.huawei.cangjie.utils.slicedMap.WritableSlice


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
    abstract protected fun <K, V> shouldBeHiddenFromParent(slice: WritableSlice<K, V>, key: K): Boolean

    override fun <K, V> record(slice: WritableSlice<K, V>, key: K, value: V) {
        if (shouldBeHiddenFromParent(slice, key)) super.record(slice, key, value) else parentTrace.record(slice, key, value)
    }

    override fun report(diagnostic: Diagnostic) {
        diagnosticsCallback?.callback(diagnostic)

        parentTrace.report(diagnostic)
    }

    override fun wantsDiagnostics(): Boolean = parentTrace.wantsDiagnostics()
}