/*
 * Copyright 2026 LinQingYing. and contributors.
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

/**
 * 从 Map 中获取值，如果键不存在则放入默认值。
 * 与标准库的 [getOrPut] 不同，此方法支持 null 值。
 *
 * @param key 要查找的键
 * @param defaultValue 当键不存在时计算默认值的函数
 * @return 对应键的值（可能为 null）
 */
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

/**
 * 创建一个具有期望容量的 HashSet。
 * 内部会根据负载因子计算合适的初始容量，以避免不必要的扩容。
 *
 * @param expectedSize 期望的元素数量
 * @return 新的 HashSet 实例
 */
fun <E> newHashSetWithExpectedSize(expectedSize: Int): HashSet<E> =
    HashSet(capacity(expectedSize))

/**
 * 根据期望的元素数量计算 HashMap/HashSet 的初始容量。
 * 考虑了默认负载因子（0.75），避免扩容操作。
 *
 * @param expectedSize 期望的元素数量
 * @return 计算后的容量
 */
private fun capacity(expectedSize: Int): Int =
    if (expectedSize < 3) 3 else expectedSize + expectedSize / 3 + 1

/**
 * 创建一个具有期望容量的 LinkedHashSet。
 * 保持插入顺序，同时优化初始容量。
 *
 * @param expectedSize 期望的元素数量
 * @return 新的 LinkedHashSet 实例
 */
fun <E> newLinkedHashSetWithExpectedSize(expectedSize: Int): LinkedHashSet<E> =
    LinkedHashSet(capacity(expectedSize))

/**
 * 使用 DSL 风格构建一个不可变 List。
 *
 * @param builder 构建器函数，提供 [CollectionBuilder] 接口
 * @return 构建的不可变列表
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> buildList(builder: (CollectionBuilder<T>).() -> Unit): List<T> =
    buildCollection(mutableListOf(), builder) as List<T>

/**
 * 使用 DSL 风格构建一个不可变 Set。
 *
 * @param builder 构建器函数，提供 [CollectionBuilder] 接口
 * @return 构建的不可变集合
 */
@Suppress("UNCHECKED_CAST")
inline fun <T> buildSet(builder: (CollectionBuilder<T>).() -> Unit): Set<T> =
    buildCollection(mutableSetOf(), builder) as Set<T>

/**
 * 创建一个具有期望容量的 LinkedHashMap。
 * 保持插入顺序，同时优化初始容量。
 *
 * @param expectedSize 期望的条目数量
 * @return 新的 LinkedHashMap 实例
 */
fun <K, V> newLinkedHashMapWithExpectedSize(expectedSize: Int): LinkedHashMap<K, V> =
    LinkedHashMap(capacity(expectedSize))

/**
 * 通用的集合构建器函数。
 *
 * @param result 用于存储结果的可变集合
 * @param builder 构建器函数
 * @return 填充后的可变集合
 */
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

/**
 * 如果元素不为 null 则添加到集合中。
 * 这是一个安全的添加操作，避免了手动的 null 检查。
 *
 * @param t 要添加的元素，可能为 null
 */
fun <T : Any> MutableCollection<T>.addIfNotNull(t: T?) {
    if (t != null) add(t)
}

/**
 * 集合构建器接口。
 * 用于 DSL 风格的集合构建，提供添加元素的基本操作。
 */
interface CollectionBuilder<in T> {
    /**
     * 添加单个元素到集合。
     *
     * @param item 要添加的元素
     */
    fun add(item: T)

    /**
     * 批量添加多个元素到集合。
     *
     * @param items 要添加的元素集合
     */
    fun addAll(items: Collection<T>)
}

/**
 * 使用 DSL 风格构建一个不可变 Map。
 * 内部使用 HashMap 实现，最终会根据大小优化为 emptyMap 或 singletonMap。
 *
 * @param builder 构建器函数，提供 [MapBuilder] 接口
 * @return 构建的不可变 Map
 *
 * 示例：
 * ```kotlin
 * val map = buildMap<String, Int> {
 *     put("one", 1)
 *     put("two", 2)
 * }
 * ```
 */
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

/**
 * Map 构建器接口。
 * 用于 DSL 风格的 Map 构建，提供添加键值对的基本操作。
 */
