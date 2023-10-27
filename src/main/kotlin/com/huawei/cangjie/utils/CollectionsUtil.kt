package com.huawei.cangjie.utils

import org.jetbrains.annotations.ApiStatus
import kotlin.reflect.KClass
import kotlin.reflect.cast

@ApiStatus.Internal
fun <T : Any> Sequence<Any>.match(vararg expectedTypes: KClass<*>, last: KClass<T>): T? =
    (expectedTypes.asSequence() + last).zip(this + sequenceOf(null).cycle())
        .map { (expectedType, parent) -> parent?.takeIf(expectedType::isInstance) }
        .takeWhileInclusive { it != null }
        .lastOrNull()
        ?.let(last::cast)
private fun <T> Sequence<T>.cycle(): Sequence<T> = sequence { while (true) yieldAll(this@cycle) }
@ApiStatus.Internal
fun <T> Sequence<T>.takeWhileInclusive(predicate: (T) -> Boolean): Sequence<T> =
    sequence {
        for (elem in this@takeWhileInclusive) {
            yield(elem)
            if (!predicate(elem)) break
        }
    }
