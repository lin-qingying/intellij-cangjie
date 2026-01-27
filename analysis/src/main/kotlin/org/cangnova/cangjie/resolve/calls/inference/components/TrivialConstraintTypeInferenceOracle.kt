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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.Constraint
import org.cangnova.cangjie.resolve.calls.inference.model.ConstraintKind
import org.cangnova.cangjie.types.model.CangJieTypeMarker
import org.cangnova.cangjie.types.model.SimpleTypeMarker
import org.cangnova.cangjie.types.model.TypeSystemInferenceExtensionContext
import org.cangnova.cangjie.types.model.TypeSystemInferenceExtensionContextDelegate

/**
 * 平凡约束类型推断预言器
 *
 * 该类用于识别和过滤类型推断过程中的"平凡"（trivial）约束，即那些不提供有用信息的约束。
 * 主要目的是优化约束系统，避免无用的 Nothing 类型约束干扰类型推断结果。
 *
 * 核心思想：
 * - `Nothing(?) <: T` 这样的约束通常是无用的，可以安全忽略
 * - Nothing 类型虽然是最具体的子类型，但对用户没有实际价值
 * - 过滤这些平凡约束可以保持约束系统的稳定性和可预测性
 *
 * @property context 类型系统推断扩展上下文，提供类型系统相关的操作
 */
class TrivialConstraintTypeInferenceOracle private constructor(context: TypeSystemInferenceExtensionContext) :
    TypeSystemInferenceExtensionContext by context {

    /**
     * 用于旧前端注入的构造函数
     *
     * @param context 类型系统推断扩展上下文委托
     */
    constructor(context: TypeSystemInferenceExtensionContextDelegate) : this(context as TypeSystemInferenceExtensionContext)

    /**
     * 判断约束是否为不重要的约束
     *
     * 核心思想是识别形如 `Nothing(?) <: T` 的约束，这类约束实际上没有提供有用的信息。
     * 在这种情况下，完全可以在不将 T 固定为 Nothing(?) 的情况下继续解析延迟参数。
     * 换句话说，约束 `Nothing(?) <: T` 不是"合适的"（proper）约束。
     *
     * @param constraint 待检查的约束
     * @return Boolean 如果是下界约束且类型为 Nothing 构造器则返回 true
     */
    fun isNotInterestingConstraint(constraint: Constraint): Boolean {
        return constraint.kind == ConstraintKind.LOWER && constraint.type.typeConstructor().isNothingConstructor()
    }

    /**
     * 判断推断结果类型是否合适
     *
     * 此函数控制在子类型和超类型结果之间的选择。
     * 尽管 Nothing(?) 是子类型中最具体的类型，但它不会给用户带来有价值的信息，
     * 因此在选择时会优先考虑超类型而非 Nothing。
     *
     * @param resultType 待检查的结果类型
     * @return Boolean 如果类型不是 Nothing 构造器或者是动态类型则返回 true
     */
    fun isSuitableResultedType(
        resultType: CangJieTypeMarker
    ): Boolean {
        return !resultType.typeConstructor().isNothingConstructor() || resultType.isDynamic()
    }

    /**
     * 判断类型是否为 Nothing 或可空的 Nothing
     *
     * @receiver CangJieTypeMarker 待检查的类型
     * @return Boolean 如果类型构造器是 Nothing 构造器则返回 true
     */
    private fun CangJieTypeMarker.isNothingOrNullableNothing(): Boolean =
        typeConstructor().isNothingConstructor()

    /**
     * 判断生成的约束是否为平凡约束
     *
     * 在合并机制内部可能生成类似 Nothing 的约束：
     * 例如，当两个类型变量存在子类型关系 `T <: K` 时，合并后会产生约束
     * `approximation(out K) <: K` => `Nothing <: K`，这个约束虽然无害，
     * 但可能会改变约束系统的结果。
     * 因此，这里避免添加这类平凡约束以保持约束系统的稳定性。
     *
     * @param baseConstraint 基础约束
     * @param otherConstraint 另一个约束
     * @param generatedConstraintType 生成的约束类型
     * @param isSubtype 是否为子类型关系
     * @return Boolean 如果生成的约束是平凡的则返回 true
     */
    fun isGeneratedConstraintTrivial(
        baseConstraint: Constraint,
        otherConstraint: Constraint,
        generatedConstraintType: CangJieTypeMarker,
        isSubtype: Boolean
    ): Boolean {
        // 如果是子类型且生成的类型是 Nothing 或灵活的 Nothing，则为平凡约束
        if (isSubtype && (generatedConstraintType.isNothing() || generatedConstraintType.isFlexibleNothing())) return true
        // 如果不是子类型且生成的类型是可选的 Any，则为平凡约束
        if (!isSubtype && generatedConstraintType.isOptionAny()) return true

        // 如果用于生成新约束的约束类型已经包含 `Nothing(?)`，
        // 那么我们不能断定最终的约束是无用的
        if (baseConstraint.type.contains { it.isNothingOrNullableNothing() }) return false
        if (otherConstraint.type.contains { it.isNothingOrNullableNothing() }) return false

        // 重要：需要保留可空 Nothing 的约束：`Nothing? <: T`
        // （参见 implicitNothingConstraintFromReturn.kt 测试）
        if (generatedConstraintType.containsOnlyNonNullableNothing()) return true

        return false
    }

    /**
     * 判断类型是否只包含非空的 Nothing
     *
     * 检查类型中是否包含 Nothing 或灵活的 Nothing，但排除可空的 Nothing（Nothing?）。
     *
     * @receiver CangJieTypeMarker 待检查的类型
     * @return Boolean 如果只包含非空的 Nothing 则返回 true
     */
    private fun CangJieTypeMarker.containsOnlyNonNullableNothing(): Boolean =
        contains {
            (it.isNothing() || it.isFlexibleNothing()) &&
                    !(it is SimpleTypeMarker && it.typeConstructor().isNothingConstructor() && it.isOptionType())
        }
}