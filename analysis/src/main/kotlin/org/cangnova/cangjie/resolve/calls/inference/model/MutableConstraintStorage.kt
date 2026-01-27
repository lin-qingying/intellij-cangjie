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
import org.cangnova.cangjie.resolve.calls.inference.components.TypeApproximatorCachesPerConfiguration
import org.cangnova.cangjie.resolve.calls.inference.extractAllContainingTypeVariables
import org.cangnova.cangjie.utils.trimToSize
import java.util.IdentityHashMap
import java.util.LinkedHashMap

private typealias Context = TypeSystemInferenceExtensionContext

fun <T> identityHashSetFromSum(first: List<T>, second: List<T>): Set<T> =
    IdentityHashMap<T, Boolean>().apply {
        for (elem in first) {
            put(elem, true)
        }
        for (elem in second) {
            put(elem, true)
        }
    }.keys

/**
 * 可变类型变量与约束集合
 *
 * 这个类用于在类型推断过程中管理类型变量及其关联的约束。
 * 它维护两个约束列表：
 * 1. mutableConstraints - 原始约束集合,可以被修改
 * 2. simplifiedConstraints - 简化后的约束集合,用于优化查询性能
 *
 * 主要功能：
 * - 添加新约束时自动进行简化和去重
 * - 检测并合并相同类型的约束
 * - 支持约束的延迟简化以提高性能
 * - 提供约束的增删改查操作
 *
 * @property context 类型系统推断扩展上下文,提供类型相关操作
 * @property typeVariable 此约束集合关联的类型变量
 * @param constraints 初始约束列表(假定已简化和去重),可为 null
 */
