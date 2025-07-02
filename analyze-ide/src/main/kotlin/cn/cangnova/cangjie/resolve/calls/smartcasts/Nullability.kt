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

package cn.cangnova.cangjie.resolve.calls.smartcasts

enum class Nullability(private val canBeNull: Boolean, private val canBeNonNull: Boolean) {
    NULL(true, false),
    NOT_NULL(false, true),
    UNKNOWN(true, true),
    IMPOSSIBLE(false, false);

    fun canBeNull(): Boolean {
        return canBeNull
    }

    fun canBeNonNull(): Boolean {
        return canBeNonNull
    }

    fun refine(other: Nullability): Nullability {
        return when (this) {
            UNKNOWN -> other
            IMPOSSIBLE -> other
            NULL -> when (other) {
                NOT_NULL -> NOT_NULL
                else -> NULL
            }

            NOT_NULL -> when (other) {
                NULL -> NOT_NULL
                else -> NOT_NULL
            }
        }

    }

    fun invert(): Nullability {
        return when (this) {
            NULL -> NOT_NULL
            NOT_NULL -> UNKNOWN
            UNKNOWN -> UNKNOWN
            IMPOSSIBLE -> UNKNOWN
        }

    }

    fun and(other: Nullability): Nullability {
        return fromFlags(this.canBeNull && other.canBeNull, this.canBeNonNull && other.canBeNonNull)
    }

    fun or(other: Nullability): Nullability {
        return fromFlags(this.canBeNull || other.canBeNull, this.canBeNonNull || other.canBeNonNull)
    }

    companion object {
        fun fromFlags(canBeNull: Boolean, canBeNonNull: Boolean): Nullability {
            if (!canBeNull && !canBeNonNull) return IMPOSSIBLE
            if (!canBeNull && canBeNonNull) return NOT_NULL
            if (canBeNull && !canBeNonNull) return NULL
            return UNKNOWN
        }
    }
}
