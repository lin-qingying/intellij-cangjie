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

package org.cangnova.cangjie.resolve.calls.smartcasts

/**
 * OptionStatus 表示仓颉语言中值的 Option 类型状态
 *
 * ## 设计原则
 *
 * 仓颉语言的 Option<T> 是一个**静态的枚举类型**,不是运行时的 null 检查。
 *
 * ```cangjie
 * enum Option<T> {
 *     | Some(T)
 *     | None
 * }
 * ```
 *
 * ## 与 Kotlin Nullability 的区别
 *
 * | 概念 | Kotlin | 仓颉 |
 * |------|--------|------|
 * | null 值 | 运行时存在 | 不存在 |
 * | 可空类型 | T? (语言内置) | Option<T> (标准库枚举) |
 * | 检查方式 | 运行时检查 | 模式匹配 |
 * | 智能转换 | if (x != null) | match x { Some(v) => ... } |
 * | ?. 操作符 | 运行时短路 | 语法糖,只能用于 Option<T> |
 *
 * ## OptionStatus 语义
 *
 * - **DEFINITE**: 值的类型是确定的非 Option 类型 (例如: `Int64`, `String`)
 *   - 这是**静态类型信息**,不是"值不为 null"的运行时状态
 *   - 示例: `let x: Int64 = 42` → x 的状态是 DEFINITE
 *
 * - **OPTION**: 值的类型是 `Option<T>`
 *   - 这是**静态类型信息**,不是"值可能为 null"的运行时状态
 *   - 示例: `let x: Option<Int64> = Some(42)` → x 的状态是 OPTION
 *   - 注意: 即使知道是 Some,类型仍然是 Option<Int64>
 *
 * - **UNKNOWN**: 编译器无法确定类型是否是 Option (用于类型推导过程中)
 *   - 这不是"可能为 null 或不为 null"
 *   - 而是"类型变量尚未解析"
 *
 * ## 重要提醒
 *
 * ⚠️ OptionStatus **不跟踪运行时状态**!
 *
 * - ❌ 错误: "值在运行时是 null 还是非 null"
 * - ✅ 正确: "值的静态类型是 T 还是 Option<T>"
 *
 * @see org.cangnova.cangjie.types.OptionTypeUtils 用于 Option 类型的静态操作
 */
enum class OptionStatus(
    private val isDefinitelyOption: Boolean,
    private val isDefinitelyNonOption: Boolean
) {
    /**
     * 值的类型确定是非 Option 类型
     *
     * 示例:
     * - `let x: Int64 = 42` → DEFINITE
     * - `let x: String = "hello"` → DEFINITE
     */
    DEFINITE(isDefinitelyOption = false, isDefinitelyNonOption = true),

    /**
     * 值的类型确定是 Option<T>
     *
     * 示例:
     * - `let x: Option<Int64> = Some(42)` → OPTION
     * - `let x: ?String = None` → OPTION
     *
     * 注意: 即使通过模式匹配知道是 Some,在匹配外部类型仍是 Option<T>
     */
    OPTION(isDefinitelyOption = true, isDefinitelyNonOption = false),

    /**
     * 类型尚未确定 (用于类型推导过程)
     *
     * 示例:
     * - 类型变量 `T` 尚未被固定
     * - 需要进一步的类型推导
     */
    UNKNOWN(isDefinitelyOption = false, isDefinitelyNonOption = false);

    /**
     * 类型是否可能是 Option<T>
     */
    fun canBeOption(): Boolean = isDefinitelyOption || this == UNKNOWN

    /**
     * 类型是否可能是非 Option 类型
     */
    fun canBeNonOption(): Boolean = isDefinitelyNonOption || this == UNKNOWN

    /**
     * 与另一个 OptionStatus 结合,取更精确的信息
     *
     * 逻辑:
     * - UNKNOWN 结合任何状态 → 采用对方的状态
     * - DEFINITE 与 OPTION 冲突 → 保持原状态(通常是类型错误)
     * - 相同状态 → 保持不变
     */
    fun refine(other: OptionStatus): OptionStatus = when (this) {
        UNKNOWN -> other
        DEFINITE -> if (other == OPTION) this else other
        OPTION -> if (other == DEFINITE) this else other
    }

    /**
     * 交集: 两个状态都必须满足
     *
     * 用于合并多个约束条件
     */
    fun and(other: OptionStatus): OptionStatus {
        val canBeOpt = this.canBeOption() && other.canBeOption()
        val canBeNonOpt = this.canBeNonOption() && other.canBeNonOption()
        return fromFlags(canBeOpt, canBeNonOpt)
    }

    /**
     * 并集: 两个状态至少满足一个
     *
     * 用于表示可能的分支情况
     */
    fun or(other: OptionStatus): OptionStatus {
        val canBeOpt = this.canBeOption() || other.canBeOption()
        val canBeNonOpt = this.canBeNonOption() || other.canBeNonOption()
        return fromFlags(canBeOpt, canBeNonOpt)
    }

    companion object {
        /**
         * 从标志位构造 OptionStatus
         */
        fun fromFlags(canBeOption: Boolean, canBeNonOption: Boolean): OptionStatus = when {
            !canBeOption && canBeNonOption -> DEFINITE
            canBeOption && !canBeNonOption -> OPTION
            else -> UNKNOWN
        }

        /**
         * 从 Kotlin 的 Nullability 迁移到 OptionStatus
         *
         * 迁移映射:
         * - NULL → OPTION (用 Option::None 表示)
         * - NOT_NULL → DEFINITE (非 Option 类型)
         * - UNKNOWN → UNKNOWN (保持不变)
         * - IMPOSSIBLE → UNKNOWN (无效状态视为未知)
         */
        @Deprecated(
            "Use OptionStatus directly instead of converting from Nullability",
            ReplaceWith("OptionStatus.DEFINITE or OptionStatus.OPTION")
        )
        fun fromNullability(nullability: Nullability): OptionStatus = when (nullability) {
            Nullability.NULL -> OPTION  // null 在仓颉中用 None 表示
            Nullability.NOT_NULL -> DEFINITE  // 非 null 即非 Option
            Nullability.UNKNOWN -> UNKNOWN
            Nullability.IMPOSSIBLE -> UNKNOWN  // 无效状态
        }
    }
}
