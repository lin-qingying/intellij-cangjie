/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.utils

import com.intellij.util.SmartList
import java.util.*
import kotlin.reflect.KClass
import kotlin.reflect.cast

inline fun <K, V> MutableMap<K, V>.getOrPutNullable(key: K, defaultValue: () -> V): V {
    return if (!containsKey(key)) {
        val answer = defaultValue()
        put(key, answer)
        answer
    } else {
        @Suppress("UNCHECKED_CAST")
        get(key) as V
    }
}

fun <E> newHashSetWithExpectedSize(expectedSize: Int): HashSet<E> =
    HashSet(capacity(expectedSize))

private fun capacity(expectedSize: Int): Int =
    if (expectedSize < 3) 3 else expectedSize + expectedSize / 3 + 1

fun <E> newLinkedHashSetWithExpectedSize(expectedSize: Int): LinkedHashSet<E> =
    LinkedHashSet(capacity(expectedSize))

@Suppress("UNCHECKED_CAST")
inline fun <T> buildList(builder: (CollectionBuilder<T>).() -> Unit): List<T> =
    buildCollection(mutableListOf(), builder) as List<T>

@Suppress("UNCHECKED_CAST")
inline fun <T> buildSet(builder: (CollectionBuilder<T>).() -> Unit): Set<T> =
    buildCollection(mutableSetOf(), builder) as Set<T>

fun <K, V> newLinkedHashMapWithExpectedSize(expectedSize: Int): LinkedHashMap<K, V> =
    LinkedHashMap(capacity(expectedSize))

inline fun <T> buildCollection(
    result: MutableCollection<T>,
    builder: (CollectionBuilder<T>).() -> Unit,
): MutableCollection<T> {
    object : CollectionBuilder<T> {
        override fun add(item: T) {
            result.add(item)
        }

        override fun addAll(items: Collection<T>) {
            result.addAll(items)
        }
    }.builder()
    return result
}

fun <T : Any> MutableCollection<T>.addIfNotNull(t: T?) {
    if (t != null) add(t)
}

interface CollectionBuilder<in T> {
    fun add(item: T)
    fun addAll(items: Collection<T>)
}

inline fun <K, V> buildMap(builder: (MapBuilder<K, V>).() -> Unit): Map<K, V> {
    val result = HashMap<K, V>()
    object : MapBuilder<K, V> {
        override fun put(key: K, value: V) {
            result[key] = value
        }

        override fun putAll(map: Map<K, V>) {
            result.putAll(map)
        }
    }.builder()

    return result.replaceTrivialMap()
}

interface MapBuilder<K, in V> {
    fun put(key: K, value: V)
    fun putAll(map: Map<K, V>)
}

fun <K, V> Map<K, V>.replaceTrivialMap(): Map<K, V> = when (size) {
    0 -> emptyMap()
    1 -> {
        val entry = entries.single()
        Collections.singletonMap(entry.key, entry.value)
    }

    else -> this
}

fun <T> SmartList<T>.optimizeList(): List<T> = when (size) {
    0 -> emptyList()
    1 -> Collections.singletonList(single())
    else -> {
        trimToSize()
        this
    }
}

private const val INT_MAX_POWER_OF_TWO: Int = Int.MAX_VALUE / 2 + 1

fun mapCapacity(expectedSize: Int): Int {
    if (expectedSize < 3) {
        return expectedSize + 1
    }
    if (expectedSize < INT_MAX_POWER_OF_TWO) {
        return expectedSize + expectedSize / 3
    }
    return Int.MAX_VALUE // any large value
}

fun <K, V> newHashMapWithExpectedSize(size: Int): HashMap<K, V> =
    HashMap<K, V>(mapCapacity(size))

fun <T> Iterable<T>.collectionSizeOrDefault(default: Int): Int =
    if (this is Collection<*>) size else default

fun makeBitMask(bitToSet: Int): Int = 1 shl bitToSet

fun <K, V1, V2> zipValues(map1: Map<K, V1>, map2: Map<K, V2>): List<Pair<V1, V2>> =
    map1.mapNotNull { (k, v1) -> map2[k]?.let { v2 -> Pair(v1, v2) } }

inline fun <T> List<T>.singleOrFilter(predicate: (T) -> Boolean): List<T> = when {
    size < 2 -> this
    else -> filter(predicate)
}

inline fun <T> List<T>.singleOrLet(function: (List<T>) -> List<T>): List<T> = when {
    size < 2 -> this
    else -> function(this)
}

inline fun <T> List<T>.notEmptyOrLet(function: (List<T>) -> List<T>): List<T> = when {
    isNotEmpty() -> this
    else -> function(this)
}

fun <T> List<T>.chain(other: List<T>): Sequence<T> =
    when {
        other.isEmpty() -> this.asSequence()
        this.isEmpty() -> other.asSequence()
        else -> this.asSequence() + other.asSequence()
    }

inline fun <T, R> Iterable<T>.mapToMutableList(transform: (T) -> R): MutableList<R> =
    mapTo(ArrayList(collectionSizeOrDefault(10)), transform)

inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> =
    mapTo(HashSet(mapCapacity(collectionSizeOrDefault(10))), transform)

inline fun <T, R : Any> Iterable<T>.mapNotNullToSet(transform: (T) -> R?): Set<R> =
    mapNotNullTo(HashSet(mapCapacity(collectionSizeOrDefault(10))), transform)

