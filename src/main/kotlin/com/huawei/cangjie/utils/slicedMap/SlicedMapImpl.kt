//package com.huawei.cangjie.utils.slicedMap
//
//import com.google.common.collect.ArrayListMultimap
//import com.google.common.collect.ImmutableMap
//import com.google.common.collect.Multimap
//import com.intellij.openapi.util.Key
//import com.intellij.util.keyFMap.KeyFMap
//
//
//class SlicedMapImpl(val alwaysAllowRewrite: Boolean) : MutableSlicedMap {
//    private var collectiveSliceKeys: Multimap<WritableSlice<*, *>, Any>? = null
//
//    private var map: MutableMap<*, KeyFMap?>? = null
//    override fun <K, V> get(slice: ReadOnlySlice<K, V>, key: K): V? {
//        val holder = map?.get(key)
//
//        val value = holder?.get(slice.key)
//
//        return slice.computeValue(this, key, value, value == null)
//    }
//
//    override fun <K, V> getKeys(slice: WritableSlice<K, V>): Collection<K> {
//        assert(slice.isCollective) { "Keys are not collected for slice $slice" }
//
//        if (collectiveSliceKeys == null) return emptyList()
//        return (collectiveSliceKeys!![slice] as Collection<K>)
//    }
//
//    override fun <K, V> forEach(f: (WritableSlice<K, V>, Any?, Any?) -> Unit) {
//        if (map == null) return
//        map!!.forEach { (key, holder) ->
//
//            if (holder == null) return
//            for (sliceKey in holder.keys) {
//                val value = holder[sliceKey]
//                f((sliceKey as AbstractWritableSlice<K, V>).slice, sliceKey, value)
//            }
//
//        }
//    }
//
//    override fun <K, V> put(slice: WritableSlice<K, V>, key: K, value: V) {
//        if (!slice.check(key, value)) {
//            return
//        }
//
//        if (map == null) {
//            map = OpenAddressLinearProbingHashTable<Any, KeyFMap?>()
//        }
//
//        var holder = map!![key]
//        if (holder == null) {
//            holder = KeyFMap.EMPTY_MAP
//        }
//
//        val sliceKey: Key<V> = slice.key
//
//        val rewritePolicy = slice.rewritePolicy
//        if (!alwaysAllowRewrite && rewritePolicy.rewriteProcessingNeeded(key)) {
//            val oldValue = holder!!.get(sliceKey)
//            if (oldValue != null) {
//                if (!rewritePolicy.processRewrite(slice, key, oldValue, value)) {
//                    return
//                }
//            }
//        }
//
//        if (slice.isCollective) {
//            if (collectiveSliceKeys == null) {
//                collectiveSliceKeys = ArrayListMultimap.create()
//            }
//
//            collectiveSliceKeys!!.put(slice, key)
//        }
//
//        map!!.put(key, holder!!.plus<V>(sliceKey, value))
//        slice.afterPut(this, key, value)
//    }
//
//    override fun clear() {
//        TODO("Not yet implemented")
//    }
//
//    override fun <K, V> getSliceContents(slice: ReadOnlySlice<K, V>): ImmutableMap<K, V> {
//        TODO("Not yet implemented")
//    }
//}