interface MapBuilder<K, in V> {
    /**
     * 添加单个键值对到 Map。
     *
     * @param key 键
     * @param value 值
     */
    fun put(key: K, value: V)

    /**
     * 批量添加多个键值对到 Map。
     *
     * @param map 要添加的键值对集合
     */
    fun putAll(map: Map<K, V>)
}

/**
 * 将平凡的 Map 替换为更优化的实现。
 * 根据 Map 的大小，返回空 Map、单例 Map 或原 Map。
 *
 * @return 优化后的 Map
 * - 大小为 0：返回 [emptyMap]
 * - 大小为 1：返回 [Collections.singletonMap]
 * - 其他情况：返回原 Map
 */
fun <K, V> Map<K, V>.replaceTrivialMap(): Map<K, V> = when (size) {
    0 -> emptyMap()
    1 -> {
        val entry = entries.single()
        Collections.singletonMap(entry.key, entry.value)
    }

    else -> this
}

/**
 * 优化 SmartList 为更高效的 List 实现。
 * 根据列表大小返回最优的实现。
 *
 * @return 优化后的 List
 * - 大小为 0：返回 [emptyList]
 * - 大小为 1：返回 [Collections.singletonList]
 * - 其他情况：调用 [trimToSize] 并返回原列表
 */
fun <T> SmartList<T>.optimizeList(): List<T> = when (size) {
    0 -> emptyList()
    1 -> Collections.singletonList(single())
    else -> {
        trimToSize()
        this
    }
}

private const val INT_MAX_POWER_OF_TWO: Int = Int.MAX_VALUE / 2 + 1

/**
 * 根据期望的元素数量计算 Map 的初始容量。
 * 该算法确保在达到指定大小前不会发生扩容，同时避免过度分配内存。
 *
 * @param expectedSize 期望的元素数量
 * @return 计算后的容量
 * - 小于 3：返回 expectedSize + 1
 * - 小于 INT_MAX_POWER_OF_TWO：返回 expectedSize + expectedSize / 3
 * - 其他情况：返回 [Int.MAX_VALUE]
 */
fun mapCapacity(expectedSize: Int): Int {
    if (expectedSize < 3) {
        return expectedSize + 1
    }
    if (expectedSize < INT_MAX_POWER_OF_TWO) {
        return expectedSize + expectedSize / 3
    }
    return Int.MAX_VALUE // any large value
}

/**
 * 创建一个具有期望容量的 HashMap。
 * 内部使用 [mapCapacity] 计算合适的初始容量，避免不必要的扩容。
 *
 * @param size 期望的键值对数量
 * @return 新的 HashMap 实例
 */
fun <K, V> newHashMapWithExpectedSize(size: Int): HashMap<K, V> =
    HashMap<K, V>(mapCapacity(size))

/**
 * 获取集合的大小，如果不是 Collection 则返回默认值。
 * 避免在不知道大小的 Iterable 上进行昂贵的遍历操作。
 *
 * @param default 当无法获取大小时的默认值
 * @return 集合的实际大小或默认值
 */
fun <T> Iterable<T>.collectionSizeOrDefault(default: Int): Int =
    if (this is Collection<*>) size else default

/**
 * 创建一个位掩码，将指定位设置为 1。
 * 使用左移操作实现。
 *
 * @param bitToSet 要设置的位索引（从 0 开始）
 * @return 位掩码值
 *
 * 示例：
 * ```kotlin
 * makeBitMask(0) // 返回 1  (二进制: 0001)
 * makeBitMask(3) // 返回 8  (二进制: 1000)
 * ```
 */
fun makeBitMask(bitToSet: Int): Int = 1 shl bitToSet

/**
 * 将两个 Map 中相同键的值配对。
 * 只有当键同时存在于两个 Map 中时，才会产生一个 Pair。
 *
 * @param map1 第一个 Map
 * @param map2 第二个 Map
 * @return 包含值对的列表
 *
 * 示例：
 * ```kotlin
 * val m1 = mapOf("a" to 1, "b" to 2, "c" to 3)
 * val m2 = mapOf("a" to "x", "c" to "z")
 * zipValues(m1, m2) // 返回 [(1, "x"), (3, "z")]
 * ```
 */
