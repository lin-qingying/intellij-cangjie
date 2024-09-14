package com.huawei.cangjie.utils.slicedMap

import com.google.common.collect.ImmutableMap
import com.intellij.openapi.util.Key
import org.jetbrains.annotations.TestOnly

//
//interface SlicedMap {
//    fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V?
//
//    // slice.isCollective() must return true
//    fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K>
//
//    fun <K, V> forEach(f: (WritableSlice<K, V>, Any?, Any?) -> Unit)
//
////    fun <K, V> forEach(f: (WritableSlice<K, V>, Any?, Any?) -> Unit)
//
//    companion object {
//        val DO_NOTHING: SlicedMap = object : SlicedMap {
//            override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
//                return slice.computeValue(this, key, null, true)
//            }
//
//            override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
//                return emptySet()
//            }
//
////            override fun <K, V> forEach(f: (WritableSlice<K, V>, Any?, Any?) -> Unit) {
////
////            }
//
//            override fun <K, V> forEach(f: (WritableSlice<K, V>, Any?, Any?) -> Unit) {
//
//            }
//
//
//        }
//    }
//}


//interface ReadOnlySlice<K, V> {
//    val key: KeyWithSlice<K, V, ReadOnlySlice<K, V>>
//
//    fun computeValue(map: SlicedMap?, key: K, value: V?, valueNotFound: Boolean): V?
//
//    /**
//     * @return a slice that only retrieves the value from the storage and skips any computeValue() calls
//     */
//    fun makeRawValueVersion(): ReadOnlySlice<K, V>?
//}

abstract class KeyWithSlice<K, V, out Slice : ReadOnlySlice<K, V>>(debugName: String) : Key<V>(debugName) {
    abstract val slice: Slice
}

//interface WritableSlice<K, V> : ReadOnlySlice<K, V> {
//
//    // True to put, false to skip
//    fun check(key: K, value: V?): Boolean
//
//    fun afterPut(map: MutableSlicedMap, key: K, value: V)
//
//    val rewritePolicy: RewritePolicy
//
//    // In a sliced map one can request all keys for a collective slice
//    val isCollective: Boolean
//}

//interface RewritePolicy {
//    fun <K> rewriteProcessingNeeded(key: K): Boolean
//
//    // True to put, false to skip
//    fun <K, V> processRewrite(
//        slice: WritableSlice<K, V>,
//        key: K,
//        oldValue: V,
//        newValue: V
//    ): Boolean
//
//    companion object {
//        val DO_NOTHING: RewritePolicy = object : RewritePolicy {
//            override fun <K> rewriteProcessingNeeded(key: K): Boolean {
//                return false
//            }
//
//            override fun <K, V> processRewrite(
//                slice: WritableSlice<K, V>,
//                key: K,
//                oldValue: V,
//                newValue: V
//            ): Boolean {
//                throw UnsupportedOperationException()
//            }
//        }
//    }
//}


interface MutableSlicedMap : SlicedMap {
    fun <K, V> put(slice: WritableSlice<K, V>, key: K, value: V)

    fun clear()

    fun <K> remove(key: K)
    @TestOnly
    fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V>
}


open class SetSlice<K> @JvmOverloads constructor(rewritePolicy: RewritePolicy, isCollective: Boolean = false) :
    BasicWritableSlice<K, Boolean>(rewritePolicy, isCollective) {
    companion object {
        @JvmField
        val DEFAULT = false
    }

    override fun check(key: K, value: Boolean?): Boolean {
        assert(value != null) { this.toString() + " called with null value" }
        return value != DEFAULT
    }

    override fun computeValue(map: SlicedMap?, key: K, value: Boolean?, valueNotFound: Boolean): Boolean? {
        val result = super.computeValue(map, key, value, valueNotFound)
        return result ?: DEFAULT
    }

    override fun makeRawValueVersion(): ReadOnlySlice<K, Boolean>? {
        return object : DelegatingSlice<K, Boolean>(this) {
            override fun computeValue(map: SlicedMap?, key: K, value: Boolean?, valueNotFound: Boolean): Boolean? {
                if (valueNotFound) return DEFAULT
                return value
            }


        }
    }


}
