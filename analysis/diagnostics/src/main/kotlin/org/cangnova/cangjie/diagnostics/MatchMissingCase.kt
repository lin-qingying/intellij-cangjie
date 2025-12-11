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

package org.cangnova.cangjie.diagnostics

import org.cangnova.cangjie.name.CallableId
import org.cangnova.cangjie.name.IClassId

/**
 * Match 表达式缺失分支情况
 *
 * 这是一个密封类层次结构，用于表示 match 表达式（模式匹配）中可能缺失的各种分支情况。
 * 当编译器检测到 match 表达式不完整（未覆盖所有可能的情况）时，会使用这些类型来描述缺失的分支。
 *
 * ## 主要用途
 * - **穷尽性检查**：确保 match 表达式覆盖了所有可能的情况
 * - **编译器诊断**：生成"缺少分支"的错误或警告信息
 * - **快速修复**：为 IDE 提供自动添加缺失分支的建议
 * - **代码补全**：在编写 match 表达式时提示缺失的 case
 *
 * ## 支持的缺失情况
 * - **Unknown**：未知情况，需要 else 分支
 * - **ConditionTypeIsExpect**：针对密封类型、枚举等的类型检查
 * - **NullIsMissing**：缺少 null 检查
 * - **BooleanIsMissing**：缺少 true 或 false 分支
 * - **IsTypeCheckIsMissing**：缺少特定类型的 is 检查
 * - **EnumCheckIsMissing**：缺少枚举值的检查
 * - **TupleCheckIsMissing**：缺少元组模式的检查
 *
 * @property branchConditionText 用于生成分支条件的文本表示，供 IDE 显示和代码生成使用
 *
 * @see org.cangnova.cangjie.diagnostics.Errors 相关的诊断错误定义
 *
 * @sample
 * ```kotlin
 * // 示例：检测缺失的枚举值分支
 * when (color) {
 *     Color.RED -> println("红色")
 *     Color.GREEN -> println("绿色")
 *     // 缺少 Color.BLUE，编译器会报告 EnumCheckIsMissing(Color.BLUE)
 * }
 * ```
 */
sealed class MatchMissingCase {
    /**
     * 分支条件的文本表示
     *
     * 用于在 IDE 中显示缺失的分支条件，也用于快速修复时生成代码。
     * 例如："null"、"true"、"is MyClass"、"Color.RED" 等。
     */
    abstract val branchConditionText: String

    /**
     * 未知情况
     *
     * 表示 match 表达式需要一个通用的 else 分支来处理所有未覆盖的情况。
     * 这通常发生在以下场景：
     * - 条件类型过于复杂，无法进行穷尽性分析
     * - 存在动态类型或运行时值
     * - 编译器无法确定所有可能的分支
     */
    object Unknown : MatchMissingCase() {
        override fun toString(): String = "unknown"
        override val branchConditionText: String = "else"
    }

    /**
     * 条件类型需要穷尽检查
     *
     * 当 match 的条件是密封类、密封接口或枚举类型时，编译器期望覆盖所有子类型。
     * 这个类用于提示开发者应该为所有子类型添加分支，或添加 else 分支。
     *
     * @property typeOfDeclaration 声明类型的描述，如 "sealed class"、"enum" 等
     */
    sealed class ConditionTypeIsExpect(val typeOfDeclaration: String) : MatchMissingCase() {
        /** 密封类：要求覆盖所有子类 */
        object SealedClass : ConditionTypeIsExpect("sealed class")

        /** 密封接口：要求覆盖所有实现类 */
        object SealedInterface : ConditionTypeIsExpect("sealed interface")

        /** 枚举类：要求覆盖所有枚举值 */
        object Enum : ConditionTypeIsExpect("enum")

        override val branchConditionText: String = "else"
        override fun toString(): String = "unknown"
    }

