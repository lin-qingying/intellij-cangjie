/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.utils

import com.intellij.util.SmartList
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