fun <K, V1, V2> zipValues(map1: Map<K, V1>, map2: Map<K, V2>): List<Pair<V1, V2>> =
    map1.mapNotNull { (k, v1) -> map2[k]?.let { v2 -> Pair(v1, v2) } }

/**
 * 如果列表只有一个元素则直接返回，否则应用过滤器。
 * 这是一个性能优化函数，避免在单元素列表上进行不必要的过滤操作。
 *
 * @param predicate 过滤谓词
 * @return 原列表（如果大小小于 2）或过滤后的列表
 */
inline fun <T> List<T>.singleOrFilter(predicate: (T) -> Boolean): List<T> = when {
    size < 2 -> this
    else -> filter(predicate)
}

/**
 * 如果列表只有一个元素则直接返回，否则应用转换函数。
 * 这是一个性能优化函数，避免在单元素列表上进行不必要的转换操作。
 *
 * @param function 转换函数
 * @return 原列表（如果大小小于 2）或转换后的列表
 */
inline fun <T> List<T>.singleOrLet(function: (List<T>) -> List<T>): List<T> = when {
    size < 2 -> this
    else -> function(this)
}

/**
 * 如果列表非空则直接返回，否则应用转换函数。
 * 用于在列表为空时提供备用逻辑。
 *
 * @param function 当列表为空时调用的转换函数
 * @return 原列表（如果非空）或转换后的列表
 */
inline fun <T> List<T>.notEmptyOrLet(function: (List<T>) -> List<T>): List<T> = when {
    isNotEmpty() -> this
    else -> function(this)
}

/**
 * 将两个列表连接成一个序列。
 * 使用惰性求值，避免创建新的列表。优化了空列表的情况。
 *
 * @param other 要连接的另一个列表
 * @return 连接后的序列
 * - 如果 other 为空：返回当前列表的序列
 * - 如果当前列表为空：返回 other 的序列
 * - 其他情况：返回两个序列的连接
 */
fun <T> List<T>.chain(other: List<T>): Sequence<T> =
    when {
        other.isEmpty() -> this.asSequence()
        this.isEmpty() -> other.asSequence()
        else -> this.asSequence() + other.asSequence()
    }

/**
 * 将 Iterable 映射为 MutableList。
 * 预先分配容量以提高性能。
 *
 * @param transform 转换函数
 * @return 转换后的可变列表
 */
inline fun <T, R> Iterable<T>.mapToMutableList(transform: (T) -> R): MutableList<R> =
    mapTo(ArrayList(collectionSizeOrDefault(10)), transform)

/**
 * 将 Iterable 映射为 Set。
 * 预先分配容量以提高性能，避免重复元素。
 *
 * @param transform 转换函数
 * @return 转换后的集合
 */
inline fun <T, R> Iterable<T>.mapToSet(transform: (T) -> R): Set<R> =
    mapTo(HashSet(mapCapacity(collectionSizeOrDefault(10))), transform)

/**
 * 将 Iterable 映射为 Set，跳过 null 值。
 * 预先分配容量以提高性能，自动过滤掉转换结果为 null 的元素。
 *
 * @param transform 转换函数，可能返回 null
 * @return 转换后的集合，不包含 null 值
 */
inline fun <T, R : Any> Iterable<T>.mapNotNullToSet(transform: (T) -> R?): Set<R> =
    mapNotNullTo(HashSet(mapCapacity(collectionSizeOrDefault(10))), transform)

/**
 * 判断两个集合是否有交集。
 * 如果 other 中的任何元素存在于当前 Set 中，则返回 true。
 *
 * @param other 要检查交集的可迭代对象
 * @return 如果存在交集则返回 true，否则返回 false
 */
fun <T> Set<T>.intersects(other: Iterable<T>): Boolean =
    other.any { this.contains(it) }

/**
 * 将 Iterable 中的元素连接到 StringBuilder。
 * 与标准库的 joinTo 不同，此方法允许每个元素自定义如何写入 buffer。
 *
 * @param buffer 用于接收字符串的 StringBuilder
 * @param separator 元素之间的分隔符，默认为 ", "
 * @param prefix 整个字符串的前缀，默认为空
 * @param postfix 整个字符串的后缀，默认为空
 * @param action 每个元素写入 buffer 的操作
 */
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

