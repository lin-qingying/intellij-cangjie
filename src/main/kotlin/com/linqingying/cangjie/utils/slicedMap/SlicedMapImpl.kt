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

//package com.linqingying.cangjie.utils.slicedMap
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
