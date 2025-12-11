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

package org.cangnova.cangjie.contracts.description

import kotlin.math.max
import kotlin.math.min

enum class EventOccurrencesRange(private val left: Int, private val right: Int) {
    ZERO(0, 0),          // 0..0
    AT_MOST_ONCE(0, 1),  // 0..1
    EXACTLY_ONCE(1, 1),  // 1..1
    AT_LEAST_ONCE(1, 3), // 1..*
    MORE_THAN_ONCE(2, 3), // 2..*
    UNKNOWN(0, 3);       // 0..*

    companion object {
        private fun fromRange(left: Int, right: Int): EventOccurrencesRange = when (min(left, 2) to min(right, 3)) {
            0 to 0 -> ZERO
            0 to 1 -> AT_MOST_ONCE
            0 to 2 -> UNKNOWN
            0 to 3 -> UNKNOWN
            1 to 1 -> EXACTLY_ONCE
            1 to 2 -> AT_LEAST_ONCE
            1 to 3 -> AT_LEAST_ONCE
            2 to 2 -> MORE_THAN_ONCE
            2 to 3 -> MORE_THAN_ONCE
            3 to 3 -> MORE_THAN_ONCE
            else -> throw IllegalArgumentException()
        }

        fun or(x: EventOccurrencesRange, y: EventOccurrencesRange): EventOccurrencesRange =
            fromRange(min(x.left, y.left), max(x.right, y.right))

        fun plus(x: EventOccurrencesRange, y: EventOccurrencesRange): EventOccurrencesRange =
            fromRange(x.left + y.left, x.right + y.right)
    }

    infix fun or(other: EventOccurrencesRange): EventOccurrencesRange = Companion.or(this, other)
    operator fun plus(other: EventOccurrencesRange): EventOccurrencesRange = Companion.plus(this, other)
    operator fun contains(other: EventOccurrencesRange): Boolean = left <= other.left && other.right <= right

    fun <D : Any> at(marker: D?): MarkedEventOccurrencesRange<D> =
        when (this) {
            ZERO -> MarkedEventOccurrencesRange.Zero
            AT_MOST_ONCE -> MarkedEventOccurrencesRange.AtMostOnce(
                marker ?: throw AssertionError("AT_MOST_ONCE event requires location")
            )

            EXACTLY_ONCE -> MarkedEventOccurrencesRange.ExactlyOnce(
                marker ?: throw AssertionError("EXACTLY_ONCE event requires location")
            )

            AT_LEAST_ONCE -> MarkedEventOccurrencesRange.AtLeastOnce
            MORE_THAN_ONCE -> MarkedEventOccurrencesRange.MoreThanOnce
            UNKNOWN -> MarkedEventOccurrencesRange.Unknown
        }
}

// Extended version of `EventOccurrencesRange` that, for events that can only happen once,
// also carries the event location.
sealed class MarkedEventOccurrencesRange<out D : Any> {
    open val location: D?
        get() = null

    data object Zero : MarkedEventOccurrencesRange<Nothing>()
    data class AtMostOnce<out D : Any>(override val location: D) : MarkedEventOccurrencesRange<D>()
    data class ExactlyOnce<out D : Any>(override val location: D) : MarkedEventOccurrencesRange<D>()
    data object AtLeastOnce : MarkedEventOccurrencesRange<Nothing>()
    data object MoreThanOnce : MarkedEventOccurrencesRange<Nothing>()
    data object Unknown : MarkedEventOccurrencesRange<Nothing>()

    val withoutMarker: EventOccurrencesRange
        get() = when (this) {
            Zero -> EventOccurrencesRange.ZERO
            is AtMostOnce -> EventOccurrencesRange.AT_MOST_ONCE
            is ExactlyOnce -> EventOccurrencesRange.EXACTLY_ONCE
            AtLeastOnce -> EventOccurrencesRange.AT_LEAST_ONCE
            MoreThanOnce -> EventOccurrencesRange.MORE_THAN_ONCE
            Unknown -> EventOccurrencesRange.UNKNOWN
        }
}

fun EventOccurrencesRange.isDefinitelyVisited(): Boolean =
    this == EventOccurrencesRange.EXACTLY_ONCE || this == EventOccurrencesRange.AT_LEAST_ONCE || this == EventOccurrencesRange.MORE_THAN_ONCE

fun EventOccurrencesRange.canBeVisited(): Boolean =
    this != EventOccurrencesRange.ZERO

fun EventOccurrencesRange.canBeRevisited(): Boolean =
    this == EventOccurrencesRange.UNKNOWN || this == EventOccurrencesRange.AT_LEAST_ONCE || this == EventOccurrencesRange.MORE_THAN_ONCE

fun MarkedEventOccurrencesRange<*>.isDefinitelyVisited(): Boolean =
    withoutMarker.isDefinitelyVisited()

fun MarkedEventOccurrencesRange<*>.canBeVisited(): Boolean =
    withoutMarker.canBeVisited()

fun MarkedEventOccurrencesRange<*>.canBeRevisited(): Boolean =
    withoutMarker.canBeRevisited()

val EventOccurrencesRange?.isInPlace: Boolean
    get() = this != null
