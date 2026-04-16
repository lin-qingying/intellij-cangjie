package org.cangnova.cangjie.utils

import io.vavr.Tuple2 as VavrTuple2
import io.vavr.control.Option

typealias ImmutableMultimap<K, V> = ImmutableMap<K, ImmutableSet<V>>
typealias ImmutableMap<K, V> = io.vavr.collection.Map<K, V>
typealias ImmutableHashMap<K, V> = io.vavr.collection.HashMap<K, V>
typealias ImmutableSet<E> = io.vavr.collection.Set<E>
typealias ImmutableHashSet<E> = io.vavr.collection.HashSet<E>
typealias ImmutableLinkedHashSet<E> = io.vavr.collection.LinkedHashSet<E>

operator fun <T> VavrTuple2<T, *>.component1(): T = _1()
operator fun <T> VavrTuple2<*, T>.component2(): T = _2()

fun <T> Option<T>.getOrNull(): T? = getOrElse(null as T?)

fun <K, V> ImmutableMap<K, V>.getOrNull(k: K): V? = get(k).getOrNull()
