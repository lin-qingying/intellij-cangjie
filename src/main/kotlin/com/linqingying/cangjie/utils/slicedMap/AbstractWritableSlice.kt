package com.linqingying.cangjie.utils.slicedMap


abstract class AbstractWritableSlice<K, V>(debugName: String) : KeyWithSlice<K, V, WritableSlice<K, V>>(debugName), WritableSlice<K, V> {
    override val slice: WritableSlice<K, V>
        get() = this


    override fun getKey(): KeyWithSlice<K, V, WritableSlice<K, V>> {
        return this
    }

}
