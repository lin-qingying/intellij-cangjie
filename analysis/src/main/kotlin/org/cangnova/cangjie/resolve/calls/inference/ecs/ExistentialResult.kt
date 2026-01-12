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

package org.cangnova.cangjie.resolve.calls.inference.ecs

/**
 * 存在性判断结果
 *
 * ECS 的求解结果，只有两种可能：
 * - [Satisfiable]: 存在满足条件的代换
 * - [Unsatisfiable]: 不存在满足条件的代换
 *
 * ## 与类型推导的区别
 *
 * 类型推导系统返回具体的类型绑定（如 T = Int64），
 * 而 ECS 只返回 YES/NO，不关心具体是什么代换。
 */
sealed class ExistentialResult {

    /**
     * 是否可满足
     */
    abstract val isSatisfiable: Boolean

    /**
     * 可满足
     *
     * 表示存在至少一个类型代换使所有约束成立。
     * 注意：ECS 不关心具体是哪个代换。
     */
    object Satisfiable : ExistentialResult() {
        override val isSatisfiable: Boolean = true
        override fun toString(): String = "∃ (satisfiable)"
    }

    /**
     * 不可满足
     *
     * 表示不存在任何类型代换能使所有约束同时成立。
     *
     * @property contradictions 导致不可满足的矛盾约束对（用于调试）
     */
    data class Unsatisfiable(
        val contradictions: List<Contradiction> = emptyList()
    ) : ExistentialResult() {
        override val isSatisfiable: Boolean = false
        override fun toString(): String = "∄ (unsatisfiable): $contradictions"
    }

    companion object {
        /** 便捷方法：创建可满足结果 */
        val YES: ExistentialResult = Satisfiable

        /** 便捷方法：创建不可满足结果 */
        val NO: ExistentialResult = Unsatisfiable()

        /** 根据布尔值创建结果 */
        fun of(satisfiable: Boolean): ExistentialResult =
            if (satisfiable) Satisfiable else Unsatisfiable()
    }
}

/**
 * 约束矛盾
 *
 * 描述两个不兼容的约束，导致约束系统不可满足。
 *
 * @property constraint1 第一个约束
 * @property constraint2 第二个冲突的约束
 * @property reason 冲突原因描述
 */
data class Contradiction(
    val constraint1: ExistentialConstraint,
    val constraint2: ExistentialConstraint,
    val reason: String = ""
) {
    override fun toString(): String = buildString {
        append("[$constraint1] conflicts with [$constraint2]")
        if (reason.isNotEmpty()) {
            append(": $reason")
        }
    }
}

/**
 * 特异性比较结果
 *
 * 用于比较两个签名的特异性关系。
 */
enum class SpecificityResult {
    /** A 比 B 更具体 */
    MORE_SPECIFIC,

    /** A 和 B 同样具体 */
    EQUALLY_SPECIFIC,

    /** A 比 B 更不具体 */
    LESS_SPECIFIC,

    /** A 和 B 无法比较（不相关） */
    INCOMPARABLE;

    /**
     * 是否不比对方更不具体（即至少一样具体或更具体）
     */
    fun isNotLessSpecific(): Boolean = this == MORE_SPECIFIC || this == EQUALLY_SPECIFIC
}

/**
 * 重载检查结果
 *
 * 判断两个函数声明是否可以重载（共存）。
 */
enum class OverloadabilityResult {
    /** 可以重载（签名可区分） */
    OVERLOADABLE,

    /** 不能重载（签名冲突） */
    CONFLICTING;

    val canOverload: Boolean
        get() = this == OVERLOADABLE
}
