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

import org.cangnova.cangjie.types.model.CangJieTypeMarker

/**
 * 存在性约束
 *
 * ECS 中的约束是轻量级的，只包含类型关系信息，不包含位置、来源等调试信息。
 * 这是因为 ECS 只关心"是否存在满足条件的代换"，不需要报告详细错误。
 *
 * ## 约束类型
 *
 * - [Subtype]: 子类型约束 `lower <: upper`
 * - [Equality]: 相等约束 `left = right`
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 创建子类型约束: T <: String
 * val constraint = ExistentialConstraint.Subtype(typeVarT, stringType)
 *
 * // 创建相等约束: T = Int64
 * val eqConstraint = ExistentialConstraint.Equality(typeVarT, int64Type)
 * ```
 */
sealed class ExistentialConstraint {

    /**
     * 子类型约束
     *
     * 表示 `lower <: upper`，即 lower 是 upper 的子类型。
     *
     * @property lower 子类型（下界）
     * @property upper 父类型（上界）
     */
    data class Subtype(
        val lower: CangJieTypeMarker,
        val upper: CangJieTypeMarker
    ) : ExistentialConstraint() {
        override fun toString(): String = "$lower <: $upper"
    }

    /**
     * 相等约束
     *
     * 表示 `left = right`，即两个类型必须相等。
     *
     * @property left 左侧类型
     * @property right 右侧类型
     */
    data class Equality(
        val left: CangJieTypeMarker,
        val right: CangJieTypeMarker
    ) : ExistentialConstraint() {
        override fun toString(): String = "$left = $right"
    }

    /**
     * 将约束转换为子类型约束列表
     *
     * - [Subtype] 直接返回
     * - [Equality] 转换为双向子类型约束
     */
    fun toSubtypeConstraints(): List<Subtype> = when (this) {
        is Subtype -> listOf(this)
        is Equality -> listOf(
            Subtype(left, right),
            Subtype(right, left)
        )
    }
}

/**
 * 约束种类枚举
 *
 * 对应 [ExistentialConstraint] 的类型，用于约束分类和处理。
 */
enum class ExistentialConstraintKind {
    /** 子类型约束 */
    SUBTYPE,

    /** 相等约束 */
    EQUALITY;

    companion object {
        fun fromConstraint(constraint: ExistentialConstraint): ExistentialConstraintKind =
            when (constraint) {
                is ExistentialConstraint.Subtype -> SUBTYPE
                is ExistentialConstraint.Equality -> EQUALITY
            }
    }
}