    /**
     * 缺少 null 检查分支
     *
     * 当 match 表达式的条件可能为 null，但没有处理 null 情况时报告此错误。
     * 常见于可选类型（Option<T>）的模式匹配。
     */
    object NullIsMissing : MatchMissingCase() {
        override val branchConditionText: String = "null"
    }

    /**
     * 缺少布尔值检查分支
     *
     * 当 match 表达式的条件是布尔类型，但缺少 true 或 false 分支时报告。
     *
     * @property value 缺失的布尔值（true 或 false）
     */
    sealed class BooleanIsMissing(val value: Boolean) : MatchMissingCase() {
        /** 缺少 true 分支 */
        object TrueIsMissing : BooleanIsMissing(true)

        /** 缺少 false 分支 */
        object FalseIsMissing : BooleanIsMissing(false)

        override val branchConditionText: String = value.toString()
    }

    /**
     * 缺少类型检查分支
     *
     * 当需要检查特定类型但没有对应的 is 分支时报告。
     * 这通常用于密封类或接口的子类型检查。
     *
     * @property classId 缺失的类型标识符
     * @property isSingleton 是否是单例对象（如 object 声明）
     *                       - true：使用类名直接匹配（如 "MySingleton"）
     *                       - false：使用 is 检查（如 "is MyClass"）
     *
     * @sample
     * ```kotlin
     * // 密封类示例
     * sealed class Result
     * object Success : Result()
     * data class Error(val message: String) : Result()
     *
     * when (result) {
     *     Success -> println("成功")
     *     // 缺少 Error 分支，报告 IsTypeCheckIsMissing(Error, false)
     * }
     * ```
     */
    class IsTypeCheckIsMissing(val classId: IClassId, val isSingleton: Boolean) : MatchMissingCase() {
        override val branchConditionText: String = run {
            val fqName = classId.asSingleFqName().toString()
            if (isSingleton) fqName else "is $fqName"
        }

        override fun toString(): String {
            val className = classId.shortClassName
            val name = if (className.isSpecial) className.asString() else className.identifier
            return if (isSingleton) name else "is $name"
        }
    }

    /**
     * 缺少其他检查分支
     *
     * 用于表示需要处理的其他情况，具体含义取决于上下文。
     */
    class OtherCheckIsMissing : MatchMissingCase() {
        override val branchConditionText: String
            get() = "Other"
    }

    /**
     * 缺少枚举值检查分支
     *
     * 当 match 表达式的条件是枚举类型，但缺少某个枚举值的分支时报告。
     *
     * @property callableId 缺失的枚举值标识符（包含完整路径）
     *
     * @sample
     * ```kotlin
     * enum class Color { RED, GREEN, BLUE }
     *
     * when (color) {
     *     Color.RED -> println("红色")
     *     Color.GREEN -> println("绿色")
     *     // 缺少 Color.BLUE，报告 EnumCheckIsMissing(Color.BLUE)
     * }
     * ```
     */
    class EnumCheckIsMissing(val callableId: CallableId) : MatchMissingCase() {
        override val branchConditionText: String = callableId.asSingleFqName().toString()

        override fun toString(): String {
            return callableId.callableName.identifier
        }
    }

    /**
     * 缺少元组模式检查分支
     *
     * 当 match 表达式进行元组解构匹配时，缺少某个元组模式的分支。
     *
     * @property callableId 缺失的元组模式标识符
     *
     * @sample
     * ```kotlin
     * // 示例：元组模式匹配
     * when (tuple) {
     *     (1, 2) -> println("1和2")
     *     (3, 4) -> println("3和4")
     *     // 可能缺少其他元组组合
     * }
     * ```
     */
    class TupleCheckIsMissing(val callableId: CallableId) : MatchMissingCase() {
        override val branchConditionText: String = callableId.asSingleFqName().toString()

        override fun toString(): String {
            return callableId.callableName.identifier
        }
    }

    /**
     * 返回缺失分支的字符串表示
     *
     * 默认返回 [branchConditionText]，用于日志记录和调试。
     */
    override fun toString(): String {
        return branchConditionText
    }
}
