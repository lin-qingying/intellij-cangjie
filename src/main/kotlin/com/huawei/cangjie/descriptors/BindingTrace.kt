package com.huawei.cangjie.descriptors

import com.huawei.cangjie.psi.CjExpression
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.utils.slicedMap.ReadOnlySlice
import com.huawei.cangjie.utils.slicedMap.WritableSlice

interface BindingTrace : DiagnosticSink {

//    fun getBindingContext(): BindingContext
    val bindingContext: BindingContext
    // slice.isCollective() must be true
    fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K>
    /**
     * Expression type should be taken from EXPRESSION_TYPE_INFO slice
     */
    fun getType(expression: CjExpression): CangJieType?

    fun <K, V> record(slice:  WritableSlice<K, V>, key: K, value: V)

    // Writes TRUE for a bool value
    fun <K> record(slice: WritableSlice<K, Boolean>, key: K)

    /**
     * Expression type should be recorded into EXPRESSION_TYPE_INFO slice
     * (either updated old or a new one)
     */
    fun recordType(expression: CjExpression, type: CangJieType?)
    operator fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V?
}