fun <T> Set<T>.intersects(other: Iterable<T>): Boolean =
    other.any { this.contains(it) }

inline fun <T> Iterable<T>.joinToWithBuffer(
    buffer: StringBuilder,
    separator: CharSequence = ", ",
    prefix: CharSequence = "",
    postfix: CharSequence = "",
    action: T.(StringBuilder) -> Unit,
) {
    buffer.append(prefix)
    var needInsertSeparator = false
    for (element in this) {
        if (needInsertSeparator) {
            buffer.append(separator)
        }
        element.action(buffer)
        needInsertSeparator = true
    }
    buffer.append(postfix)
}

fun <T : Any> Iterator<T>.nextOrNull(): T? =
    if (hasNext()) next() else null

fun <T> MutableList<T>.removeLast(): T = removeAt(size - 1)

fun <T> dequeOf(vararg elements: T): Deque<T> =
    ArrayDeque<T>().apply { addAll(elements) }

inline fun <reified T : Enum<T>> enumSetOf(): EnumSet<T> = EnumSet.noneOf(T::class.java)

fun <T> List<T>.isSortedWith(comparator: Comparator<T>): Boolean =
    asSequence().zipWithNext { a, b -> comparator.compare(a, b) <= 0 }.all { it }

typealias LookbackValue<T> = Pair<T, T?>

fun <T> Sequence<T>.withPrevious(): Sequence<LookbackValue<T>> = LookbackSequence(this)

private class LookbackSequence<T>(private val sequence: Sequence<T>) : Sequence<LookbackValue<T>> {

    override fun iterator(): Iterator<LookbackValue<T>> = LookbackIterator(sequence.iterator())
}

private class LookbackIterator<T>(private val iterator: Iterator<T>) : Iterator<LookbackValue<T>> {

    private var previous: T? = null

    override fun hasNext() = iterator.hasNext()

    override fun next(): LookbackValue<T> {
        val next = iterator.next()
        val result = LookbackValue(next, previous)
        previous = next
        return result
    }
}

typealias WithNextValue<T> = Pair<T, T?>

fun <T : Any> Sequence<T>.withNext(): Sequence<WithNextValue<T>> = WithNextSequence(this)

private class WithNextSequence<T : Any>(private val sequence: Sequence<T>) : Sequence<WithNextValue<T>> {

    override fun iterator(): Iterator<WithNextValue<T>> = WithNextIterator(sequence.iterator())
}

private class WithNextIterator<T : Any>(private val iterator: Iterator<T>) : Iterator<WithNextValue<T>> {

    private var next: T? = null

    override fun hasNext() = next != null || iterator.hasNext()

    override fun next(): WithNextValue<T> {
        if (next == null) { // The first invocation (or illegal after-the-last invocation)
            next = iterator.next()
        }
        val next = next ?: throw NoSuchElementException()
        val nextNext = iterator.nextOrNull()
        this.next = nextNext
        return WithNextValue(next, nextNext)
    }
}

/**
 * 从列表中删除一个元素。
 * 被删除的元素会被列表的最后一个元素替换。
 * 该操作类似于 [MutableList.removeAt]，但牺牲了列表顺序以获得 O(1) 的删除复杂度。
 *
 * @param index 要删除元素的索引
 */
fun <T> MutableList<T>.swapRemoveAt(index: Int) {
    if (index == lastIndex) {
        removeLast()
    } else {
        set(index, removeLast())
    }
}

fun <K, V> Iterable<V>.valuesToMap(key: (V) -> K): Map<K, V> {
    return associateBy(key, { it })
}

fun <K, V> Iterable<K>.keysToMap(value: (K) -> V): Map<K, V> {
    return associateBy({ it }, value)
}

inline fun <reified T, reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceMapTo(
    destination: C,
    transform: (T) -> R
): C {
    for (element in this) {
        if (element is T) {
            destination.add(transform(element))
        }
    }
    return destination
}

inline fun <reified R> Collection<*>.filterIsInstanceAnd(predicate: (R) -> Boolean): List<R> {
    if (isEmpty()) return emptyList()
    return filterIsInstanceAndTo(SmartList(), predicate)
}

inline fun <reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceAndTo(
    destination: C,
    predicate: (R) -> Boolean
): C {
    for (element in this) {
        if (element is R && predicate(element)) {
            destination.add(element)
        }
    }
    return destination
}

fun <T> ArrayList<T>.compact(): List<T> =
    when (size) {
        0 -> emptyList()
        1 -> listOf(first())
        else -> apply { trimToSize() }
    }

fun <T : Any> Sequence<Any>.match(vararg expectedTypes: KClass<*>, last: KClass<T>): T? =
    (expectedTypes.asSequence() + last).zip(this + sequenceOf(null).cycle())
        .map { (expectedType, parent) -> parent?.takeIf(expectedType::isInstance) }
        .takeWhileInclusive { it != null }
        .lastOrNull()
        ?.let(last::cast)

private fun <T> Sequence<T>.cycle(): Sequence<T> = sequence { while (true) yieldAll(this@cycle) }

fun <T> Sequence<T>.takeWhileInclusive(predicate: (T) -> Boolean): Sequence<T> =
    sequence {
        for (elem in this@takeWhileInclusive) {
            yield(elem)
            if (!predicate(elem)) break
        }
    }
