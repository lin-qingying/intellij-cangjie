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

package com.linqingying.cangjie.types


enum class Variance(
    val label: String,
    val allowsInPosition: Boolean,
    val allowsOutPosition: Boolean,
    private val superpositionFactor: Int
) {
    INVARIANT("", true, true, 0),
//    IN_VARIANCE("in", true, false, -1),
//    OUT_VARIANCE("out", false, true, +1);
    ;

//    fun allowsPosition(position: Variance): Boolean
//            = when (position) {
//        IN_VARIANCE -> allowsInPosition
//        OUT_VARIANCE -> allowsOutPosition
//
//        INVARIANT -> allowsInPosition && allowsOutPosition
//    }

    fun superpose(other: Variance): Variance {
        return when (val r = this.superpositionFactor * other.superpositionFactor) {
            0 -> INVARIANT
//            -1 -> IN_VARIANCE

            else -> throw IllegalStateException("Illegal factor: $r")
        }
    }

    fun opposite(): Variance {
        return when (this) {
            INVARIANT -> INVARIANT
//            IN_VARIANCE -> OUT_VARIANCE
//            OUT_VARIANCE -> IN_VARIANCE
        }
    }

    override fun toString() = label
}