class MutableVariableWithConstraints private constructor(
    private val context: Context,
    override val typeVariable: TypeVariableMarker,
    constraints: List<Constraint>? // assume simplified and deduplicated
) : VariableWithConstraints {
    /**
     * 构造函数 - 创建一个空的约束集合
     *
     * @param context 类型系统推断扩展上下文
     * @param typeVariable 要管理约束的类型变量
     */
    constructor(context: Context, typeVariable: TypeVariableMarker) : this(context, typeVariable, null)

    /**
     * 复制构造函数 - 从另一个 VariableWithConstraints 创建新实例
     *
     * @param context 类型系统推断扩展上下文
     * @param other 要复制的源变量约束
     */
    constructor(context: Context, other: VariableWithConstraints) : this(context, other.typeVariable, other.constraints)
    constructor(context:  Context, first: VariableWithConstraints, second: VariableWithConstraints) : this(
        context,
        first.typeVariable.also { require(it == second.typeVariable) },
        identityHashSetFromSum(first.constraints, second.constraints)
            .toList(),
    )

    /**
     * 可变约束集合 - 存储所有添加的原始约束
     * 使用 SmartList 以优化内存占用(小集合时使用数组,大集合时使用 ArrayList)
     */
    private val mutableConstraints = if (constraints == null) SmartList() else SmartList(constraints)

    /**
     * 简化后的约束集合 - 用于优化查询性能的缓存
     *
     * 变更契约：
     * - 唯一允许的变更操作是追加元素
     * - 其他任何情况下必须设置为 null,以便在调用 [constraints] 时重新计算
     *
     * 原因：
     * - 在迭代过程中列表可能会被修改
     * - 因此在 [org.cangnova.cangjie.resolve.calls.inference.components.ConstraintIncorporator.forEachConstraint]
     *   中使用索引循环而不是迭代器
     *
     * 简化规则：
     * 1. 移除被更强约束覆盖的弱约束
     * 2. 合并相同类型的约束
     * 3. 移除冗余的灵活类型下界约束
     */
    private var simplifiedConstraints: SmartList<Constraint>? = mutableConstraints

    /**
     * 简化约束集合 - 执行完整的约束简化流程
     *
     * 简化步骤：
     * 1. simplifyLowerConstraints() - 简化下界约束
     * 2. simplifyEqualityConstraints() - 简化等式约束
     *
     * @return 简化后的约束列表
     */
    private fun SmartList<Constraint>.simplifyConstraints(): SmartList<Constraint> =
        simplifyLowerConstraints().simplifyEqualityConstraints()

    /**
     * 获取投影的输入调用类型
     *
     * 此方法提取所有仅输入类型位置的约束类型,用于参数类型推断。
     * 主要用于从调用上下文中收集实参类型信息。
     *
     * 相关注解: @OnlyInputTypes - 标记仅用于输入位置的类型参数
     *
     * @param utilContext 约束系统工具上下文,提供类型操作辅助方法
     * @return 类型和约束种类的配对集合,每个配对代表一个输入类型约束
     *         - first: 去除捕获类型后的类型标记
     *         - second: 约束种类(LOWER/UPPER/EQUALITY)
     */
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

    /**
     * 简化等式约束
     *
     * 移除被等式约束覆盖的冗余上界和下界约束。
     * 例如: 如果有 T = Int, 则 T <: Int 和 T :> Int 都是冗余的。
     *
     * 算法：
     * 1. 收集所有等式约束并按类型哈希码分组
     * 2. 对于每个非等式约束,检查是否存在相同类型的等式约束
     * 3. 如果存在,该约束是冗余的,可以被移除
     *
     * @return 简化后的约束列表
     */
    private fun SmartList<Constraint>.simplifyEqualityConstraints(): SmartList<Constraint> {
        val equalityConstraints = filter { it.kind == ConstraintKind.EQUALITY }.groupBy { it.typeHashCode }
        return when {
            equalityConstraints.isEmpty() -> this
            else -> filterTo(SmartList()) { isUsefulConstraint(it, equalityConstraints) }
        }
    }

    /**
     * 判断约束是否有用(不被等式约束覆盖)
     *
     * @param constraint 要检查的约束
     * @param equalityConstraints 按类型哈希码分组的等式约束映射
     * @return true 如果约束有用(不冗余), false 如果约束被等式约束覆盖
     *
     * 判断规则：
     * - 等式约束总是有用的
     * - 对于非等式约束,如果存在相同类型的等式约束,则该约束无用
     */
    private fun isUsefulConstraint(constraint: Constraint, equalityConstraints: Map<Int, List<Constraint>>): Boolean {
        if (constraint.kind == ConstraintKind.EQUALITY) return true
        return equalityConstraints[constraint.typeHashCode]?.none { it.type == constraint.type } ?: true
    }

    /**
     * 检查约束是否为具有默认非空下界的灵活类型下界约束
     *
     * 灵活类型(Flexible Type)表示类型的范围,例如 Kotlin 的平台类型 String!
     * 可以表示为 String..String? (从 String 到 String?)。
     *
     * 此方法检查约束是否满足以下条件：
     * 1. 约束种类是 LOWER (下界约束)
     * 2. 约束类型是灵活类型
     * 3. 灵活类型的下界不是可选类型(Option)
     *
     * 这种约束适合进行简化,因为可能被更强的约束覆盖。
     *
     * @return true 如果约束是可简化的灵活类型下界约束
     */
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
     * A map that for a specified key (type constructor of a type variable) returns a collection of constraints that contains
     * the type variable.
     *
     * The property is necessary for the sake of optimizations only and expected to be nullified after any modifications [constraints]
     */
    private var constraintsGroupedByContainedTypeVariables: Map<TypeConstructorMarker, Collection<Constraint>>? = null

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



    private fun computeConstraintsGroupedByContainedTypeVariables(): Map<TypeConstructorMarker, Collection<Constraint>> = with(context) {
        buildMap<TypeConstructorMarker, MutableCollection<Constraint>> {
            for (constraint in constraints) {
                for (otherTypeVariable in constraint.type.extractAllContainingTypeVariables()) {
                    this.getOrPut(otherTypeVariable) { SmartList() }.add(constraint)
                }
            }
        }
    }
    override fun getConstraintsContainedSpecifiedTypeVariable(typeVariableConstructor: TypeConstructorMarker): Collection<Constraint> {
        if (constraintsGroupedByContainedTypeVariables == null) {
            constraintsGroupedByContainedTypeVariables = computeConstraintsGroupedByContainedTypeVariables()
        }

        return constraintsGroupedByContainedTypeVariables!![typeVariableConstructor] ?: emptyList()
    }
    override fun toString(): String {
        return "Constraints for $typeVariable"
    }
}

internal class MutableConstraintStorage : ConstraintStorage {
    override val allTypeVariables: MutableMap<TypeConstructorMarker, TypeVariableMarker> = LinkedHashMap()
    override val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints> =
        LinkedHashMap()
    override val typeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>> =
        LinkedHashMap()
    override val approximatorCaches: TypeApproximatorCachesPerConfiguration = mutableMapOf()

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
