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

import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.TypeUtils

import org.cangnova.cangjie.types.error.ErrorTypeKind

/**
 * 获取类型的 Option 状态 (推荐使用)
 *
 * - Option<T> 类型 → OptionStatus.OPTION
 * - 非 Option 类型 → OptionStatus.DEFINITE
 */
private val CangJieType.immanentOptionStatus: OptionStatus
    get() = if (TypeUtils.isOptionType(this)) OptionStatus.OPTION else OptionStatus.DEFINITE

/**
 * 获取类型的内在可空性 (已废弃)
 *
 * ⚠️ 此属性基于 Kotlin 的 null 语义,不适合仓颉语言。
 * 请使用 [immanentOptionStatus] 替代。
 *
 * @see immanentOptionStatus
 */
@Deprecated(
    "Use immanentOptionStatus instead. Nullability is based on Kotlin's nullable types.",
    ReplaceWith("immanentOptionStatus")
)
private val CangJieType.immanentNullability: Nullability
    get() = if (TypeUtils.isOptionType(this)) Nullability.UNKNOWN else Nullability.NOT_NULL

/**
 * 数据流值
 *
 * ⚠️ 注意: 仓颉语言的数据流分析与 Kotlin 有本质区别
 *
 * ## 仓颉语言特性
 *
 * 仓颉语言**不需要运行时 null 检查**:
 * - 没有 null 值,只有 `Option<T>` 枚举类型
 * - Option 解包通过模式匹配,不需要智能转换
 * - `?.` 操作符是语法糖,仅适用于 Option 类型
 *
 * ## 当前实现状态
 *
 * 此类来自 Kotlin 编译器,主要用于:
 * - **类型推导**: 追踪表达式的类型信息
 * - **稳定性分析**: 判断变量是否可能被意外修改
 * - **模式匹配**: 支持 match 表达式的类型细化
 *
 * **不再用于**: 运行时 null 状态追踪 (Kotlin 的智能转换)
 *
 * ## 迁移说明
 *
 * - 使用 `immanentOptionStatus` 替代 `immanentNullability`
 * - `immanentOptionStatus` 表示静态类型状态:
 *   - `OptionStatus.OPTION` = 类型是 `Option<T>`
 *   - `OptionStatus.DEFINITE` = 类型是确定的非 Option 类型
 *
 * @param identifierInfo 标识符信息，描述值的来源
 * @param type 值的类型
 * @param immanentNullability [已废弃] 内在可空性,请使用 immanentOptionStatus
 *
 * @see IdentifierInfo
 * @see OptionStatus
 * @see DataFlowInfo
 */
