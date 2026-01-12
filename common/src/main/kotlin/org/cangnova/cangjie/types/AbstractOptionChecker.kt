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

package org.cangnova.cangjie.types

import org.cangnova.cangjie.types.model.*

/**
 * 抽象 Option 类型检查器
 *
 * 负责检查 Option 类型的子类型关系。
 * 在仓颉语言中，Option 类型使用 `?T` 语法表示可选值。
 *
 * 核心规则：
 * - T <: ?T  (非 Option 类型可以赋值给 Option 类型)
 * - ?T 不能赋值给 T (除非通过显式检查)
 * - ?T <: ?U 当且仅当 T <: U
 */
object AbstractOptionChecker {

    /**
     * 检查从 Option 角度看，subType 是否可能是 superType 的子类型
     *
     * 这是一个快速检查，用于在完整子类型检查之前过滤掉明显不可能的情况。
     *
     * ## 仓颉语言 Option 规则：
     * 1. T <: ?T  (任何类型都是其 Option 版本的子类型)
     * 2. ?T <: ?U 需要 T <: U
     * 3. ?T 不能是 T 的子类型（除非 T 本身是 Option）
     *
     * @param state 类型检查器状态
     * @param subType 子类型
     * @param superType 父类型
     * @return 如果从 Option 角度看可能存在子类型关系，返回 true；否则返回 false
     */
    fun isPossibleSubtype(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean = with(state.typeSystemContext) {
        // 1. 如果两个类型的 Option 标记相同，继续检查
        if (subType.isMarkedOption() == superType.isMarkedOption()) {
            return true
        }

        // 2. 如果 subType 不是 Option，superType 是 Option
        //    这是允许的：T <: ?T
        if (!subType.isMarkedOption() && superType.isMarkedOption()) {
            return true
        }

        // 3. 如果 subType 是 Option，superType 不是 Option
        //    这通常不允许，但有特殊情况：
        //    - subType 可能是 Nothing?，可以赋值给任何类型
        //    - superType 可能是 Any，Any 可以接受 Option 类型
        if (subType.isMarkedOption() && !superType.isMarkedOption()) {
            // Nothing? 可以赋值给任何类型（因为 Nothing 是所有类型的子类型）
            if (subType.typeConstructor().isNothingConstructor()) {
                return true
            }

            // Any 可以接受任何类型，包括 Option 类型
            if (superType.typeConstructor().isAnyConstructor()) {
                return true
            }

            // 其他情况：?T 不能赋值给 T
            return false
        }

        return true
    }

    /**
     * 检查两个类型是否具有相同的 Option 标记
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @return 如果两个类型都是 Option 或都不是 Option，返回 true
     */
    context(c: TypeSystemContext)
    fun hasEqualOption(a: SimpleTypeMarker, b: SimpleTypeMarker): Boolean {
        return a.isMarkedOption() == b.isMarkedOption()
    }

    /**
     * 检查子类型的 Option 标记是否与父类型兼容
     *
     * @param subType 子类型
     * @param superType 父类型
     * @return 如果 Option 标记兼容，返回 true
     */
    context(c: TypeSystemContext)
    fun isSubtypeOfByOption(subType: SimpleTypeMarker, superType: SimpleTypeMarker): Boolean {
        // 如果两者 Option 标记相同，总是兼容
        if (subType.isMarkedOption() == superType.isMarkedOption()) {
            return true
        }

        // T <: ?T (非 Option 可以赋值给 Option)
        if (!subType.isMarkedOption() && superType.isMarkedOption()) {
            return true
        }

        // ?T 不能赋值给 T
        return false
    }

    /**
     * 检查类型是否为 Any 的子类型
     *
     * 用于类型推断中判断类型是否可以安全地作为 Any 的子类型
     *
     * @param context 类型检查器提供上下文
     * @param type 要检查的类型
     * @return 如果类型是 Any 的子类型，返回 true
     */
    fun isSubtypeOfAny(context: TypeCheckerProviderContext, type: CangJieTypeMarker): Boolean {
        val state = context.newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true)
        return with(state.typeSystemContext) {
            // Option 类型不是 Any 的子类型（Option 类型可能包含 None）
            if (type.isMarkedOption()) {
                return@with false
            }

            // 检查类型是否为 Any 或 Any 的子类型
            type.typeConstructor().isAnyConstructor() ||
                    AbstractTypeChecker.isSubtypeOf(state, type, (this as TypeSystemBuiltInsContext).anyType())
        }
    }
}
