package com.linqingying.cangjie.utils

abstract class AbstractArrayMapOwner<K : Any, V : Any> : Iterable<V>{
    protected abstract val arrayMap: ArrayMap<V>
    abstract class AbstractArrayMapAccessor<K : Any, V : Any, T : V>(
        protected val id: Int
    ) {
        protected fun extractValue(thisRef: AbstractArrayMapOwner<K, V>): T? {
            @Suppress("UNCHECKED_CAST")
            return thisRef.arrayMap[id] as T?
        }
    }

    final override fun iterator(): Iterator<V> = arrayMap.iterator()
}
