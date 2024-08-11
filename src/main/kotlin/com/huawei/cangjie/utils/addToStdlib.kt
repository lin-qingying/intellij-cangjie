package com.huawei.cangjie.utils

import java.lang.reflect.Modifier
import java.util.*
import java.util.concurrent.ConcurrentHashMap

fun <T> sequenceOfLazyValues(vararg elements: () -> T): Sequence<T> = elements.asSequence().map { it() }

inline fun <T, R : Any> Iterable<T>.firstNotNullResult(transform: (T) -> R?): R? {
    for (element in this) {
        val result = transform(element)
        if (result != null) return result
    }
    return null
}

fun shouldNotBeCalled(message: String = "should not be called"): Nothing {
    error(message)
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
    transform: (T) -> Iterable<R>?
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

@Suppress("INVISIBLE_REFERENCE", "INVISIBLE_MEMBER")
@UnsafeCastFunction
inline fun <reified T : Any> Any?.safeAs(): @kotlin.internal.NoInfer T? = this as? T


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