/**
 * 安全地获取迭代器的下一个元素。
 * 如果没有更多元素则返回 null，而不是抛出异常。
 *
 * @return 下一个元素，如果没有则返回 null
 */
fun <T : Any> Iterator<T>.nextOrNull(): T? =
    if (hasNext()) next() else null

/**
 * 移除并返回列表的最后一个元素。
 * 这是 [removeAt](size - 1) 的便捷方法。
 *
 * @return 被移除的最后一个元素
 * @throws IndexOutOfBoundsException 如果列表为空
 */
fun <T> MutableList<T>.removeLast(): T = removeAt(size - 1)

/**
 * 创建一个包含指定元素的双端队列（Deque）。
 * 使用 ArrayDeque 实现。
 *
 * @param elements 要添加到队列的元素
 * @return 新的 Deque 实例
 */
fun <T> dequeOf(vararg elements: T): Deque<T> =
    ArrayDeque<T>().apply { addAll(elements) }

/**
 * 创建一个空的 EnumSet。
 * 使用 reified 类型参数自动推断枚举类型。
 *
 * @return 空的 EnumSet
 *
 * 示例：
 * ```kotlin
 * enum class Color { RED, GREEN, BLUE }
 * val colors = enumSetOf<Color>()
 * ```
 */
inline fun <reified T : Enum<T>> enumSetOf(): EnumSet<T> = EnumSet.noneOf(T::class.java)

/**
 * 判断列表是否按照指定的比较器排序。
 * 检查列表中的每对相邻元素是否满足排序关系。
 *
 * @param comparator 用于比较元素的比较器
 * @return 如果列表已排序则返回 true，否则返回 false
 */
fun <T> List<T>.isSortedWith(comparator: Comparator<T>): Boolean =
    asSequence().zipWithNext { a, b -> comparator.compare(a, b) <= 0 }.all { it }

/**
 * 表示一个值及其前一个值的类型别名。
 * first 是当前值，second 是前一个值（可能为 null）。
 */
typealias LookbackValue<T> = Pair<T, T?>

/**
 * 为序列中的每个元素附加其前一个元素。
 * 第一个元素的前一个值为 null。
 *
 * @return 新的序列，每个元素是一个 Pair(当前值, 前一个值)
 *
 * 示例：
 * ```kotlin
 * sequenceOf(1, 2, 3).withPrevious()
 * // 结果: (1, null), (2, 1), (3, 2)
 * ```
 */
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

/**
 * 表示一个值及其后一个值的类型别名。
 * first 是当前值，second 是后一个值（可能为 null）。
 */
typealias WithNextValue<T> = Pair<T, T?>

/**
 * 为序列中的每个元素附加其后一个元素。
 * 最后一个元素的后一个值为 null。
 *
 * @return 新的序列，每个元素是一个 Pair(当前值, 后一个值)
 *
 * 示例：
 * ```kotlin
 * sequenceOf(1, 2, 3).withNext()
 * // 结果: (1, 2), (2, 3), (3, null)
 * ```
 */
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

/**
 * 将 Iterable 中的值转换为 Map，使用提供的函数生成键。
 * 值本身作为 Map 的值。
 *
 * @param key 根据值生成键的函数
 * @return 生成的 Map，键由 key 函数生成，值为原始元素
 *
 * 示例：
 * ```kotlin
 * listOf("a", "bb", "ccc").valuesToMap { it.length }
 * // 结果: {1="a", 2="bb", 3="ccc"}
 * ```
 */
fun <K, V> Iterable<V>.valuesToMap(key: (V) -> K): Map<K, V> {
    return associateBy(key, { it })
}

/**
 * 将 Iterable 中的键转换为 Map，使用提供的函数生成值。
 * 键本身作为 Map 的键。
 *
 * @param value 根据键生成值的函数
 * @return 生成的 Map，键为原始元素，值由 value 函数生成
 *
 * 示例：
 * ```kotlin
 * listOf(1, 2, 3).keysToMap { it * 10 }
 * // 结果: {1=10, 2=20, 3=30}
 * ```
 */
