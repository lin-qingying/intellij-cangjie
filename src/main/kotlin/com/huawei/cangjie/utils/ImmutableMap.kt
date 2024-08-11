package com.huawei.cangjie.utils

import javaslang.Tuple2
import javaslang.control.Option

typealias ImmutableMap<K, V> = javaslang.collection.Map<K, V>
typealias ImmutableHashMap<K, V> = javaslang.collection.HashMap<K, V>
typealias ImmutableSet<E> = javaslang.collection.Set<E>
typealias ImmutableHashSet<E> = javaslang.collection.HashSet<E>
typealias ImmutableLinkedHashSet<E> = javaslang.collection.LinkedHashSet<E>

operator fun <T> Tuple2<T, *>.component1(): T = _1()
operator fun <T> Tuple2<*, T>.component2(): T = _2()

fun <T> Option<T>.getOrNull(): T? = getOrElse(null as T?)
fun <K, V> ImmutableMap<K, V>.getOrNull(k: K): V? = get(k)?.getOrElse(null as V?)
