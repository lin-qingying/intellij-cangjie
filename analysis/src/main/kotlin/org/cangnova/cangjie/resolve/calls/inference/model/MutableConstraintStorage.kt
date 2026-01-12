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

package org.cangnova.cangjie.resolve.calls.inference.model

import org.cangnova.cangjie.resolve.calls.inference.ForkPointData
import org.cangnova.cangjie.resolve.calls.inference.components.ConstraintSystemUtilContext
import org.cangnova.cangjie.resolve.calls.tower.isSuccess
import org.cangnova.cangjie.types.model.*
import com.intellij.util.SmartList
import org.cangnova.cangjie.utils.trimToSize

private typealias Context = TypeSystemInferenceExtensionContext

class MutableVariableWithConstraints private constructor(
    private val context: Context,
    override val typeVariable: TypeVariableMarker,
    constraints: List<Constraint>? // assume simplified and deduplicated
) : VariableWithConstraints {
    constructor(context: Context, typeVariable: TypeVariableMarker) : this(context, typeVariable, null)

    constructor(context: Context, other: VariableWithConstraints) : this(context, other.typeVariable, other.constraints)

    private val mutableConstraints = if (constraints == null) SmartList() else SmartList(constraints)

    /**
     * The contract for mutating this list is that the only allowed mutation is appending items.
     * In any other case, it must be set to `null`, so that it will be recomputed when [constraints] is called.
     *
     * The reason is that the list might be mutated while it's being iterated.
     * For this reason, we use an index loop in
     * [org.cangnova.cangjie.resolve.calls.inference.components.ConstraintIncorporator.forEachConstraint].
     */
    private var simplifiedConstraints: SmartList<Constraint>? = mutableConstraints
    private fun SmartList<Constraint>.simplifyConstraints(): SmartList<Constraint> =
        simplifyLowerConstraints().simplifyEqualityConstraints()

    // see @OnlyInputTypes annotation
    fun getProjectedInputCallTypes(utilContext: ConstraintSystemUtilContext): Collection<Pair<CangJieTypeMarker, ConstraintKind>> {
        return with(utilContext) {
            mutableConstraints
                .mapNotNullTo(SmartList()) {
                    if (it.position.from is OnlyInputTypeConstraintPosition || it.inputTypePositionBeforeIncorporation != null)
                        it.type.unCapture() to it.kind
                    else null
                }
        }
    }

    private fun SmartList<Constraint>.simplifyEqualityConstraints(): SmartList<Constraint> {
        val equalityConstraints = filter { it.kind == ConstraintKind.EQUALITY }.groupBy { it.typeHashCode }
        return when {
            equalityConstraints.isEmpty() -> this
            else -> filterTo(SmartList()) { isUsefulConstraint(it, equalityConstraints) }
        }
    }

    private fun isUsefulConstraint(constraint: Constraint, equalityConstraints: Map<Int, List<Constraint>>): Boolean {
        if (constraint.kind == ConstraintKind.EQUALITY) return true
        return equalityConstraints[constraint.typeHashCode]?.none { it.type == constraint.type } ?: true
    }

    // Such constraint is applicable for simplification
    private fun Constraint.isLowerAndFlexibleTypeWithDefNotNullLowerBound(): Boolean {
        return with(context) {
            kind == ConstraintKind.LOWER && type.isFlexible() && !type.lowerBoundIfFlexible().isMarkedOption()
        }
    }

    // This method should be used only when constraint system has state COMPLETION
    internal fun removeConstrains(shouldRemove: (Constraint) -> Boolean) {
        mutableConstraints.removeAll(shouldRemove)
        if (simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints = null
        }
    }

    val rawConstraintsCount get() = mutableConstraints.size

    // This method should be used only for transaction in constraint system
    // shouldRemove should give true only for tail elements
    internal fun removeLastConstraints(sinceIndex: Int) {
        mutableConstraints.trimToSize(sinceIndex)
        if (simplifiedConstraints !== mutableConstraints) {
            simplifiedConstraints = null
        }
    }

    fun runConstraintsSimplification() {
        val currentState = constraints.toList()
        mutableConstraints.apply {
            clear()
            addAll(currentState)
        }
    }


    private fun SmartList<Constraint>.simplifyLowerConstraints(): SmartList<Constraint> {
        val usefulConstraints = SmartList<Constraint>()
        for (constraint in this) {
            if (!constraint.isLowerAndFlexibleTypeWithDefNotNullLowerBound()) {
                usefulConstraints.add(constraint)
                continue
            }

            // Now we have to check that some constraint T!!.T? <: K is useless or not
            // If there is constraint T..T? <: K, then the original one (T!!.T?) is useless
            // This is so because CST(T..T?, T!!..T?) == CST(T..T?)

            val thereIsStrongerConstraint =
                this.any { it.isStrongerThanLowerAndFlexibleTypeWithDefNotNullLowerBound(constraint) }

            if (!thereIsStrongerConstraint) {
                usefulConstraints.add(constraint)
            }
        }

        return usefulConstraints
    }

    private fun newConstraintIsUseless(old: Constraint, new: Constraint): Boolean {
        // Constraints from declared upper bound are quite special -- they aren't considered as a proper ones
        // In other words, user-defined constraints have "higher" priority and here we're trying not to loose them
        if (old.position.from is DeclaredUpperBoundConstraintPosition<*> && new.position.from !is DeclaredUpperBoundConstraintPosition<*>)
            return false

        /*
         * We discriminate upper expected type constraints during finding a result type to fix variable (see ResultTypeResolver.cj):
         * namely, we don't intersect the expected type with other upper constraints' types to prevent cases like this:
         *  fun <T : String> materialize(): T = null as T
         *  val bar: Int = materialize() // T is inferred into String & Int without discriminating upper expected type constraints
         * So here we shouldn't lose upper non-expected type constraints.
         */
//        if (old.position.from is ExpectedTypeConstraintPosition<*> && new.position.from !is ExpectedTypeConstraintPosition<*> && old.kind.isUpper() && new.kind.isUpper())
//            return false

        return when (old.kind) {
            ConstraintKind.EQUALITY -> true
            ConstraintKind.LOWER -> new.kind.isLower()
            ConstraintKind.UPPER -> new.kind.isUpper()
        }
    }

    /**
     * 添加一个新的约束到现有的约束集合中。
     * 该函数旨在通过将新约束与现有约束合并来简化现有约束，或直接添加新约束。
     * 简化在特定条件下发生，例如当新约束与现有约束类型相同且具有相同的可空性约束时。
     *
     * @param constraint 要添加的新约束。
     * @return 一个包含两个元素的Pair对象：
     *         - 第一个元素是最终添加或更新的约束。
     *         - 第二个元素是一个布尔值，表示是否添加了新的约束（true）或只是保留了现有约束（false）。
     */
    fun addConstraint(constraint: Constraint): Pair<Constraint, Boolean> {
        val isLowerAndFlexibleTypeWithDefNotNullLowerBound =
            constraint.isLowerAndFlexibleTypeWithDefNotNullLowerBound() // 检查新约束是否为具有默认非空下界的灵活类型

        for (previousConstraint in constraints) { // 遍历现有的约束集合
            if (previousConstraint.typeHashCode == constraint.typeHashCode // 检查类型哈希码是否相同
                && previousConstraint.type == constraint.type // 检查类型是否相同
                && previousConstraint.isNullabilityConstraint == constraint.isNullabilityConstraint // 检查可空性约束是否相同
            ) {
                val noNewCustomAttributes = with(context) { // 检查新约束和现有约束是否有相同的自定义属性
                    val previousType = previousConstraint.type
                    val type = constraint.type
                    (!previousType.hasCustomAttributes() && !type.hasCustomAttributes()) || // 两者都没有自定义属性
                            (previousType.getCustomAttributes() == type.getCustomAttributes()) // 两者的自定义属性相同
                }

                if (newConstraintIsUseless(previousConstraint, constraint)) { // 检查新约束是否无用
                    // 保留具有不同自定义类型属性的约束，以便在 CommonSuperTypeCalculator 中联合类型属性。
                    if (noNewCustomAttributes) { // 如果没有新的自定义属性
                        return previousConstraint to false // 返回现有约束，并标记为未添加新约束
                    }
                }

                val isMatchingForSimplification = when (previousConstraint.kind) { // 检查是否可以简化约束
                    ConstraintKind.LOWER -> constraint.kind.isUpper() // 现有约束为下界，新约束为上界
                    ConstraintKind.UPPER -> constraint.kind.isLower() // 现有约束为上界，新约束为下界
                    ConstraintKind.EQUALITY -> true // 现有约束为等式约束
                }
                if (isMatchingForSimplification && noNewCustomAttributes) { // 如果可以简化且没有新的自定义属性
                    val actualConstraint = if (constraint.kind != ConstraintKind.EQUALITY) { // 如果新约束不是等式约束
                        Constraint(
                            ConstraintKind.EQUALITY, // 创建一个新的等式约束
                            constraint.type,
                            constraint.position.takeIf { it.from !is DeclaredUpperBoundConstraintPosition<*> } // 使用新约束的位置，除非它是声明的上界位置
                                ?: previousConstraint.position, // 否则使用现有约束的位置
                            constraint.typeHashCode,
                            derivedFrom = constraint.derivedFrom,
                            isNullabilityConstraint = false
                        )
                    } else constraint // 如果新约束已经是等式约束，直接使用新约束
                    mutableConstraints.add(actualConstraint) // 将新的等式约束添加到可变约束集合中
                    simplifiedConstraints = null // 重置简化约束集合
                    return actualConstraint to true // 返回新的等式约束，并标记为已添加新约束
                }
            }

            if (isLowerAndFlexibleTypeWithDefNotNullLowerBound && // 如果新约束是具有默认非空下界的灵活类型
                previousConstraint.isStrongerThanLowerAndFlexibleTypeWithDefNotNullLowerBound(constraint) // 且现有约束更强
            ) {
                return previousConstraint to false // 返回现有约束，并标记为未添加新约束
            }
        }

        mutableConstraints.add(constraint) // 将新约束添加到可变约束集合中
        if (simplifiedConstraints != null && simplifiedConstraints !== mutableConstraints) { // 如果简化约束集合存在且不等于可变约束集合
            simplifiedConstraints!!.add(constraint) // 将新约束添加到简化约束集合中
        }

        if (simplifiedConstraints != null && isLowerAndFlexibleTypeWithDefNotNullLowerBound) { // 如果简化约束集合存在且新约束是具有默认非空下界的灵活类型
            simplifiedConstraints = null // 重置简化约束集合
        }

        return constraint to true // 返回新约束，并标记为已添加新约束
    }


    private fun Constraint.isStrongerThanLowerAndFlexibleTypeWithDefNotNullLowerBound(other: Constraint): Boolean {
        if (this === other) return false

        if (typeHashCode != other.typeHashCode || kind == ConstraintKind.UPPER) return false
        with(context) {
            if (!type.isFlexible() || !other.type.isFlexible()) return false
            val otherLowerBound = other.type.lowerBoundIfFlexible()
            if (otherLowerBound.isMarkedOption()) return false
            val thisLowerBound = type.lowerBoundIfFlexible()
            val thisUpperBound = type.upperBoundIfFlexible()
            val otherUpperBound = other.type.upperBoundIfFlexible()
            return thisLowerBound == otherLowerBound && thisUpperBound == otherUpperBound
        }
    }

    override val constraints: List<Constraint>
        get() {
            if (simplifiedConstraints == null) {
                simplifiedConstraints = mutableConstraints.simplifyConstraints()
            }
            return simplifiedConstraints!!
        }

    override fun toString(): String {
        return "Constraints for $typeVariable"
    }
}

