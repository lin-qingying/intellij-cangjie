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

import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap
fun <T, C : MutableCollection<in T>> Iterable<Iterable<T>>.flattenTo(c: C): C {
    for (element in this) {
        c.addAll(element)
    }
    return c
}
inline fun <T> Iterable<T>.sumByLong(selector: (T) -> Long): Long {
    var sum: Long = 0
    for (element in this) {
        sum += selector(element)
    }
    return sum
}

fun <T> sequenceOfLazyValues(vararg elements: () -> T): Sequence<T> = elements.asSequence().map { it() }
inline fun <T, C : Collection<T>, O> C.ifNotEmpty(body: C.() -> O?): O? = if (isNotEmpty()) this.body() else null

inline fun <T, R : Any> Iterable<T>.firstNotNullResult(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) return result
    }
    return null
}

fun <E> MutableList<E>.popLast(): E = removeAt(lastIndex)
inline fun <T> Boolean.ifTrue(body: () -> T?): T? =
    if (this) body() else null

fun shouldNotBeCalled(message: String = "should not be called"): Nothing {
    error(message)
}

inline fun <reified T> Sequence<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

inline fun <reified T> Array<*>.firstIsInstance(): T {
    for (element in this) if (element is T) return element
    throw NoSuchElementException("No element of given type found")
}

inline fun <reified T : Any> Array<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

fun <K, V> Map<K, V>.compactIfPossible(): Map<K, V> =
    when (size) {
        0 -> emptyMap()
        1 -> Collections.singletonMap(keys.single(), values.single())
        else -> this
    }

inline fun <T, R, C : MutableCollection<in R>> Iterable<T>.flatMapToNullable(
    destination: C,
    transform: (T) -> Iterable<R>?,
): C? {
    for (element in this) {
        val list = transform(element) ?: return null
        destination.addAll(list)
    }
    return destination
}

fun <E> MutableList<E>.trimToSize(newSize: Int) {
    subList(newSize, size).clear()
}

fun <T> Set<T>.compactIfPossible(): Set<T> =
    when (size) {
        0 -> emptySet()
        1 -> setOf(single())
        else -> this
    }

inline fun <R> runIf(condition: Boolean, block: () -> R): R? = if (condition) block() else null
inline fun <reified T : Any> Sequence<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

annotation class UnsafeCastFunction

@UnsafeCastFunction
inline fun <reified T : Any> Any?.cast(): T = this as T
private val constantMap = ConcurrentHashMap<Function0<*>, Any>()
fun <T : Any> constant(calculator: () -> T): T {
    val cached = constantMap[calculator]
    @Suppress("UNCHECKED_CAST")
    if (cached != null) return cached as T

    val fields = calculator::class.java.declaredFields.filter { it.modifiers.and(Modifier.STATIC) == 0 }
    assert(fields.isEmpty()) {
        "No fields in the passed lambda expected but ${fields.joinToString()} found"
    }

    val value = calculator()
    constantMap[calculator] = value
    return value
}

@Suppress( "INVISIBLE_MEMBER")
@UnsafeCastFunction
inline fun <reified T : Any> Any?.safeAs():   T? = this as? T

inline fun <reified T : Any> Iterable<*>.lastIsInstanceOrNull(): T? {
    when (this) {
        is List<*> -> {
            for (i in this.indices.reversed()) {
                val element = this[i]
                if (element is T) return element
            }
            return null
        }

        else -> {
            return reversed().firstIsInstanceOrNull<T>()
        }
    }
}

inline fun <reified T : Any> Iterable<*>.firstIsInstanceOrNull(): T? {
    for (element in this) if (element is T) return element
    return null
}

/**
 * Checks if a bit flag is set in this integer value.
 * @param flag The flag to check
 * @return true if the flag is set, false otherwise
 */
infix fun Int.hasFlag(flag: Int): Boolean = (this and flag) == flag
