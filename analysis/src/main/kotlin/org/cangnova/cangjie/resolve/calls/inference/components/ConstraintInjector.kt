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

package org.cangnova.cangjie.resolve.calls.inference.components

import com.intellij.util.SmartList
import org.cangnova.cangjie.config.LanguageFeature
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.inference.ConstraintSystemOperation
import org.cangnova.cangjie.resolve.calls.inference.ForkPointBranchDescription
import org.cangnova.cangjie.resolve.calls.inference.ForkPointData
import org.cangnova.cangjie.resolve.calls.inference.model.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.types.TypeCheckerState
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.model.*
import org.cangnova.cangjie.utils.addIfNotNull
import org.cangnova.cangjie.utils.popLast
import kotlin.math.max

/**
 * 获取仓颉类型的类型构造器
 *
 * @param context 类型系统上下文
 * @return 类型构造器标记
 */
fun CangJieTypeMarker.typeConstructor(context: TypeSystemContext): TypeConstructorMarker =
    with(context) { typeConstructor() }

/**
 * 约束注入器
 *
 * 负责将类型约束注入到约束系统中，并通过约束合并器进行处理。
 * 这是类型推断系统的核心组件之一，用于管理约束的添加、处理和合并。
 *
 * 主要功能：
 * - 处理初始约束（相等性约束和子类型约束）
 * - 管理约束的合并和传播
 * - 处理分支点（fork point）约束
 * - 维护类型深度限制以避免无限递归
 * - 处理错过的约束和错误报告
 *
 * @property constraintIncorporator 约束合并器，用于合并和处理约束
 * @property typeApproximator 类型近似器，用于类型近似计算
 * @property languageVersionSettings 语言版本设置，控制特性开关
 */
