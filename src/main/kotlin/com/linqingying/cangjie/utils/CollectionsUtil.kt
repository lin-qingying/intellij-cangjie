package com.linqingying.cangjie.utils

import com.intellij.util.SmartList
import org.jetbrains.annotations.ApiStatus
import java.util.ArrayList
import kotlin.reflect.KClass
import kotlin.reflect.cast
inline fun <reified T, reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceMapTo(destination: C, transform: (T) -> R): C {
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
inline fun <reified R, C : MutableCollection<in R>> Iterable<*>.filterIsInstanceAndTo(destination: C, predicate: (R) -> Boolean): C {
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