internal class MutableConstraintStorage : ConstraintStorage {
    override val allTypeVariables: MutableMap<TypeConstructorMarker, TypeVariableMarker> = LinkedHashMap()
    override val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints> =
        LinkedHashMap()
    override val missedConstraints: MutableList<Pair<IncorporationConstraintPosition, MutableList<Pair<TypeVariableMarker, Constraint>>>> =
        SmartList()
    override val initialConstraints: MutableList<InitialConstraint> = SmartList()
    override var maxTypeDepthFromInitialConstraints: Int = 1
    override val errors: MutableList<ConstraintSystemError> = SmartList()
    override val hasContradiction: Boolean get() = errors.any { !it.applicability.isSuccess }
    override val fixedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker> = LinkedHashMap()
    override val postponedTypeVariables: MutableList<TypeVariableMarker> = SmartList()
    override val builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables: MutableMap<Pair<TypeConstructorMarker, List<Pair<TypeConstructorMarker, Int>>>, CangJieTypeMarker> =
        LinkedHashMap()
    override val builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker> =
        LinkedHashMap()

    override val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>> =
        SmartList()

    override var outerSystemVariablesPrefixSize: Int = 0

    override var usesOuterCs: Boolean = false

    //    @AssertionsOnly
    internal var outerCS: ConstraintStorage? = null
}

/**
 * Annotated member is used only for assertion purposes and does not affect semantics
 */
@RequiresOptIn
annotation class AssertionsOnly