class ConstraintInjector(
    val constraintIncorporator: ConstraintIncorporator,
    val typeApproximator: AbstractTypeApproximator,
    private val languageVersionSettings: LanguageVersionSettings,
) {
    /**
     * 约束合并时允许的最大类型深度增量
     *
     * 用于防止类型深度在约束合并过程中无限增长
     */
    private val ALLOWED_DEPTH_DELTA_FOR_INCORPORATION = 1

    /**
     * 处理给定分支点的约束
     *
     * 当约束系统在分支点（fork point）选择某个分支后，
     * 需要处理该分支产生的所有约束。
     *
     * @param c 约束系统上下文
     * @param constraintSet 该分支产生的约束集合
     * @param position 约束合并位置
     */
    fun processGivenForkPointBranchConstraints(
        c: Context,
        constraintSet: Collection<Pair<TypeVariableMarker, Constraint>>,
        position: IncorporationConstraintPosition
    ) {
        processGivenConstraints(
            c,
            TypeCheckerStateForConstraintInjector(c, position),
            constraintSet,
        )
    }

    /**
     * 处理错过的约束
     *
     * 在旧的约束处理系统中，某些约束可能被错误的优化跳过。
     * 此方法用于补充处理这些被错过的约束。
     *
     * 注意：当启用正确的约束处理特性时，此方法直接返回，
     * 因为新系统不会产生错过的约束。
     *
     * @param c 约束系统上下文
     * @param position 约束合并位置
     * @param missedConstraints 被错过的约束列表
     */
    fun processMissedConstraints(
        c: Context,
        position: IncorporationConstraintPosition,
        missedConstraints: List<Pair<TypeVariableMarker, Constraint>>
    ) {
        // 默认启用：使用正确的类型推断约束处理
        val properConstraintsProcessingEnabled = true

        // 如果启用了正确的约束处理，则不会有错过的约束
        if (properConstraintsProcessingEnabled) return

        val typeCheckerState = TypeCheckerStateForConstraintInjector(c, position)
        for ((variable, constraint) in missedConstraints) {
            typeCheckerState.addPossibleNewConstraint(variable, constraint)
        }
        processConstraints(c, typeCheckerState, skipProperEqualityConstraints = false)
    }

    /**
     * 通过子类型关系添加初始相等性约束
     *
     * 对于某些类型（如可选类型），相等性约束通过双向子类型约束实现：
     * - 添加 a <: b
     * - 添加 b <: a
     *
     * 这确保了类型的完全相等性。
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @param typeCheckerState 类型检查器状态
     */
    private fun Context.addInitialEqualityConstraintThroughSubtyping(
        a: CangJieTypeMarker,
        b: CangJieTypeMarker,
        typeCheckerState: TypeCheckerStateForConstraintInjector
    ) {
        updateAllowedTypeDepth(this, a)
        updateAllowedTypeDepth(this, b)
        addSubTypeConstraintAndIncorporateIt(this, a, b, typeCheckerState)
        addSubTypeConstraintAndIncorporateIt(this, b, a, typeCheckerState)
    }

    /**
     * 添加初始相等性约束
     *
     * 当需要约束两个类型相等时（如 T == Int），此方法将创建并添加相等性约束。
     *
     * 处理流程：
     * 1. 确定哪个是类型变量，哪个是具体类型
     * 2. 创建初始约束并记录
     * 3. 根据类型特性选择合适的约束添加方式：
     *    - 对于可选类型或复杂类型，使用双向子类型约束
     *    - 对于简单类型，直接添加相等性约束
     *
     * @param c 约束系统上下文
     * @param a 第一个类型（可能是类型变量）
     * @param b 第二个类型（可能是类型变量）
     * @param position 约束位置
     */
    fun addInitialEqualityConstraint(
        c: Context,
        a: CangJieTypeMarker,
        b: CangJieTypeMarker,
        position: ConstraintPosition
    ) = with(c) {
        // 确定哪个是类型变量，哪个是等式右边的类型
        val (typeVariable, equalType) = when {
            a.typeConstructor(c) is TypeVariableTypeConstructorMarker -> a to b
            b.typeConstructor(c) is TypeVariableTypeConstructorMarker -> b to a
            else -> return
        }

        // 创建并记录初始约束
        val initialConstraint = InitialConstraint(typeVariable, equalType, ConstraintKind.EQUALITY, position).also {
            c.addInitialConstraint(it)
        }
        val typeCheckerState =
            TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))

        // 对于非简单类型或可选类型，使用旧的方式（通过子类型添加约束）
        if (!typeVariable.isSimpleType() || typeVariable.isOptionType()) {
            addInitialEqualityConstraintThroughSubtyping(typeVariable, equalType, typeCheckerState)
            return
        }

        // 对于简单类型，直接添加相等性约束
        updateAllowedTypeDepth(c, equalType)
        addEqualityConstraintAndIncorporateIt(c, typeVariable, equalType, typeCheckerState)
    }

    /**
     * 添加相等性约束并合并
     *
     * 将相等性约束添加到类型检查器状态中，然后处理所有产生的约束。
     *
     * @param c 约束系统上下文
     * @param typeVariable 类型变量
     * @param equalType 与类型变量相等的类型
     * @param typeCheckerState 类型检查器状态
     */
    private fun addEqualityConstraintAndIncorporateIt(
        c: Context,
        typeVariable: CangJieTypeMarker,
        equalType: CangJieTypeMarker,
        typeCheckerState: TypeCheckerStateForConstraintInjector
    ) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(typeVariable, equalType)
        typeCheckerState.addEqualityConstraint(typeVariable.typeConstructor(c), equalType)

        // 错过的约束是由于错误的优化而在约束处理器中跳过的约束
        val missedConstraints = processConstraints(c, typeCheckerState)

        if (missedConstraints != null) {
            c.addMissedConstraints(typeCheckerState.position, missedConstraints)
        }
    }

    /**
     * 更新允许的类型深度
     *
     * 跟踪初始约束中出现的最大类型深度，用于限制约束合并过程中的类型深度。
     *
     * @param c 约束系统上下文
     * @param initialType 初始类型
     */
    private fun updateAllowedTypeDepth(c: Context, initialType: CangJieTypeMarker) = with(c) {
        c.maxTypeDepthFromInitialConstraints = max(c.maxTypeDepthFromInitialConstraints, initialType.typeDepth())
    }

    /**
     * 处理约束
     *
     * 处理类型检查器状态中的所有待处理约束。
     * 此方法会提取分支点数据并在完成状态下立即解析分支点约束。
     *
     * @param c 约束系统上下文
     * @param typeCheckerState 类型检查器状态
     * @param skipProperEqualityConstraints 是否跳过正确的相等性约束（默认为 true）
     * @return 被错过的约束列表，如果没有则返回 null
     */
    private fun processConstraints(
        c: Context,
        typeCheckerState: TypeCheckerStateForConstraintInjector,
        skipProperEqualityConstraints: Boolean = true
    ): MutableList<Pair<TypeVariableMarker, Constraint>>? {
        return processConstraintsIgnoringForksData(typeCheckerState, c, skipProperEqualityConstraints).also {
            // 提取所有分支点数据
            typeCheckerState.extractForkPointsData()?.let { allForkPointsData ->
                allForkPointsData.mapTo(c.constraintsFromAllForkPoints) { forkPointData ->
                    typeCheckerState.position to forkPointData
                }

                // 在完成阶段，我们立即开始处理分支约束
                if (c.atCompletionState) {
                    c.resolveForkPointsConstraints()
                }
            }
        }
    }

    /**
     * 判断是否应该跳过约束
     *
     * 某些约束是冗余的或已隐含的，应该被跳过以提高效率。
     *
     * 跳过的约束包括：
     * 1. T <: T 或 T? <: T! 等自反约束（除了 T? <: T 的特殊情况）
     * 2. T <: Any? 这样的上界约束（Any? 是所有类型的超类型）
     *
     * @param typeVariable 类型变量
     * @param constraint 约束
     * @return true 如果应该跳过此约束
     */
    private fun Context.shouldWeSkipConstraint(typeVariable: TypeVariableMarker, constraint: Constraint): Boolean {
        // 不跳过相等性约束
        if (constraint.kind == ConstraintKind.EQUALITY)
            return false

        val constraintType = constraint.type

        // 检查是否是自反约束（类型变量约束其自身）
        if (constraintType.typeConstructor() == typeVariable.freshTypeConstructor()) {
            // T? <: T 不应该跳过
            if (constraintType.lowerBoundIfFlexible()
                    .isOptionType() && constraint.kind == ConstraintKind.LOWER
            ) return false

            return true // T <: T(?!) 应该跳过
        }

        // 跳过 T <: Any? 这样的上界约束
        if (constraint.position.from is DeclaredUpperBoundConstraintPosition<*> &&
            constraint.kind == ConstraintKind.UPPER && constraintType.isOptionAny()
        ) {
            return true
        }

        return false
    }


    /**
     * 处理给定的类型约束。
     *
     * 该函数遍历类型约束集合，根据条件决定是否跳过每个约束。对于未被跳过的约束，将其添加到相应的类型变量约束集中，并在必要时将其合并到类型检查器状态中。
     *
     * @param c 上下文对象，用于提供类型检查所需的信息和操作。
     * @param typeCheckerState 类型检查器的状态，用于存储和管理类型变量及其约束。
     * @param constraintsToProcess 需要处理的类型约束集合，每个约束包含一个类型变量和一个约束。
     */
    private fun processGivenConstraints(
        c: Context,
        typeCheckerState: TypeCheckerStateForConstraintInjector,
        constraintsToProcess: Collection<Pair<TypeVariableMarker, Constraint>>
    ) {
        for ((typeVariable, constraint) in constraintsToProcess) {
            if (c.shouldWeSkipConstraint(typeVariable, constraint)) continue

            val constraints =
                c.notFixedTypeVariables[typeVariable.freshTypeConstructor(c)] ?: typeCheckerState.fixedTypeVariable(
                    typeVariable
                )

            // 在此处添加约束，因为合并过程中会读取这些约束
            val (addedOrNonRedundantExistedConstraint, wasAdded) = constraints.addConstraint(constraint)
            val positionFrom = constraint.position.from
            val constraintToIncorporate = when {
                wasAdded && !constraint.isNullabilityConstraint -> addedOrNonRedundantExistedConstraint
                positionFrom is FixVariableConstraintPosition<*> && positionFrom.variable == typeVariable && constraint.kind == ConstraintKind.EQUALITY ->
                    addedOrNonRedundantExistedConstraint

                else -> null
            }

            if (constraintToIncorporate != null) {
                constraintIncorporator.incorporate(typeCheckerState, typeVariable, constraintToIncorporate)
            }
        }
    }


    /**
     * 处理类型约束，忽略来自分支的约束数据。
     *
     * 该函数旨在处理类型约束，同时忽略那些从分支生成的约束，以避免不正确的优化。
     *
     * @param typeCheckerState 类型检查器状态对象，用于约束注入。
     * @param c 上下文对象，用于约束系统操作。
     * @param skipProperEqualityConstraints 是否跳过正确的相等性约束。
     * @return 处理后的约束列表，如果没有任何约束需要处理则返回null。
     */
    private fun processConstraintsIgnoringForksData(
        typeCheckerState: TypeCheckerStateForConstraintInjector,
        c: Context,
        skipProperEqualityConstraints: Boolean
    ): MutableList<Pair<TypeVariableMarker, Constraint>>? {
        // 检查语言版本是否支持正确的类型推断约束处理
        val properConstraintsProcessingEnabled = true
//            languageVersionSettings.supportsFeature(LanguageFeature.ProperTypeInferenceConstraintsProcessing)

        while (typeCheckerState.hasConstraintsToProcess()) {
            // 处理当前的所有约束
            processGivenConstraints(c, typeCheckerState, typeCheckerState.extractAllConstraints()!!)

            val contextOps = c as? ConstraintSystemOperation

            // 判断是否使用不正确的优化
            val useIncorrectOptimization = skipProperEqualityConstraints && !properConstraintsProcessingEnabled

            if (!useIncorrectOptimization) continue

            // 检查每个类型变量是否有正确的相等性约束
            val hasProperEqualityConstraintForEachVariable =
                contextOps != null && c.notFixedTypeVariables.all { typeVariable ->
                    typeVariable.value.constraints.any { constraint ->
                        constraint.kind == ConstraintKind.EQUALITY && contextOps.isProperType(constraint.type)
                    }
                }

            // 如果每个类型变量都有正确的相等性约束，则返回所有约束
            if (hasProperEqualityConstraintForEachVariable) return typeCheckerState.extractAllConstraints()
        }
        // 如果没有需要处理的约束，返回null
        return null
    }


    /**
     * 添加子类型约束并合并
     *
     * 将子类型约束添加到类型检查器状态，然后处理所有产生的约束。
     *
     * @param c 约束系统上下文
     * @param lowerType 子类型（下界）
     * @param upperType 超类型（上界）
     * @param typeCheckerState 类型检查器状态
     */
    private fun addSubTypeConstraintAndIncorporateIt(
        c: Context,
        lowerType: CangJieTypeMarker,
        upperType: CangJieTypeMarker,
        typeCheckerState: TypeCheckerStateForConstraintInjector
    ) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(lowerType, upperType)
        typeCheckerState.runIsSubtypeOf(lowerType, upperType)

        // 错过的约束是由于错误的优化而在约束处理器中跳过的约束
        val missedConstraints = processConstraints(c, typeCheckerState)

        if (missedConstraints != null) {
            c.addMissedConstraints(typeCheckerState.position, missedConstraints)
        }
    }

    /**
     * 添加初始子类型约束
     *
     * 当需要添加子类型关系约束时（如 T <: Number），此方法创建并处理该约束。
     *
     * @param c 约束系统上下文
     * @param lowerType 子类型
     * @param upperType 超类型
     * @param position 约束位置
     */
    fun addInitialSubtypeConstraint(
        c: Context,
        lowerType: CangJieTypeMarker,
        upperType: CangJieTypeMarker,
        position: ConstraintPosition
    ) {
        val initialConstraint =
            InitialConstraint(lowerType, upperType, ConstraintKind.UPPER, position).also { c.addInitialConstraint(it) }
        val typeCheckerState =
            TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))

        updateAllowedTypeDepth(c, lowerType)
        updateAllowedTypeDepth(c, upperType)

        addSubTypeConstraintAndIncorporateIt(c, lowerType, upperType, typeCheckerState)
    }

    /**
     * 约束注入器的上下文接口
     *
     * 定义了约束注入器所需的所有操作接口。
     * 实现此接口的类通常是约束系统的核心实现类。
     */
    interface Context : TypeSystemInferenceExtensionContext {
        /** 所有类型变量的映射（从类型构造器到类型变量） */
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        /** 从初始约束中得到的最大类型深度 */
        var maxTypeDepthFromInitialConstraints: Int

        /** 未固定的类型变量及其约束 */
        val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints>

        /** 已固定的类型变量及其确定的类型 */
        val fixedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker>

        /** 所有分支点的约束数据 */
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>

        /** 是否处于完成状态 */
        val atCompletionState: Boolean

        /** 添加初始约束 */
        fun addInitialConstraint(initialConstraint: InitialConstraint)

        /** 添加错误 */
        fun addError(error: ConstraintSystemError)

        /** 添加错过的约束 */
        fun addMissedConstraints(
            position: IncorporationConstraintPosition,
            constraints: MutableList<Pair<TypeVariableMarker, Constraint>>
        )

        /** 解析分支点约束 */
        fun resolveForkPointsConstraints()
    }


    /**
     * 类型检查器状态（用于约束注入器）
     *
     * 这是一个特殊的类型检查器状态实现，专门用于约束注入过程。
     * 它扩展了标准的类型检查器状态，添加了约束收集和分支点处理功能。
     *
     * 主要职责：
     * 1. 收集在类型检查过程中产生的新约束
     * 2. 管理分支点（fork point）的约束集合
     * 3. 处理约束的合并和传播
     * 4. 检测并报告类型错误
     *
     * @property c 约束系统上下文
     * @property position 约束合并位置，用于追踪约束来源
     */
    private inner class TypeCheckerStateForConstraintInjector(
        baseState: TypeCheckerState,
        val c: Context,
        val position: IncorporationConstraintPosition
    ) : TypeCheckerStateForConstraintSystem(
        c,
        baseState.cangjieTypePreparator,
        baseState.cangjieTypeRefiner
    ), ConstraintIncorporator.Context, TypeSystemInferenceExtensionContext by c {
        /**
         * 便捷构造函数，自动创建类型检查器状态
         *
         * @param c 约束系统上下文
         * @param position 约束合并位置
         */
        constructor(c: Context, position: IncorporationConstraintPosition) : this(
            c.newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true),
            c,
            position
        )

        /**
         * 可能产生的新约束列表
         *
         * 使用 var 是为了避免额外的内存分配，因为这个属性访问频繁
         */
        private var possibleNewConstraints: MutableList<Pair<TypeVariableMarker, Constraint>>? = null

        /** 分支点数据列表 */
        private var forkPointsData: MutableList<ForkPointData>? = null

        /** 当前分支点的约束集合栈 */
        private var stackForConstraintsSetsFromCurrentForkPoint: Stack<MutableList<ForkPointBranchDescription>>? = null

        /** 当前分支点分支的约束集合栈 */
        private var stackForConstraintSetFromCurrentForkPointBranch: Stack<MutableList<Pair<TypeVariableMarker, Constraint>>>? =
            null

        /** 是否允许分支（fork） */
        private val allowForking: Boolean
            get() = constraintIncorporator.utilContext.isForcedAllowForkingInferenceSystem

        /** 基础下界类型（用于调试信息） */
        private var baseLowerType = position.initialConstraint.a

        /** 基础上界类型（用于调试信息） */
        private var baseUpperType = position.initialConstraint.b

        /** 是否正在合并来自声明上界的约束 */
        private var isIncorporatingConstraintFromDeclaredUpperBound = false

        /**
         * 提取所有约束并清空约束列表
         *
         * @return 当前收集的所有约束，提取后原列表被置空
         */
        fun extractAllConstraints() = possibleNewConstraints.also { possibleNewConstraints = null }

        /**
         * 提取所有分支点数据并清空列表
         *
         * @return 当前收集的所有分支点数据，提取后原列表被置空
         */
        fun extractForkPointsData() = forkPointsData.also { forkPointsData = null }

        /**
         * 添加可能的新约束
         *
         * 根据当前是否在分支点内部，将约束添加到相应的位置：
         * - 如果在分支点内部，添加到当前分支的约束集合
         * - 否则添加到全局的约束列表
         *
         * @param variable 类型变量
         * @param constraint 约束
         */
        fun addPossibleNewConstraint(variable: TypeVariableMarker, constraint: Constraint) {
            val constraintsSetsFromCurrentFork = stackForConstraintsSetsFromCurrentForkPoint?.lastOrNull()
            if (constraintsSetsFromCurrentFork != null) {
                val currentConstraintSetForForkPointBranch =
                    stackForConstraintSetFromCurrentForkPointBranch?.lastOrNull()
                require(currentConstraintSetForForkPointBranch != null) { "Constraint has been added not under fork {...} call " }
                currentConstraintSetForForkPointBranch.add(variable to constraint)
                return
            }

            if (possibleNewConstraints == null) {
                possibleNewConstraints = SmartList()
            }
//            TODO 排除掉来自扩展的约束，因为那并不一定来字扩展
//            if (constraint.position.from is ReceiverConstraintPosition<*>) return
            possibleNewConstraints!!.add(variable to constraint)
        }

        override fun addLowerConstraint(
            typeVariable: TypeConstructorMarker,
            subType: CangJieTypeMarker,
            isFromNullabilityConstraint: Boolean,
            isNoInfer: Boolean
        ) = addConstraint(typeVariable, subType, ConstraintKind.LOWER, isFromNullabilityConstraint)

        override fun addEqualityConstraint(typeVariable: TypeConstructorMarker, type: CangJieTypeMarker) =
            addConstraint(typeVariable, type, ConstraintKind.EQUALITY, false)

        override fun addUpperConstraint(
            typeVariable: TypeConstructorMarker,
            superType: CangJieTypeMarker,
            isNoInfer: Boolean
        ) =
            addConstraint(typeVariable, superType, ConstraintKind.UPPER)

        override val languageVersionSettings: LanguageVersionSettings
            get() = this@ConstraintInjector.languageVersionSettings

        override fun isMyTypeVariable(type: SimpleTypeMarker): Boolean =
            c.allTypeVariables.containsKey(type.typeConstructor().unwrapStubTypeVariableConstructor())

        override fun runForkingPoint(block: ForkPointContext.() -> Unit): Boolean {
            if (!allowForking) {
                return super.runForkingPoint(block)
            }

            if (stackForConstraintsSetsFromCurrentForkPoint == null) {
                stackForConstraintsSetsFromCurrentForkPoint = SmartList()
            }

            stackForConstraintsSetsFromCurrentForkPoint!!.add(SmartList())
            val isThereSuccessfulFork = with(MyForkCreationContext()) {
                block()
                anyForkSuccessful
            }

            val constraintSets = stackForConstraintsSetsFromCurrentForkPoint?.popLast()

            when {
                // Just an optimization
                constraintSets.isNullOrEmpty() -> return isThereSuccessfulFork
                constraintSets.size > 1 -> {
                    if (forkPointsData == null) {
                        forkPointsData = SmartList()
                    }
                    forkPointsData!!.addIfNotNull(
                        constraintSets
                    )
                    return true
                }

                else -> {
                    // The emptiness case has been already handled above
                    processGivenForkPointBranchConstraints(
                        c,
                        constraintSets.single(),
                        position,
                    )
                }
            }

            return isThereSuccessfulFork
        }

        private inner class MyForkCreationContext : ForkPointContext {
            var anyForkSuccessful = false

            override fun fork(block: () -> Boolean) {
                if (stackForConstraintSetFromCurrentForkPointBranch == null) {
                    stackForConstraintSetFromCurrentForkPointBranch = SmartList()
                }

                stackForConstraintSetFromCurrentForkPointBranch!!.add(SmartList())

                block().also { anyForkSuccessful = anyForkSuccessful || it }

                stackForConstraintsSetsFromCurrentForkPoint!!.last()
                    .addIfNotNull(
                        stackForConstraintSetFromCurrentForkPointBranch?.popLast()?.takeIf { it.isNotEmpty() }?.toSet()
                    )
            }
        }

        fun hasConstraintsToProcess() = possibleNewConstraints != null

        fun setConstrainingTypesToPrintDebugInfo(lowerType: CangJieTypeMarker, upperType: CangJieTypeMarker) {
            baseLowerType = lowerType
            baseUpperType = upperType
        }

        fun runIsSubtypeOf(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,
            shouldTryUseDifferentFlexibilityForUpperType: Boolean = false,
            isFromNullabilityConstraint: Boolean = false
        ) {
            fun isSubtypeOf(upperType: CangJieTypeMarker) =
                CangJieTypeChecker.DEFAULT.isSubtypeOf(
                    lowerType as CangJieType,
                    upperType as CangJieType
                )

            if (!isSubtypeOf(upperType)) {
                // todo 改进错误报告--添加有关基本类型的信息
                if (shouldTryUseDifferentFlexibilityForUpperType && upperType.isSimpleType()) {
                    /**
                     * 请不要重复使用此逻辑。
                     * 当灵活性没有通过类型变量传播时，这对于解决约束系统是必要的。
                     * 在旧的推断系统中这样做是可以的，因为它使用了已替换的类型，具备正确的灵活性。
                     */
                    require(upperType is SimpleTypeMarker)
                    val flexibleUpperType = createFlexibleType(upperType, upperType.withOption(true))
                    if (!isSubtypeOf(flexibleUpperType)) {
                        c.addError(ConstraintError(lowerType, flexibleUpperType, position))
                    }
                } else {
                    c.addError(ConstraintError(lowerType, upperType, position))
                }
            }
        }


        private fun isCapturedTypeFromSubtyping(type: CangJieTypeMarker): Boolean {
            val capturedType = type as? CapturedTypeMarker ?: return false

            if (capturedType.isOldCapturedType()) return false

            return when (capturedType.captureStatus()) {
                CaptureStatus.FROM_EXPRESSION -> false
                CaptureStatus.FOR_SUBTYPING -> true
                CaptureStatus.FOR_INCORPORATION ->
                    error("Captured type for incorporation shouldn't escape from incorporation: $type\n" + renderBaseConstraint())
            }
        }

        private fun addConstraint(
            typeVariableConstructor: TypeConstructorMarker,
            type: CangJieTypeMarker,
            kind: ConstraintKind,
            isFromNullabilityConstraint: Boolean = false
        ) {
            val typeVariable = c.allTypeVariables[typeVariableConstructor.unwrapStubTypeVariableConstructor()]
                ?: error("Should by type variableConstructor: $typeVariableConstructor. ${c.allTypeVariables.values}")

            addNewIncorporatedConstraint(
                typeVariable,
                type,
                ConstraintContext(kind, emptySet(), isNullabilityConstraint = isFromNullabilityConstraint)
            )
        }

        private fun addNewIncorporatedConstraintFromDeclaredUpperBound(runIsSubtypeOf: Runnable) {
            isIncorporatingConstraintFromDeclaredUpperBound = true
            runIsSubtypeOf.run()
            isIncorporatingConstraintFromDeclaredUpperBound = false
        }

        // from ConstraintIncorporator.Context
        override fun addNewIncorporatedConstraint(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,
            shouldTryUseDifferentFlexibilityForUpperType: Boolean,
            isFromNullabilityConstraint: Boolean,
            isFromDeclaredUpperBound: Boolean
        ) {

            if (lowerType === upperType) return

            if (c.isAllowedType(lowerType) && c.isAllowedType(upperType)) {
                fun runIsSubtypeOf() =
                    runIsSubtypeOf(
                        lowerType,
                        upperType,
                        shouldTryUseDifferentFlexibilityForUpperType,
                        isFromNullabilityConstraint
                    )

                if (isFromDeclaredUpperBound) addNewIncorporatedConstraintFromDeclaredUpperBound(::runIsSubtypeOf) else runIsSubtypeOf()
            }
        }

        override fun addNewIncorporatedConstraint(
            typeVariable: TypeVariableMarker,
            type: CangJieTypeMarker,
            constraintContext: ConstraintContext
        ) {
            val (kind, derivedFrom, inputTypePosition, isNullabilityConstraint) = constraintContext

            var targetType = type
            if (targetType.isUninferredParameter()) {
                // there already should be an error, so there is no point in reporting one more
                return
            }

            if (targetType.isError()) {
                c.addError(ConstrainingTypeIsError(typeVariable, targetType, position))
                return
            }

            if (type.contains(this::isCapturedTypeFromSubtyping)) {
                // TypeVariable <: type -> if TypeVariable <: subType => TypeVariable <: type
                if (kind == ConstraintKind.UPPER) {
                    val subType =
                        typeApproximator.approximateToSubType(
                            type,
                            TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation
                        )
                    if (subType != null) {
                        targetType = subType
                    }
                }

                if (kind == ConstraintKind.LOWER) {
                    val superType =
                        typeApproximator.approximateToSuperType(
                            type,
                            TypeApproximatorConfiguration.SubtypeCapturedTypesApproximation
                        )
                    if (superType != null) { // todo rethink error reporting for Any cases
                        targetType = superType
                    }
                }

                if (targetType === type) {
                    c.addError(CapturedTypeFromSubtyping(typeVariable, type, position))
                    return
                }
            }

            val position =
                if (isIncorporatingConstraintFromDeclaredUpperBound) position.copy(isFromDeclaredUpperBound = true) else position

            val newConstraint = Constraint(
                /*   if (position.from is ExpectedTypeConstraintPosition<*>) ConstraintKind.EQUALITY else*/ kind,
                targetType,
                position,
                derivedFrom = derivedFrom,
                isNullabilityConstraint = isNullabilityConstraint,
                inputTypePositionBeforeIncorporation = inputTypePosition
            )

            addPossibleNewConstraint(typeVariable, newConstraint)
        }

        override val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>
            get() = c.notFixedTypeVariables.values

        override fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker? {
            val typeVariable = c.allTypeVariables[typeConstructor]
            if (typeVariable != null && !c.notFixedTypeVariables.containsKey(typeConstructor)) {
                fixedTypeVariable(typeVariable)
            }
            return typeVariable
        }

        override fun getConstraintsForVariable(typeVariable: TypeVariableMarker) =
            c.notFixedTypeVariables[typeVariable.freshTypeConstructor()]?.constraints
                ?: fixedTypeVariable(typeVariable)

        fun fixedTypeVariable(variable: TypeVariableMarker): Nothing {
            error(
                "Type variable $variable should not be fixed!\n" +
                        renderBaseConstraint()
            )
        }

        private fun renderBaseConstraint() =
            "Base constraint: $baseLowerType <: $baseUpperType from position: $position"
    }

    /**
     * 检查类型是否在允许的深度范围内
     *
     * 为了避免无限递归和性能问题，约束合并过程中限制了类型深度。
     * 允许的深度 = 初始约束中的最大深度 + 允许的增量
     *
     * @param type 要检查的类型
     * @return true 如果类型深度在允许范围内
     */
    private fun Context.isAllowedType(type: CangJieTypeMarker) =
        type.typeDepth() <= maxTypeDepthFromInitialConstraints + ALLOWED_DEPTH_DELTA_FOR_INCORPORATION

}

/**
 * 栈类型别名
 *
 * 使用可变列表作为栈的实现
 */
private typealias Stack<E> = MutableList<E>

/**
 * 约束上下文
 *
 * 封装约束的上下文信息，包括约束种类、派生关系和位置信息。
 *
 * @property kind 约束种类（UPPER、LOWER 或 EQUALITY）
 * @property derivedFrom 该约束派生自哪些类型变量
 * @property inputTypePositionBeforeIncorporation 合并前的输入类型位置（如果有）
 * @property isNullabilityConstraint 是否为可空性约束
 */
data class ConstraintContext(
    val kind: ConstraintKind,
    val derivedFrom: Set<TypeVariableMarker>,
    val inputTypePositionBeforeIncorporation: OnlyInputTypeConstraintPosition? = null,
    val isNullabilityConstraint: Boolean
)

/**
 * 获取类型变量的新鲜类型构造器
 *
 * @param c 类型系统推断扩展上下文
 * @return 新鲜类型构造器
 */
fun TypeVariableMarker.freshTypeConstructor(c: TypeSystemInferenceExtensionContext) = with(c) { freshTypeConstructor() }
