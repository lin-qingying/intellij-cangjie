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
 */

package org.cangnova.cangjie.types.expressions.match

import org.cangnova.cangjie.psi.CjEnum
import org.cangnova.cangjie.resolve.source.getPsi
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.deccriptorClass
import org.cangnova.cangjie.types.isEnum

/**
 * 可反驳性检查结果（RefutabilityResult）
 *
 * 表示模式可反驳性检查的结果。在仓颉语言中，模式分为两类：
 *
 * - **Irrefutable（不可反驳）**: 总是匹配成功
 * - **Refutable（可反驳）**: 可能匹配失败
 *
 * ## 仓颉语言规则
 *
 * | 上下文 | 允许的模式 |
 * |--------|-----------|
 * | match 表达式 | 任意模式 |
 * | let 声明 | 仅 irrefutable |
 * | for-in 循环 | 仅 irrefutable |
 * | let 表达式 | 任意模式 |
 *
 * @see RefutabilityChecker
 * @see PatternSource
 */
sealed class RefutabilityResult {

    /**
     * 模式是不可反驳的（总是匹配）
     */
    data object Irrefutable : RefutabilityResult()

    /**
     * 模式是可反驳的（可能不匹配）
     *
     * @property reason 可反驳的原因（用于错误消息）
     * @property patternDescription 模式的描述（用于错误消息）
     */
    data class Refutable(
        val reason: String,
        val patternDescription: String = ""
    ) : RefutabilityResult()

    /**
     * 是否为不可反驳
     */
    val isIrrefutable: Boolean
        get() = this is Irrefutable

    /**
     * 是否为可反驳
     */
    val isRefutable: Boolean
        get() = this is Refutable
}

/**
 * 可反驳性检查器（RefutabilityChecker）
 *
 * 检查模式的可反驳性（refutability）。在仓颉语言中，某些语法位置
 * （如 let 声明、for-in 循环）只允许不可反驳的模式。
 *
 * ## 仓颉语言模式可反驳性规则
 *
 * | 模式类型 | 可反驳性 | 说明 |
 * |---------|---------|------|
 * | 通配符 `_` | Irrefutable | 匹配任何值 |
 * | 绑定 `x` | Irrefutable | 匹配任何值并绑定 |
 * | 常量 `42` | Refutable | 仅匹配特定值 |
 * | 类型 `x: T` | Refutable | 运行时类型检查 |
 * | 元组 `(p1, p2)` | 递归检查 | 所有子模式都必须 irrefutable |
 * | 枚举 `Some(x)` | 取决于枚举 | 单构造器枚举是 irrefutable |
 *
 * ## 使用示例
 *
 * ```kotlin
 * val checker = RefutabilityChecker.INSTANCE
 *
 * // 检查单个模式
 * val result = checker.check(pattern)
 * if (result.isRefutable && !context.allowRefutable) {
 *     reportError("此位置不允许可反驳模式")
 * }
 * ```
 *
 * @see RefutabilityResult
 * @see PatternSource
 */
object RefutabilityChecker {

    /**
     * 检查模式的可反驳性
     *
     * @param pattern 要检查的模式
     * @return 检查结果
     */
    fun check(pattern: Pattern): RefutabilityResult {
        return checkKind(pattern.kind, pattern.type)
    }

    /**
     * 检查模式种类的可反驳性
     */
    private fun checkKind(kind: PatternKind, type: CangJieType): RefutabilityResult {
        return when (kind) {
            // 通配符模式：总是匹配
            is PatternKind.Wild -> RefutabilityResult.Irrefutable

            // 绑定模式：总是匹配（绑定任何值）
            is PatternKind.Binding -> RefutabilityResult.Irrefutable

            // 错误模式：视为 irrefutable（避免级联错误）
            is PatternKind.Error -> RefutabilityResult.Irrefutable

            // 常量模式：可反驳（仅匹配特定值）
            is PatternKind.Const -> RefutabilityResult.Refutable(
                reason = "常量模式可能不匹配",
                patternDescription = kind.showString()
            )

            // 类型模式：可反驳（运行时类型检查可能失败）
            is PatternKind.Type -> RefutabilityResult.Refutable(
                reason = "类型模式可能不匹配",
                patternDescription = "${kind.name}: ${kind.type}"
            )

            // 元组模式：递归检查所有子模式
            is PatternKind.Tuple -> checkTuplePattern(kind)

            // 枚举模式：取决于枚举是否为单构造器
            is PatternKind.Enum -> checkEnumPattern(kind, type)
        }
    }

    /**
     * 检查元组模式的可反驳性
     *
     * 元组模式是 irrefutable 当且仅当所有子模式都是 irrefutable。
     */
    private fun checkTuplePattern(kind: PatternKind.Tuple): RefutabilityResult {
        for (subPattern in kind.subPatterns) {
            val result = check(subPattern)
            if (result is RefutabilityResult.Refutable) {
                return RefutabilityResult.Refutable(
                    reason = "元组中的子模式可能不匹配",
                    patternDescription = kind.showString()
                )
            }
        }
        return RefutabilityResult.Irrefutable
    }

    /**
     * 检查枚举模式的可反驳性
     *
     * 枚举模式是 irrefutable 当且仅当：
     * 1. 枚举只有一个构造器（单构造器枚举）
     * 2. 所有子模式都是 irrefutable
     */
    private fun checkEnumPattern(kind: PatternKind.Enum, type: CangJieType): RefutabilityResult {
        // 检查枚举是否为单构造器
        if (!isSingleConstructorEnum(kind.enum)) {
            return RefutabilityResult.Refutable(
                reason = "枚举模式可能不匹配其他变体",
                patternDescription = kind.showString()
            )
        }

        // 递归检查子模式
        for (subPattern in kind.subPatterns) {
            val result = check(subPattern)
            if (result is RefutabilityResult.Refutable) {
                return RefutabilityResult.Refutable(
                    reason = "枚举中的子模式可能不匹配",
                    patternDescription = kind.showString()
                )
            }
        }

        return RefutabilityResult.Irrefutable
    }

    /**
     * 检查枚举是否只有一个构造器
     *
     * @param enum 枚举的 PSI 元素
     * @return 如果枚举只有一个构造器则返回 true
     */
    private fun isSingleConstructorEnum(enum: CjEnum): Boolean {
        return enum.constructor.size == 1
    }

    /**
     * 检查类型是否为单构造器枚举类型
     *
     * @param type 要检查的类型
     * @return 如果是单构造器枚举则返回 true
     */
    fun isSingleConstructorEnumType(type: CangJieType): Boolean {
        if (!type.isEnum) return false
        val enumPsi = type.deccriptorClass?.source?.getPsi() as? CjEnum ?: return false
        return isSingleConstructorEnum(enumPsi)
    }


    /**
     * 便捷方法：检查模式的可反驳性
     */
    fun checkPattern(pattern: Pattern): RefutabilityResult {
        return check(pattern)
    }

    /**
     * 便捷方法：检查模式是否为 irrefutable
     */
    fun isIrrefutable(pattern: Pattern): Boolean {
        return check(pattern).isIrrefutable
    }

    /**
     * 便捷方法：检查模式是否为 refutable
     */
    fun isRefutable(pattern: Pattern): Boolean {
        return check(pattern).isRefutable
    }

}
