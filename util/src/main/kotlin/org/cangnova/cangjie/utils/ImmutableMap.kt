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

import io.vavr.Tuple2
import io.vavr.control.Option

/**
 * 不可变多重映射（键到值集合的映射）
 */
typealias ImmutableMultimap<K, V> = ImmutableMap<K, ImmutableSet<V>>

/**
 * 元组类型别名（二元组）
 */
typealias Tuple2 = Tuple2<*, *>

/**
 * 不可变映射类型别名
 *
 * 基于 Vavr 库的不可变 Map 实现。
 */
typealias ImmutableMap<K, V> = io.vavr.collection.Map<K, V>

/**
 * 不可变哈希映射类型别名
 *
 * 基于 Vavr 库的不可变 HashMap 实现。
 */
typealias ImmutableHashMap<K, V> = io.vavr.collection.HashMap<K, V>

/**
 * 不可变集合类型别名
 *
 * 基于 Vavr 库的不可变 Set 实现。
 */
typealias ImmutableSet<E> = io.vavr.collection.Set<E>

/**
 * 不可变哈希集合类型别名
 *
 * 基于 Vavr 库的不可变 HashSet 实现。
 */
typealias ImmutableHashSet<E> = io.vavr.collection.HashSet<E>

/**
 * 不可变链接哈希集合类型别名
 *
 * 基于 Vavr 库的不可变 LinkedHashSet 实现，保持插入顺序。
 */
typealias ImmutableLinkedHashSet<E> = io.vavr.collection.LinkedHashSet<E>

/**
 * 二元组的第一个元素访问器
 *
 * 使用解构语法访问 Tuple2 的第一个元素。
 *
 * 示例：
 * ```kotlin
 * val tuple = Tuple2.of("hello", 42)
 * val (first, second) = tuple
 * println(first)  // "hello"
 * ```
 *
 * @return 元组的第一个元素
 */
operator fun <T> Tuple2<T, *>.component1(): T = _1()

/**
 * 二元组的第二个元素访问器
 *
 * 使用解构语法访问 Tuple2 的第二个元素。
 *
 * 示例：
 * ```kotlin
 * val tuple = Tuple2.of("hello", 42)
 * val (first, second) = tuple
 * println(second)  // 42
 * ```
 *
 * @return 元组的第二个元素
 */
operator fun <T> Tuple2<*, T>.component2(): T = _2()

/**
 * 将 Option 转换为可空类型
 *
 * 如果 Option 有值，返回该值；否则返回 null。
 *
 * 示例：
 * ```kotlin
 * val some: Option<String> = Option.some("hello")
 * val none: Option<String> = Option.none()
 *
 * println(some.getOrNull())  // "hello"
 * println(none.getOrNull())  // null
 * ```
 *
 * @return Option 中的值或 null
 */
fun <T> Option<T>.getOrNull(): T? = getOrElse(null as T?)

/**
 * 从不可变 Map 中获取可空值
 *
 * 如果键存在，返回对应的值；否则返回 null。
 *
 * 示例：
 * ```kotlin
 * val map = HashMap.of("key1", "value1")
 * println(map.getOrNull("key1"))  // "value1"
 * println(map.getOrNull("key2"))  // null
 * ```
 *
 * @param k 要查找的键
 * @return 键对应的值或 null
 */
fun <K, V> ImmutableMap<K, V>.getOrNull(k: K): V? = get(k)?.getOrElse(null as V?)
