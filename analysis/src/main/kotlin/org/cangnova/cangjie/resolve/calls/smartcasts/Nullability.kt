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

package org.cangnova.cangjie.resolve.calls.smartcasts

/**
 * Nullability 枚举 - 已废弃
 *
 * ⚠️ 此枚举来自 Kotlin 的可空性系统,不适合仓颉语言。
 *
 * 在仓颉语言中:
 * - 没有运行时 null 值
 * - Option<T> 是一个标准的枚举类型
 * - 不需要跟踪"可空性"状态
 *
 * 请使用 [OptionStatus] 替代。
 *
 * @see OptionStatus 用于仓颉语言的 Option 类型状态
 */
@Deprecated(
    "Use OptionStatus instead. Nullability is based on Kotlin's nullable types, " +
            "which don't match Cangjie's Option<T> design.",
    ReplaceWith("OptionStatus", "org.cangnova.cangjie.resolve.calls.smartcasts.OptionStatus")
)
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

    /**
     * 转换为 OptionStatus
     *
     * 迁移映射:
     * - NULL → OPTION (用 Option::None 表示)
     * - NOT_NULL → DEFINITE (非 Option 类型)
     * - UNKNOWN → UNKNOWN (保持不变)
     */
    fun toOptionStatus(): OptionStatus = when (this) {
        NULL -> OptionStatus.OPTION
        NOT_NULL -> OptionStatus.DEFINITE
        UNKNOWN -> OptionStatus.UNKNOWN
        IMPOSSIBLE -> OptionStatus.UNKNOWN
    }

    companion object {
        fun fromFlags(canBeNull: Boolean, canBeNonNull: Boolean): Nullability {
            if (!canBeNull && !canBeNonNull) return IMPOSSIBLE
            if (!canBeNull && canBeNonNull) return NOT_NULL
            if (canBeNull && !canBeNonNull) return NULL
            return UNKNOWN
        }

        /**
         * 从 OptionStatus 转换 (用于向后兼容)
         */
        fun fromOptionStatus(status: OptionStatus): Nullability = when (status) {
            OptionStatus.DEFINITE -> NOT_NULL
            OptionStatus.OPTION -> NULL  // 注意: 这是近似映射
            OptionStatus.UNKNOWN -> UNKNOWN
        }
    }
}
