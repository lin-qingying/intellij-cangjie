package com.linqingying.cangjie.resolve.controlFlow

import com.linqingying.cangjie.utils.ImmutableHashMap
import com.linqingying.cangjie.utils.ImmutableMap

abstract class ControlFlowInfo<S : ControlFlowInfo<S, K, D>, K : Any, D : Any>
internal constructor(
    protected val map: ImmutableMap<K, D> = ImmutableHashMap.empty()
) : ImmutableMap<K, D> by map, ReadOnlyControlFlowInfo<K, D> {
    protected abstract fun copy(newMap: ImmutableMap<K, D>): S

    override fun put(key: K, value: D): S = put(key, value, this[key].getOrElse(null as D?))

    /**
     * This overload exists just for sake of optimizations: in some cases we've just retrieved the old value,
     * so we don't need to scan through the persistent hashmap again
     */
    fun put(key: K, value: D, oldValue: D?): S {
        @Suppress("UNCHECKED_CAST")
        // Avoid a copy instance creation if new value is the same
        if (value == oldValue) return this as S
        return copy(map.put(key, value))
    }

    override fun getOrNull(key: K): D? = this[key].getOrElse(null as D?)
    override fun asMap() = this

    fun retainAll(predicate: (K) -> Boolean): S = copy(map.removeAll(map.keySet().filterNot(predicate)))

    override fun equals(other: Any?) = map == (other as? ControlFlowInfo<*, *, *>)?.map

    override fun hashCode() = map.hashCode()

    override fun toString() = map.toString()
}
interface ReadOnlyControlFlowInfo<K : Any, D : Any> {
    fun getOrNull(key: K): D?
    // Only used in tests
    fun asMap(): ImmutableMap<K, D>
}
