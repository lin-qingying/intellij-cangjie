package com.linqingying.cangjie.utils.slicedMap
//
//open class DelegatingSlice<K, V>(val delegate: WritableSlice<K, V>) : WritableSlice<K, V> {
//
//
//    override fun computeValue(map: SlicedMap?, key: K, value: V?, valueNotFound: Boolean): V? {
//        return delegate.computeValue(map, key, value, valueNotFound)
//
//    }
//
//    override fun makeRawValueVersion(): ReadOnlySlice<K, V>? {
//        return delegate.makeRawValueVersion()
//
//    }
//
//    override fun getKey(): KeyWithSlice<K, V, WritableSlice<K, V>> {
//        TODO("Not yet implemented")
//    }
//
//    override fun check(key: K, value: V?): Boolean {
//        return delegate.check(key, value)
//
//    }
//
//    override fun afterPut(map: MutableSlicedMap, key: K, value: V) {
//        delegate.afterPut(map, key, value)
//
//    }
//
//    override val rewritePolicy: RewritePolicy
//        get() = delegate.rewritePolicy
//
//    override val isCollective: Boolean
//        get() = delegate.isCollective
//}