fun <K, V> Iterable<K>.keysToMap(value: (K) -> V): Map<K, V> {
    return associateBy({ it }, value)
}

/**
 * 过滤特定类型的实例并进行转换，然后添加到目标集合。
 * 结合了类型过滤和映射的功能。
 *
 * @param destination 目标集合
 * @param transform 对过滤出的元素进行转换的函数
 * @return 目标集合
 *
 * 示例：
 * ```kotlin
 * val mixed = listOf(1, "a", 2, "b", 3)
 * val result = mutableListOf<String>()
 * mixed.filterIsInstanceMapTo<Int, String, _>(result) { "num:$it" }
 * // result: ["num:1", "num:2", "num:3"]
 * ```
 */
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

/**
 * 过滤特定类型的实例并应用额外的谓词过滤。
 * 结合了类型过滤和条件过滤的功能。
 *
 * @param predicate 对过滤出的元素进行额外判断的谓词
 * @return 过滤后的列表
 *
 * 示例：
 * ```kotlin
 * val mixed = listOf(1, "a", 2, "b", 3)
 * mixed.filterIsInstanceAnd<Int> { it > 1 }
 * // 结果: [2, 3]
 * ```
 */
inline fun <reified R> Collection<*>.filterIsInstanceAnd(predicate: (R) -> Boolean): List<R> {
    if (isEmpty()) return emptyList()
    return filterIsInstanceAndTo(SmartList(), predicate)
}

/**
 * 过滤特定类型的实例并应用额外的谓词过滤，结果添加到目标集合。
 * 结合了类型过滤和条件过滤的功能。
 *
 * @param destination 目标集合
 * @param predicate 对过滤出的元素进行额外判断的谓词
 * @return 目标集合
 */
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

/**
 * 压缩 ArrayList 以减少内存占用。
 * 根据列表大小返回最优的实现。
 *
 * @return 优化后的 List
 * - 大小为 0：返回 [emptyList]
 * - 大小为 1：返回单例列表
 * - 其他情况：调用 [trimToSize] 并返回原列表
 */
fun <T> ArrayList<T>.compact(): List<T> =
    when (size) {
        0 -> emptyList()
        1 -> listOf(first())
        else -> apply { trimToSize() }
    }

/**
 * 匹配序列中的元素是否符合指定的类型模式。
 * 检查序列中的元素是否依次匹配给定的类型，最后返回符合最后一个类型的元素。
 *
 * @param expectedTypes 期望的类型序列（除了最后一个）
 * @param last 最后期望匹配的类型
 * @return 如果匹配成功，返回符合最后一个类型的元素；否则返回 null
 *
 * 示例：
 * ```kotlin
 * sequenceOf(String::class, Int::class, Boolean::class)
 *     .match(String::class, last = Int::class)
 * // 检查序列是否为 [String, Int]，如果是则返回 Int 类型的元素
 * ```
 */
fun <T : Any> Sequence<Any>.match(vararg expectedTypes: KClass<*>, last: KClass<T>): T? =
    (expectedTypes.asSequence() + last).zip(this + sequenceOf(null).cycle())
        .map { (expectedType, parent) -> parent?.takeIf(expectedType::isInstance) }
        .takeWhileInclusive { it != null }
        .lastOrNull()
        ?.let(last::cast)

private fun <T> Sequence<T>.cycle(): Sequence<T> = sequence { while (true) yieldAll(this@cycle) }

/**
 * 获取序列中满足谓词的元素，包括第一个不满足条件的元素。
 * 与标准库的 [takeWhile] 不同，此方法会包含第一个不满足谓词的元素。
 *
 * @param predicate 判断条件
 * @return 新的序列，包含满足条件的元素以及第一个不满足条件的元素
 *
 * 示例：
 * ```kotlin
 * sequenceOf(2, 4, 6, 7, 8).takeWhileInclusive { it % 2 == 0 }
 * // 结果: 2, 4, 6, 7 (包含第一个奇数 7)
 * ```
 */
fun <T> Sequence<T>.takeWhileInclusive(predicate: (T) -> Boolean): Sequence<T> =
    sequence {
        for (elem in this@takeWhileInclusive) {
            yield(elem)
            if (!predicate(elem)) break
        }
    }