class DataFlowValue(
    val identifierInfo: IdentifierInfo,
    val type: CangJieType,
    @Deprecated("Use immanentOptionStatus instead")
    val immanentNullability: Nullability = type.immanentNullability
) {

    /**
     * 值的 Option 类型状态 (推荐使用)
     *
     * - `OptionStatus.OPTION`: 值的类型是 `Option<T>`
     * - `OptionStatus.DEFINITE`: 值的类型是确定的非 Option 类型
     */
    val immanentOptionStatus: OptionStatus = type.immanentOptionStatus

    /** 数据流值的种类，决定智能转换的安全性 */
    val kind: Kind get() = identifierInfo.kind

    /**
     * 数据流值种类枚举
     *
     * 定义了不同来源的值的稳定性特征，直接影响智能类型转换的安全性。
     *
     * 智能转换规则：
     * - **STABLE_***: 完全安全，可以放心使用智能转换
     * - **LEGACY_***: 历史遗留，有潜在风险但允许使用(带警告)
     * - **其他**: 不安全，不允许智能转换或需要额外检查
     */
    enum class Kind(private val str: String, val description: String = str) {
        /**
         * 稳定的值(let)
         *
         * 包括：
         * - 局部不可变绑定
         * - 函数参数
         * - private/internal成员不可变属性(无open/自定义getter)
         * - 同模块内的protected/public成员不可变属性(无open/自定义getter)
         *
         * 智能转换：完全安全
         */
        STABLE_VALUE("stable let"),

        /**
         * 稳定的复杂表达式
         *
         * 包括：
         * - 代码块
         * - if/else表达式
         * - match表达式
         *
         * 智能转换：完全安全
         */
        STABLE_COMPLEX_EXPRESSION("complex expression", ""),

        /**
         * 遗留：稳定的局部委托属性
         *
         * 历史遗留：本应不稳定，但为了兼容性允许使用，会产生弃用警告
         *
         * 智能转换：允许但不推荐(有警告)
         */
        LEGACY_STABLE_LOCAL_DELEGATED_PROPERTY("local delegated property"),

        /**
         * 带有getter的属性
         *
         * 包括：
         * - 带有open修饰符的属性
         * - 带有自定义getter的属性
         *
         * 原因：getter可能在每次访问时返回不同的值
         *
         * 智能转换：不安全
         */
        PROPERTY_WITH_GETTER("custom getter", "property that has open or custom getter"),

        /**
         * 遗留：外部基类属性
         *
         * 来自其他模块的基类中声明的protected/public属性，在派生类中访问
         * 历史遗留：本应不稳定，但为了兼容性允许使用，会产生弃用警告
         *
         * 原因：外部模块可能修改基类实现
         *
         * 智能转换：允许但不推荐(有警告)
         */
        LEGACY_ALIEN_BASE_PROPERTY("alien derived", "property declared in base class from different module"),

        /**
         * 遗留：非公开类中继承的外部基类属性
         *
         * 来自其他模块的基类中声明的protected/public属性，在非公开API的派生类中继承
         * 历史遗留：本应不稳定，但为了兼容性允许使用，会产生弃用警告
         *
         * 原因：虽然派生类是非公开的，但基类属性仍可能被外部修改
         *
         * 智能转换：允许但不推荐(有警告)
         */
        LEGACY_ALIEN_BASE_PROPERTY_INHERITED_IN_INVISIBLE_CLASS(
            "alien inherited in invisible",
            "property declared in base class from different module inherited in non-public API class"
        ),

        /**
         * 外部公开属性
         *
         * 来自其他模块的public API属性
         *
         * 原因：其他模块可以修改属性值，线程不安全
         *
         * 智能转换：不安全
         */
        ALIEN_PUBLIC_PROPERTY("alien public", "public API property declared in different module"),

        /**
         * 稳定的变量(var)
         *
         * 尚未被修改闭包捕获的局部可变变量
         *
         * 注意：在循环或闭包之前的智能转换是安全的，但之后可能失效
         *
         * 示例：
         * ```cangjie
         * var x: Option<Int> = Some(42)
         * if (x is Some) {
         *     println(x.value)  // 安全，x还没有被修改
         * }
         * // 但在循环中：
         * while (true) {
         *     if (x is Some) {
         *         // 警告：x可能在循环中被修改
         *     }
         *     x = None
         * }
         * ```
         *
         * 智能转换：安全，但需考虑后续修改
         */
        STABLE_VARIABLE("stable var", "local variable that can be changed since the check in a loop"),

        /**
         * 被捕获的变量
         *
         * 被修改闭包捕获的局部变量
         *
         * 原因：闭包可能在任何时候修改变量值
         *
         * 示例：
         * ```cangjie
         * var x: Option<Int> = Some(42)
         * let closure = { => x = None }
         * if (x is Some) {
         *     // 不安全：closure可能已经修改了x
         * }
         * ```
         *
         * 智能转换：不安全
         */
        CAPTURED_VARIABLE("captured var", "local variable that is captured by a changing closure"),

        /**
         * 可变成员属性
         *
         * 任何可见性的可变成员属性(var)
         *
         * 原因：属性可能被其他代码修改，特别是在多线程环境中
         *
         * 智能转换：不安全
         */
        MUTABLE_PROPERTY("member", "mutable property that could have been changed by this time"),

        /**
         * 其他复杂表达式
         *
         * 无法确定稳定性的复杂表达式
         *
         * 智能转换：不安全
         */
        OTHER("other", "complex expression");

        override fun toString() = str
    }

    /**
     * 是否可以被绑定
     *
     * 决定该值是否可以参与数据流分析的绑定操作
     */
    val canBeBound get() = identifierInfo.canBeBound

    /**
     * 是否稳定
     *
     * 稳定意味着我们不期望值会发生突然的变化(例如在另一个线程中访问可变属性)，
     * 因此可以安全地使用智能类型转换。
     *
     * 稳定的值种类包括：
     * - STABLE_VALUE: 稳定的不可变绑定
     * - STABLE_VARIABLE: 稳定的可变变量(未被闭包捕获)
     * - STABLE_COMPLEX_EXPRESSION: 稳定的复杂表达式
     * - LEGACY_STABLE_LOCAL_DELEGATED_PROPERTY: 遗留的稳定局部委托属性
     * - LEGACY_ALIEN_BASE_PROPERTY: 遗留的外部基类属性
     * - LEGACY_ALIEN_BASE_PROPERTY_INHERITED_IN_INVISIBLE_CLASS: 遗留的非公开类中继承的外部基类属性
     *
     * 注意：标记为LEGACY_*的种类虽然被认为稳定，但会产生弃用警告
     */
    val isStable = kind == Kind.STABLE_VALUE ||
            kind == Kind.STABLE_VARIABLE ||
            kind == Kind.STABLE_COMPLEX_EXPRESSION ||
            kind == Kind.LEGACY_STABLE_LOCAL_DELEGATED_PROPERTY ||
            kind == Kind.LEGACY_ALIEN_BASE_PROPERTY ||
            kind == Kind.LEGACY_ALIEN_BASE_PROPERTY_INHERITED_IN_INVISIBLE_CLASS

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is DataFlowValue) return false

        if (identifierInfo != other.identifierInfo) return false
        if (type != other.type) return false

        return true
    }

    override fun toString() = "$kind $identifierInfo $immanentOptionStatus"

    /** 缓存的哈希码 */
    private var hashCode = 0

    /**
     * 计算哈希码
     *
     * 使用延迟计算和缓存策略提高性能
     * 哈希码基于类型和标识符信息
     */
    override fun hashCode(): Int {
        var hashCode = hashCode

        if (hashCode == 0) {
            hashCode = type.hashCode() + 31 * identifierInfo.hashCode()
            this.hashCode = hashCode
        }

        return hashCode
    }

    companion object {

        /**
         * 创建 None 值的数据流值 (推荐使用)
         *
         * 仓颉语言中没有 null,用 Option::None 表示"无值"。
         *
         * @param builtIns 内置类型提供者
         * @return 表示 None 的数据流值
         */
        fun noneValue(builtIns: CangJieBuiltIns) =
            DataFlowValue(
                IdentifierInfo.NULL,
                builtIns.nothingType,
                @Suppress("DEPRECATION") Nullability.NULL
            )

        /**
         * 创建 null 值的数据流值 (已废弃)
         *
         * ⚠️ 仓颉语言中没有 null,请使用 [noneValue] 替代。
         *
         * @param builtIns 内置类型提供者
         * @return 表示 null 的数据流值
         */
        @Deprecated(
            "Cangjie has no null value. Use noneValue() instead.",
            ReplaceWith("noneValue(builtIns)")
        )
        fun nullValue(builtIns: CangJieBuiltIns) = noneValue(builtIns)

        /**
         * 错误数据流值
         *
         * 用于错误恢复和异常情况处理
         */
        val ERROR = DataFlowValue(
            IdentifierInfo.ERROR,
            ErrorUtils.createErrorType(ErrorTypeKind.ERROR_DATA_FLOW_TYPE),
            @Suppress("DEPRECATION") Nullability.IMPOSSIBLE
        )
    }
}
