//package com.linqingying.cangjie.utils.slicedMap
//
//open class BasicWritableSlice<K, V>(
//    val rewritePolicy: RewritePolicy,
//    val isCollective: Boolean = false
//) : AbstractWritableSlice<K, V>("<BasicWritableSlice>") {
//    override fun computeValue(map: SlicedMap?, key: K, value: V?, valueNotFound: Boolean): V? {
//        if (valueNotFound) assert(value == null)
//        return value
//    }
//
//    override fun makeRawValueVersion(): ReadOnlySlice<K, V>? {
//        return object : DelegatingSlice<K, V>(this) {
//            override fun computeValue(map: SlicedMap?, key: K, value: V?, valueNotFound: Boolean): V? {
//                if (valueNotFound) assert(value == null)
//                return value
//            }
//        }
//    }
//
//    override fun isCollective(): Boolean = isCollective
//
//    override fun getRewritePolicy(): RewritePolicy  = rewritePolicy
//
//    override fun check(key: K, value: V?): Boolean {
//
//        assert(value != null) { "$this called with null value" }
//        return true
//    }
//
//    override fun afterPut(map: MutableSlicedMap, key: K, value: V) {
//
//    }
//
//
//}
