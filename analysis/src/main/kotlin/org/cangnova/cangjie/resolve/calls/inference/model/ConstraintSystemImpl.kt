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

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import org.cangnova.cangjie.resolve.calls.inference.*
import org.cangnova.cangjie.resolve.calls.inference.components.*
import org.cangnova.cangjie.types.AbstractTypeApproximator
import org.cangnova.cangjie.types.AbstractTypeChecker
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeApproximatorConfiguration
import org.cangnova.cangjie.types.checker.CangJieTypeChecker
import org.cangnova.cangjie.types.model.*
import com.intellij.util.SmartList
import org.cangnova.cangjie.resolve.checkers.EmptyIntersectionTypeInfo
import org.cangnova.cangjie.utils.SmartSet
import org.cangnova.cangjie.utils.trimToSize
import kotlin.also
import kotlin.collections.addAll
import kotlin.collections.putAll
import kotlin.collections.set
import kotlin.math.max

/**
 * 新约束系统实现
 *
 * 这是仓颉语言类型推导系统的核心实现，负责管理和求解类型约束。
 * 采用状态机模式，支持事务、快照和回滚，确保类型推导的正确性和一致性。
 *
 * ## 核心职责
 *
 * 1. **约束收集**：收集函数调用、变量赋值等产生的类型约束
 * 2. **约束求解**：通过约束传播和合一算法求解类型变量
 * 3. **类型固定**：将推导出的具体类型绑定到类型变量
 * 4. **错误检测**：检测类型不匹配、循环约束等错误
 * 5. **事务管理**：支持约束添加的原子性操作和回滚
 *
 * ## 状态机
 *
 * ```
 * BUILDING ──┬──> TRANSACTION ──> (回滚) ──> BUILDING
 *            │                 └──> (提交) ──> BUILDING
 *            ├──> COMPLETION ──> BUILDING
 *            └──> FREEZED (只读快照)
 * ```
 *
 * ## 约束类型
 *
 * - **子类型约束**：`A <: B` (A 是 B 的子类型)
 * - **相等性约束**：`A = B` (A 和 B 必须相同)
 * - **固定约束**：`T = ConcreteType` (类型变量 T 被固定为具体类型)
 *
 * ## 使用示例
 *
 * ```kotlin
 * // 场景：推导泛型函数调用
 * // func identity<T>(x: T): T = x
 * // let result = identity(42)
 *
 * val system = ConstraintSystemImpl(injector, context, settings)
 *
 * // 1. 注册类型变量 T
 * system.registerVariable(typeVariableT)
 *
 * // 2. 添加约束：Int64 <: T (参数类型约束)
 * system.addSubtypeConstraint(Int64Type, typeVariableT.defaultType(), position)
 *
 * // 3. 添加约束：T <: resultType (返回类型约束)
 * system.addSubtypeConstraint(typeVariableT.defaultType(), resultType, position)
 *
 * // 4. 固定类型变量：T = Int64
 * system.fixVariable(typeVariableT, Int64Type, position)
 * ```
 *
 * ## 关键数据结构
 *
 * - [storage]：可变约束存储，包含所有类型变量、约束和错误
 * - [notFixedTypeVariables]：未固定的类型变量及其约束
 * - [fixedTypeVariables]：已固定的类型变量及其具体类型
 * - [postponedTypeVariables]：延迟推导的类型变量（如 lambda 返回类型）
 *
 * @param constraintInjector 约束注入器，负责将高层约束转换为底层约束
 * @param typeSystemContext 类型系统上下文，提供类型操作接口
 * @param languageVersionSettings 语言版本设置，控制特性开关
 */
class ConstraintSystemImpl(
    private val constraintInjector: ConstraintInjector,
    val typeSystemContext: TypeSystemInferenceExtensionContext,
    private val languageVersionSettings: LanguageVersionSettings,
) : ConstraintSystemCompletionContext(),
    TypeSystemInferenceExtensionContext by typeSystemContext,
    ConstraintSystem,
    ConstraintSystemBuilder,
    ConstraintInjector.Context,
    ResultTypeResolver.Context,
    PostponedArgumentsAnalyzerContext {

    /** 当前状态，控制允许的操作 */
    private var state = State.BUILDING

    /** 事务中添加的类型变量列表，用于回滚 */
    private val typeVariablesTransaction: MutableList<TypeVariableMarker> = SmartList()

    /** 非 proper 类型缓存，加速 isProperType 判断 */
    private val notProperTypesCache: MutableSet<CangJieTypeMarker> = SmartSet.create()

    /** 标记是否可以通过无限制构建器推导解析 */
    private var couldBeResolvedWithUnrestrictedBuilderInference: Boolean = false

    /** Proper 类型缓存，加速 isProperType 判断 */
    private val properTypesCache: MutableSet<CangJieTypeMarker> = SmartSet.create()

    /** 约束系统工具上下文 */
    private val utilContext = constraintInjector.constraintIncorporator.utilContext

    /** 交集类型缓存，用于检测空交集 */
    private val intersectionTypesCache: MutableMap<Collection<CangJieTypeMarker>, EmptyIntersectionTypeInfo?> =
        mutableMapOf()

    /** 可变约束存储，包含所有约束和类型变量 */
    private val storage = MutableConstraintStorage()

    /** 所有类型变量固定后执行的延迟计算 */
    private val postponedComputationsAfterAllVariablesAreFixed = mutableListOf<() -> Unit>()

    /** 临时被当作 proper 类型的类型变量集合 */
    override var typeVariablesThatAreCountedAsProperTypes: Set<TypeConstructorMarker>? = null

    /**
     * 约束系统状态枚举
     *
     * - **BUILDING**：构建状态，可以添加约束和类型变量
     * - **TRANSACTION**：事务状态，支持原子性操作和回滚
     * - **FREEZED**：冻结状态，只读快照
     * - **COMPLETION**：完成状态，正在执行类型推导完成器
     */
    private enum class State {
        BUILDING,
        TRANSACTION,
        FREEZED,
        COMPLETION
    }

    /*
     * 注意：如果移除展开运算符，调用 `checkState` 会解析到它自己，
     * 而不是函数 checkState(vararg allowedState: State)
     */
    /**
     * 检查当前状态是否符合预期
     *
     * 用于在调试模式下验证约束系统的状态一致性。
     * 仅在启用慢速断言时执行检查。
     *
     * @param a 允许的状态
     */
    private fun checkState(a: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a))
    }

    private fun checkState(a: State, b: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b))
    }

    /**
     * 获取只读约束存储快照
     *
     * 将约束系统冻结为只读状态，防止进一步修改。
     * 常用于将约束系统传递给其他组件进行分析。
     *
     * @return 只读约束存储
     */
    override fun asReadOnlyStorage(): ConstraintStorage {
        checkState(State.BUILDING, State.FREEZED)
        if (  areThereContradictionsInForks()) {
            // If there are contradictions already, we might apply all the forks because CS is anyway already failed
            resolveForkPointsConstraints()
        }

        state = State.FREEZED

        return storage
    }

    private fun checkState(a: State, b: State, c: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b, c))
    }

    private fun checkState(a: State, b: State, c: State, d: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b, c, d))
    }

    private fun checkState(vararg allowedState: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        assert(state in allowedState) {
            "State $state is not allowed. AllowedStates: ${allowedState.joinToString()}"
        }
    }

    /**
     * 事务状态类
     *
     * 管理约束系统的事务，支持原子性操作和回滚。
     * 当需要尝试性地添加约束时使用事务，如果失败可以回滚到事务开始前的状态。
     *
     * ## 使用场景
     *
     * 1. **分支选择**：尝试多个重载候选，选择成功的分支
     * 2. **猜测推导**：尝试推导类型变量，失败则回滚
     * 3. **Fork 点解析**：尝试约束集合的不同分支
     *
     * @property beforeState 事务前的状态
     * @property beforeInitialConstraintCount 事务前的初始约束数量
     * @property beforeErrorsCount 事务前的错误数量
     * @property beforeMaxTypeDepthFromInitialConstraints 事务前的最大类型深度
     * @property beforeTypeVariablesTransactionSize 事务前的类型变量事务大小
     * @property beforeMissedConstraintsCount 事务前的缺失约束数量
     * @property beforeConstraintCountByVariables 事务前每个类型变量的约束数量
     * @property beforeConstraintsFromAllForks 事务前的 fork 约束数量
     */
    private inner class TransactionState(
        private val beforeState: State,
        private val beforeInitialConstraintCount: Int,
        private val beforeErrorsCount: Int,
        private val beforeMaxTypeDepthFromInitialConstraints: Int,
        private val beforeTypeVariablesTransactionSize: Int,
        private val beforeMissedConstraintsCount: Int,
        private val beforeConstraintCountByVariables: Map<TypeConstructorMarker, Int>,
        private val beforeConstraintsFromAllForks: Int,
    ) : ConstraintSystemTransaction() {
        /**
         * 关闭事务（提交）
         *
         * 保留事务中添加的所有约束和类型变量，恢复到之前的状态。
         */
        override fun closeTransaction() {
            checkState(State.TRANSACTION)
            typeVariablesTransaction.trimToSize(beforeTypeVariablesTransactionSize)
            state = beforeState
        }

        /**
         * 回滚事务
         *
         * 撤销事务中的所有修改，包括：
         * - 移除添加的类型变量
         * - 移除添加的约束
         * - 恢复错误列表
         * - 恢复类型深度
         */
        override fun rollbackTransaction() {
            // 移除事务中添加的类型变量
            for (addedTypeVariable in typeVariablesTransaction.subList(
                beforeTypeVariablesTransactionSize,
                typeVariablesTransaction.size
            )) {
                storage.allTypeVariables.remove(addedTypeVariable.freshTypeConstructor())
                storage.notFixedTypeVariables.remove(addedTypeVariable.freshTypeConstructor())
            }

            // 恢复各项计数和状态
            storage.maxTypeDepthFromInitialConstraints = beforeMaxTypeDepthFromInitialConstraints
            storage.errors.trimToSize(beforeErrorsCount)
            storage.missedConstraints.trimToSize(beforeMissedConstraintsCount)
            storage.constraintsFromAllForkPoints.trimToSize(beforeConstraintsFromAllForks)

            // 获取事务中添加的初始约束
            val addedInitialConstraints = storage.initialConstraints.subList(
                beforeInitialConstraintCount,
                storage.initialConstraints.size
            )

            // 移除事务中为每个类型变量添加的约束
            for (variableWithConstraint in storage.notFixedTypeVariables.values) {
                val sinceIndexToRemoveConstraints =
                    beforeConstraintCountByVariables[variableWithConstraint.typeVariable.freshTypeConstructor()]
                if (sinceIndexToRemoveConstraints != null) {
                    variableWithConstraint.removeLastConstraints(sinceIndexToRemoveConstraints)
                }
            }

            // 清除添加的初始约束
            addedInitialConstraints.clear()
            closeTransaction(beforeState, beforeTypeVariablesTransactionSize)
        }
    }

    private fun closeTransaction(beforeState: State, beforeTypeVariables: Int) {
        checkState(State.TRANSACTION)
        typeVariablesTransaction.trimToSize(beforeTypeVariables)
        state = beforeState
    }

    /**
     * 准备事务
     *
     * 创建一个新的事务状态，保存当前约束系统的快照。
     * 事务可以通过 [ConstraintSystemTransaction.closeTransaction] 提交，
     * 或通过 [ConstraintSystemTransaction.rollbackTransaction] 回滚。
     *
     * ## 使用示例
     *
     * ```kotlin
     * val transaction = system.prepareTransaction()
     * try {
     *     system.addSubtypeConstraint(type1, type2, position)
     *     if (system.hasContradiction) {
     *         transaction.rollbackTransaction()  // 回滚
     *     } else {
     *         transaction.closeTransaction()     // 提交
     *     }
     * } catch (e: Exception) {
     *     transaction.rollbackTransaction()      // 异常时回滚
     * }
     * ```
     *
     * @return 事务状态对象
     */
    override fun prepareTransaction(): ConstraintSystemTransaction {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return TransactionState(
            beforeState = state,
            beforeInitialConstraintCount = storage.initialConstraints.size,
            beforeErrorsCount = storage.errors.size,
            beforeMaxTypeDepthFromInitialConstraints = storage.maxTypeDepthFromInitialConstraints,
            beforeTypeVariablesTransactionSize = typeVariablesTransaction.size,
            beforeMissedConstraintsCount = storage.missedConstraints.size,
            beforeConstraintCountByVariables = storage.notFixedTypeVariables.mapValues { it.value.rawConstraintsCount },
            beforeConstraintsFromAllForks = storage.constraintsFromAllForkPoints.size,
        ).also {
            state = State.TRANSACTION
        }
    }

    /**
     * 构建当前替换器
     *
     * 创建类型替换器，将已固定的类型变量替换为其具体类型。
     * 用于在类型推导过程中应用已知的类型绑定。
     *
     * @return 类型替换器
     */
    override fun buildCurrentSubstitutor(): TypeSubstitutorMarker {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return buildCurrentSubstitutor(emptyMap())
    }

    /**
     * 构建当前替换器（带额外绑定）
     *
     * 创建类型替换器，除了已固定的类型变量外，还包含额外的类型绑定。
     *
     * @param additionalBindings 额外的类型构造器到类型的映射
     * @return 类型替换器
     */
    override fun buildCurrentSubstitutor(additionalBindings: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage.buildCurrentSubstitutor(this, additionalBindings)
    }

    /**
     * 获取当前约束存储
     *
     * 返回当前的可变约束存储，供约束完成器和参数分析器使用。
     *
     * @return 当前约束存储
     */
    override fun currentStorage(): ConstraintStorage {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage
    }

    /**
     * 检查是否存在矛盾
     *
     * 判断约束系统中是否存在不可解决的约束冲突。
     * 例如：T <: Int64 且 T <: String 同时存在。
     *
     * @return true 表示存在矛盾，false 表示没有矛盾
     */
    override val hasContradiction: Boolean
        get() {
            checkState(
                State.FREEZED,
                State.BUILDING,
                State.COMPLETION,
                State.TRANSACTION
            )

            if (storage.hasContradiction) return true


            // Since 2.2 at each hasContradiction check, we make sure that all forks might be successfully resolved, too
            return areThereContradictionsInForks()
        }

    /**
     * 在事务中注册类型变量
     *
     * 仅在事务状态下才记录新注册的类型变量，用于事务回滚时移除。
     *
     * @param variable 待注册的类型变量
     */
    private fun transactionRegisterVariable(variable: TypeVariableMarker) {
        if (state != State.TRANSACTION) return
        if (variable.freshTypeConstructor() in storage.allTypeVariables) return
        typeVariablesTransaction.add(variable)
    }

    /**
     * 注册类型变量
     *
     * 将新的类型变量添加到约束系统中，初始化其约束集合。
     * 每个类型变量在系统中必须唯一注册。
     *
     * ## 使用场景
     *
     * 在解析泛型函数调用时，为每个类型参数创建对应的类型变量：
     * ```kotlin
     * // func map<T, R>(list: Array<T>, fn: (T) -> R): Array<R>
     * registerVariable(typeVariableT)  // 注册 T
     * registerVariable(typeVariableR)  // 注册 R
     * ```
     *
     * @param variable 待注册的类型变量
     * @throws IllegalStateException 如果类型变量已注册
     */
    override fun registerVariable(variable: TypeVariableMarker) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)

        transactionRegisterVariable(variable)
        storage.allTypeVariables.put(variable.freshTypeConstructor(), variable)
            ?.let { error("Type variable already registered: old: $it, new: $variable") }
        notProperTypesCache.clear()
        storage.notFixedTypeVariables[variable.freshTypeConstructor()] = MutableVariableWithConstraints(this, variable)
    }

    /**
     * 标记类型变量为延迟推导
     *
     * 延迟推导的类型变量不会立即固定，而是等待更多上下文信息。
     * 常用于 lambda 返回类型、构建器推导等场景。
     *
     * @param variable 待标记的类型变量
     */
    override fun markPostponedVariable(variable: TypeVariableMarker) {
        storage.postponedTypeVariables += variable
    }

    /**
     * 标记可以通过无限制构建器推导解析
     *
     * 用于标识某些构建器模式可以通过更宽松的推导规则解析。
     */
    override fun markCouldBeResolvedWithUnrestrictedBuilderInference() {
        couldBeResolvedWithUnrestrictedBuilderInference = true
    }

    /**
     * 取消延迟推导标记
     *
     * 将类型变量从延迟推导列表中移除，使其可以立即固定。
     *
     * @param variable 待取消标记的类型变量
     */
    override fun unmarkPostponedVariable(variable: TypeVariableMarker) {
        storage.postponedTypeVariables -= variable
    }

    /**
     * 移除所有延迟推导类型变量
     *
     * 清空延迟推导列表，通常在完成所有延迟推导后调用。
     */
    override fun removePostponedVariables() {
        storage.postponedTypeVariables.clear()
    }

    /**
     * 替换固定类型变量
     *
     * 对所有已固定的类型变量应用类型替换器，
     * 用于在固定新类型变量后更新已固定类型中的引用。
     *
     * @param substitutor 类型替换器
     */
    override fun substituteFixedVariables(substitutor: TypeSubstitutorMarker) {
        storage.fixedTypeVariables.replaceAll { _, type -> substitutor.safeSubstitute(type) }
    }


    override fun getBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
    ) = storage.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables[topLevelVariable to pathToExpectedType]


    override fun getBuiltFunctionalExpectedTypeForPostponedArgument(expectedTypeVariable: TypeConstructorMarker) =
        storage.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables[expectedTypeVariable]

    override fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        topLevelVariable: TypeConstructorMarker,
        pathToExpectedType: List<Pair<TypeConstructorMarker, Int>>,
        builtFunctionalType: CangJieTypeMarker,
    ) {
        storage.builtFunctionalTypesForPostponedArgumentsByTopLevelTypeVariables[topLevelVariable to pathToExpectedType] =
            builtFunctionalType
    }

    override fun putBuiltFunctionalExpectedTypeForPostponedArgument(
        expectedTypeVariable: TypeConstructorMarker,
        builtFunctionalType: CangJieTypeMarker,
    ) {
        storage.builtFunctionalTypesForPostponedArgumentsByExpectedTypeVariables[expectedTypeVariable] =
            builtFunctionalType
    }

    /**
     * 添加子类型约束
     *
     * 添加 `lowerType <: upperType` 约束，表示 lowerType 必须是 upperType 的子类型。
     *
     * ## 使用场景
     *
     * 1. **参数传递**：`identity(42)` → `Int64 <: T`
     * 2. **返回类型**：`func foo(): T` → `T <: expectedReturnType`
     * 3. **赋值**：`let x: SuperType = subTypeValue` → `SubType <: SuperType`
     *
     * @param lowerType 子类型（下界）
     * @param upperType 父类型（上界）
     * @param position 约束位置，用于错误报告
     */
    override fun addSubtypeConstraint(
        lowerType: CangJieTypeMarker,
        upperType: CangJieTypeMarker,
        position: ConstraintPosition
    ) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        constraintInjector.addInitialSubtypeConstraint(lowerType, upperType, position)

    }


    /**
     * 添加相等性约束
     *
     * 添加 `a = b` 约束，表示两个类型必须完全相同。
     * 相等性约束比子类型约束更严格，要求精确匹配。
     *
     * ## 使用场景
     *
     * 1. **类型参数固定**：固定类型变量 `T = Int64`
     * 2. **双向约束**：同时存在 `T <: A` 和 `A <: T`
     * 3. **类型别名**：`type MyInt = Int64`
     *
     * @param a 第一个类型
     * @param b 第二个类型
     * @param position 约束位置，用于错误报告
     */
    override fun addEqualityConstraint(a: CangJieTypeMarker, b: CangJieTypeMarker, position: ConstraintPosition) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        constraintInjector.addInitialEqualityConstraint(a, b, position)
    }

    /**
     * 判断类型是否为 proper 类型
     *
     * Proper 类型是不包含未固定类型变量的类型。
     * 例如：`Int64` 是 proper 类型，`T` 和 `Array<T>` 不是 proper 类型。
     *
     * ## 使用场景
     *
     * - 检查函数返回类型是否已完全确定
     * - 确定是否可以固定类型变量
     * - 验证类型推导是否完成
     *
     * ## 性能优化
     *
     * 使用缓存加速重复判断，避免递归遍历类型结构。
     *
     * @param type 待判断的类型
     * @return true 表示是 proper 类型，false 表示包含未固定类型变量
     */
    override fun isProperType(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        if (storage.allTypeVariables.isEmpty()) return true
        if (notProperTypesCache.contains(type)) return false
        if (properTypesCache.contains(type)) return true
        return isProperTypeImpl(type).also {
            (if (it) properTypesCache else notProperTypesCache).add(type)
        }
    }

    /**
     * 判断类型是否为 proper 类型的实现
     *
     * 递归检查类型及其所有类型参数，确保不包含未固定的类型变量。
     *
     * @param type 待判断的类型
     * @return true 表示是 proper 类型
     */
    private fun isProperTypeImpl(type: CangJieTypeMarker): Boolean =
        !type.contains {
            val capturedType = it.asSimpleType()?.asCapturedType()

            val typeToCheck = it

            // 如果类型变量被临时标记为 proper 类型，则跳过
            if (typeVariablesThatAreCountedAsProperTypes?.contains(typeToCheck.typeConstructor()) == true) {
                return@contains false
            }

            // 检查是否为未固定的类型变量
            return@contains storage.allTypeVariables.containsKey(typeToCheck.typeConstructor())
        }

    // ConstraintInjector.Context, FixationOrderCalculator.Context
    override val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.notFixedTypeVariables
        }
    override val fixedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.fixedTypeVariables
        }
    override val approximatorCaches: TypeApproximatorCachesPerConfiguration
        get() = storage.approximatorCaches

    override val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.constraintsFromAllForkPoints
        }

    override var atCompletionState: Boolean = false

    override fun addInitialConstraint(initialConstraint: InitialConstraint) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        storage.initialConstraints.add(initialConstraint)
    }


    override fun addMissedConstraints(
        position: IncorporationConstraintPosition,
        constraints: MutableList<Pair<TypeVariableMarker, Constraint>>,
    ) {
        storage.missedConstraints.add(position to constraints)
    }


    override val postponedTypeVariables: List<TypeVariableMarker>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.postponedTypeVariables
        }

    /**
     * @see org.jetbrains.kotlin.resolve.calls.inference.model.ConstraintStorage.typeVariableDependencies
     */
    override val typeVariableDependencies: MutableMap<TypeConstructorMarker, MutableSet<TypeConstructorMarker>>
        get() {
            checkState( State.BUILDING,   State.COMPLETION,  State.TRANSACTION)
            return storage.typeVariableDependencies
        }
    override fun containsOnlyFixedOrPostponedVariables(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains {
            val typeConstructor = it.typeConstructor()
            val variable = storage.notFixedTypeVariables[typeConstructor]?.typeVariable
            variable !in storage.postponedTypeVariables && storage.notFixedTypeVariables.containsKey(typeConstructor)
        }
    }
    private fun <T> MutableList<T>.addAllDistinct(other: List<T>) {
        val set = identityHashSetFromSum(this, other)
        clear()
        addAll(set)
    }

    private fun doAddOtherSystem(otherSystem: ConstraintStorage, mergeMode: Boolean) {
        if (otherSystem.allTypeVariables.isNotEmpty()) {
            otherSystem.allTypeVariables.forEach {
                transactionRegisterVariable(it.value)
            }
            storage.allTypeVariables.putAll(otherSystem.allTypeVariables)
            notProperTypesCache.clear()
        }

        for ((k, v) in otherSystem.approximatorCaches) {
            storage.approximatorCaches.getOrPut(k) { AbstractTypeApproximator.Cache() } += v
        }

        for ((variable, constraints) in otherSystem.notFixedTypeVariables) {
            if (!mergeMode) {
                notFixedTypeVariables[variable] = MutableVariableWithConstraints(this, constraints)
            } else {
                val previous = notFixedTypeVariables[variable]
                if (previous != null) {
                    notFixedTypeVariables[variable] = MutableVariableWithConstraints(this, previous, constraints)
                } else {
                    notFixedTypeVariables[variable] = MutableVariableWithConstraints(this, constraints)
                }
            }
        }

        for ((variable, variablesThatReferenceGivenOne) in otherSystem.typeVariableDependencies) {
            if (!mergeMode || variable !in typeVariableDependencies) {
                typeVariableDependencies[variable] = variablesThatReferenceGivenOne.toMutableSet()
            } else {
                typeVariableDependencies[variable]?.addAll(variablesThatReferenceGivenOne)
            }
        }

        // Merge mode: filtering identical constraints
        if (mergeMode) {
            storage.initialConstraints.addAllDistinct(otherSystem.initialConstraints)
            storage.constraintsFromAllForkPoints.addAllDistinct(otherSystem.constraintsFromAllForkPoints)
            storage.errors.addAllDistinct(otherSystem.errors)
        } else {
            storage.initialConstraints.addAll(otherSystem.initialConstraints)
            storage.constraintsFromAllForkPoints.addAll(otherSystem.constraintsFromAllForkPoints)
            storage.errors.addAll(otherSystem.errors)
        }

        storage.maxTypeDepthFromInitialConstraints =
            max(storage.maxTypeDepthFromInitialConstraints, otherSystem.maxTypeDepthFromInitialConstraints)
        // Keys are compared by identity only.
        // Sometimes we create structurally identical type variables (at least in K2),
        // and they should be considered different.
        storage.fixedTypeVariables.putAll(otherSystem.fixedTypeVariables)
        // K1-only, so merge isn't important here
        storage.postponedTypeVariables.addAll(otherSystem.postponedTypeVariables)

        hasContradictionInForkPointsCache = null
    }
    override fun containsOnlyFixedVariables(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains {
            val typeConstructor = it.typeConstructor()
            storage.notFixedTypeVariables.containsKey(typeConstructor)
        }
    }

    // ConstraintInjector.Context, CangJieConstraintSystemCompleter.Context
    override fun addError(error: ConstraintSystemError) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        storage.errors.add(error)
    }
    private var hasContradictionInForkPointsCache: Boolean? = null

    override fun onNewConstraintOrForkPoint() {
        hasContradictionInForkPointsCache = null

    }
    private fun applyForkPointBranch(
        constraintSetForForkBranch: ForkPointBranchDescription,
        position: IncorporationConstraintPosition,
    ) {
        checkState( State.BUILDING,   State.COMPLETION,   State.TRANSACTION)
        constraintInjector.processGivenForkPointBranchConstraints(
            constraintSetForForkBranch,
            position,
        )

        // Some new fork points constraints might be introduced, and we apply them immediately because we anyway at the
        // completion state (as we already started resolving them)
        resolveForkPointsConstraints()
    }
    /**
     * Applies the first successful branch if there's any.
     * Otherwise, applies just the first branch (containing contradictions)
     *
     * @return true if there is a successful constraint set for the fork point.
     */
    private fun applyTheBestBranchFromForkPoint(
        forkPointData: ForkPointData,
        position: IncorporationConstraintPosition,
    ): Boolean {
        val isSuccessful = forkPointData.any { constraintSetForForkBranch ->
            runTransaction {
                applyForkPointBranch(constraintSetForForkBranch, position)

                !storage.hasContradiction
            }
        }

        if (!isSuccessful) {
            applyForkPointBranch(forkPointData.first(), position)
        }

        return isSuccessful
    }
    /**
     * Checks if the current state of forked constraints is not contradictory.
     *
     * That function is expected to be pure, i.e., it should leave the system in the same state it was found before the call.
     *
     */
    fun areThereContradictionsInForks(): Boolean {
        // Before freezing, we guarantee to apply contradictions to the regular storage if there are any
        // (see NewConstraintSystemImpl.asReadOnlyStorage)
        if (state ==  State.FREEZED) return false

        if (constraintsFromAllForkPoints.isEmpty()) return false

        hasContradictionInForkPointsCache?.let { return it }

        val allForkPointsData = constraintsFromAllForkPoints.toList()
        constraintsFromAllForkPoints.clear()

        val isThereAnyUnsuccessful: Boolean
        runTransaction {
            isThereAnyUnsuccessful = allForkPointsData.any { (position, forkPointData) ->
                !applyTheBestBranchFromForkPoint(forkPointData, position)
            }

            false
        }

        constraintsFromAllForkPoints.addAll(allForkPointsData)

        return isThereAnyUnsuccessful.also { hasContradictionInForkPointsCache = it }
    }

    override fun getEmptyIntersectionTypeKind(types: Collection<CangJieTypeMarker>): EmptyIntersectionTypeInfo? {
        if (types in intersectionTypesCache)
            return intersectionTypesCache.getValue(types)

        return computeEmptyIntersectionTypeKind(types).also {
            intersectionTypesCache[types] = it
        }
    }

    private fun checkInferredEmptyIntersection(variable: TypeVariableMarker, resultType: CangJieTypeMarker) {
//        val intersectionTypeConstructor = resultType.typeConstructor().takeIf { it is IntersectionTypeConstructorMarker } ?: return
//        val upperTypes = intersectionTypeConstructor.supertypes()
//
//        // Diagnostic with these incompatible types has already been reported at the resolution stage
//        if (upperTypes.size <= 1 || storage.errors.any { it is InferredEmptyIntersection && it.incompatibleTypes == upperTypes })
//            return
//
//        val emptyIntersectionTypeInfo = getEmptyIntersectionTypeKind(upperTypes) ?: return
//
//        // Remove existing errors from the resolution stage because a completion stage error is always more precise
//        storage.errors.removeIf { it is InferredEmptyIntersection }
//
//        val isInferredEmptyIntersectionForbidden =
//            languageVersionSettings.supportsFeature(LanguageFeature.ForbidInferringTypeVariablesIntoEmptyIntersection)
//        val errorFactory = if (emptyIntersectionTypeInfo.kind.isDefinitelyEmpty && isInferredEmptyIntersectionForbidden)
//            ::InferredEmptyIntersectionError
//        else ::InferredEmptyIntersectionWarning
//
//        addError(
//            errorFactory(upperTypes.toList(), emptyIntersectionTypeInfo.casingTypes.toList(), variable, emptyIntersectionTypeInfo.kind)
//        )
    }

    override val errors: List<ConstraintSystemError>
        get() = storage.errors

    override fun asConstraintSystemCompleterContext() = apply {
        checkState(State.BUILDING)

        this.atCompletionState = true
    }

    override fun getBuilder() = apply { checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION) }
    override fun asPostponedArgumentsAnalyzerContext() = apply { checkState(State.BUILDING) }



    /**
     * 固定类型变量
     *
     * 将类型变量绑定到具体类型，这是类型推导的最终步骤。
     * 固定后，类型变量将从未固定集合移至固定集合，并触发约束传播。
     *
     * ## 工作流程
     *
     * 1. **检测空交集**：检查推导出的类型是否为空交集（不兼容的类型交集）
     * 2. **添加相等性约束**：添加 `T = resultType` 约束
     * 3. **检查缺失约束**：验证固定后是否有约束冲突
     * 4. **移除类型变量**：从未固定集合中移除该类型变量
     * 5. **清理约束**：移除其他类型变量中引用该类型变量的约束
     * 6. **记录固定结果**：将类型变量和固定类型添加到固定集合
     * 7. **替换缺失约束**：用固定类型替换缺失约束中的类型变量
     * 8. **延迟检查**：如果类型变量有 OnlyInputTypes 属性，延迟检查
     * 9. **执行延迟计算**：如果所有类型变量都已固定，执行延迟计算
     *
     * ## 使用示例
     *
     * ```kotlin
     * // func identity<T>(x: T): T = x
     * // identity(42)
     *
     * // 1. 收集约束
     * system.addSubtypeConstraint(Int64Type, T.defaultType(), position)
     * system.addSubtypeConstraint(T.defaultType(), Int64Type, position)
     *
     * // 2. 固定类型变量
     * system.fixVariable(T, Int64Type, position, Int64Type)
     * // 结果：T = Int64
     * ```
     *
     * @param variable 待固定的类型变量
     * @param resultType 推导出的结果类型
     * @param position 固定约束位置，用于错误报告
     * @param resultTypeForOnlyInputTypes 仅输入类型的结果类型，用于 OnlyInputTypes 检查
     */
    override fun fixVariable(
        variable: TypeVariableMarker,
        resultType: CangJieTypeMarker,
        position: FixVariableConstraintPosition<*>,
        resultTypeForOnlyInputTypes: CangJieTypeMarker,
    ) = with(utilContext) {
        checkState(State.BUILDING, State.COMPLETION)

        // 检查推导出的类型是否为空交集
        checkInferredEmptyIntersection(variable, resultType)

        // 添加相等性约束：T = resultType
        constraintInjector.addInitialEqualityConstraint(

            variable.defaultType(),
            resultType,
            position
        )


        val freshTypeConstructor = variable.freshTypeConstructor()
        val variableWithConstraints = notFixedTypeVariables.remove(freshTypeConstructor)

        // 移除其他类型变量中包含该类型变量的约束
        for (otherVariableWithConstraints in notFixedTypeVariables.values) {
            otherVariableWithConstraints.removeConstrains { containsTypeVariable(it.type, freshTypeConstructor) }
        }

        // 记录固定的类型变量和结果类型
        storage.fixedTypeVariables[freshTypeConstructor] = resultType

        // 用固定类型替换缺失约束中的类型变量引用
        substituteMissedConstraints()

        // 延迟 OnlyInputTypes 检查
        postponeOnlyInputTypesCheck(variableWithConstraints, resultTypeForOnlyInputTypes)

        // 如果所有类型变量都已固定，执行延迟计算
        doPostponedComputationsIfAllVariablesAreFixed()
    }

    private fun doPostponedComputationsIfAllVariablesAreFixed() {
        if (notFixedTypeVariables.isEmpty()) {
            postponedComputationsAfterAllVariablesAreFixed.forEach { it() }
        }
    }

    private fun ConstraintSystemUtilContext.postponeOnlyInputTypesCheck(
        variableWithConstraints: MutableVariableWithConstraints?,
        resultType: CangJieTypeMarker,
    ) {
        if (variableWithConstraints != null && variableWithConstraints.typeVariable.hasOnlyInputTypesAttribute()) {
            postponedComputationsAfterAllVariablesAreFixed.add {
                checkOnlyInputTypesAnnotation(
                    variableWithConstraints,
                    resultType
                )
            }
        }
    }

    private fun CangJieTypeMarker.substituteAndApproximateIfNecessary(
        substitutor: TypeSubstitutorMarker,
        approximator: AbstractTypeApproximator,
        constraintKind: ConstraintKind,
    ): CangJieTypeMarker {
        val doesInputTypeContainsOtherVariables =
            this.contains { it.typeConstructor() is TypeVariableTypeConstructorMarker }
        val substitutedType = if (doesInputTypeContainsOtherVariables) substitutor.safeSubstitute(this) else this
        // Appoximation here is the same as ResultTypeResolver do
        val approximatedType = when (constraintKind) {
            ConstraintKind.LOWER ->
                approximator.approximateToSuperType(
                    substitutedType,
                    TypeApproximatorConfiguration.InternalTypesApproximation
                )

            ConstraintKind.UPPER ->
                approximator.approximateToSubType(
                    substitutedType,
                    TypeApproximatorConfiguration.InternalTypesApproximation
                )

            ConstraintKind.EQUALITY -> substitutedType
        } ?: substitutedType

        return approximatedType
    }

    private fun checkOnlyInputTypesAnnotation(
        variableWithConstraints: MutableVariableWithConstraints,
        resultType: CangJieTypeMarker
    ) {
        val substitutor = buildCurrentSubstitutor()
        val approximator = constraintInjector.typeApproximator
        val isResultTypeEqualSomeInputType =
            variableWithConstraints.getProjectedInputCallTypes(utilContext).any { (inputType, constraintKind) ->
                val inputTypeConstructor = inputType.typeConstructor()
                val otherResultType =
                    inputType.substituteAndApproximateIfNecessary(substitutor, approximator, constraintKind)

                if (CangJieTypeChecker.DEFAULT.equalTypes(resultType as CangJieType, otherResultType as CangJieType)) return@any true
                if (!inputTypeConstructor.isIntersection()) return@any false

                inputTypeConstructor.supertypes().any {
                    val intersectionComponentResultType =
                        it.substituteAndApproximateIfNecessary(substitutor, approximator, constraintKind)
                    CangJieTypeChecker.DEFAULT.equalTypes(resultType as CangJieType, intersectionComponentResultType as CangJieType)
                }
            }
        if (!isResultTypeEqualSomeInputType) {
            addError(OnlyInputTypesDiagnostic(variableWithConstraints.typeVariable))
        }
    }

    private fun substituteMissedConstraints() {
        val substitutor = buildCurrentSubstitutor()
        for ((_, constraints) in storage.missedConstraints) {
            for ((index, variableWithConstraint) in constraints.withIndex()) {
                val (typeVariable, constraint) = variableWithConstraint
                constraints[index] = typeVariable to constraint.replaceType(substitutor.safeSubstitute(constraint.type))
            }
        }
    }

    override fun couldBeResolvedWithUnrestrictedBuilderInference() =
        couldBeResolvedWithUnrestrictedBuilderInference

    /**
     * This function tries to find the solution (set of constraints) that is consistent with some branch of each fork
     * And those constraints are being immediately applied to the system
     */
    override fun resolveForkPointsConstraints() {
        if (constraintsFromAllForkPoints.isEmpty()) return
        val allForkPointsData = constraintsFromAllForkPoints.toList()
        constraintsFromAllForkPoints.clear()

        // There may be multiple fork points:
        // - One from subtyping A<Int> & A<T> <: A<Xv>
        // - Another one from B<String> & B<F> <: B<Yv>
        // Each of them defines two sets of constraints, e.g. for the first for point:
        // 1. {Xv=Int} – is a one-element set (but potentially there might be more constraints in the set)
        // 2. {Xv=T} – second constraints set
        for ((position, forkPointData) in allForkPointsData) {
            applyTheBestBranchFromForkPoint(forkPointData, position)
        }
    }



    override fun <R> withTypeVariablesThatAreCountedAsProperTypes(
        typeVariables: Set<TypeConstructorMarker>,
        block: () -> R
    ): R {
        checkState(State.BUILDING)
        // Cleaning cache is necessary because temporarily we change the meaning of what does "proper type" mean
        properTypesCache.clear()
        notProperTypesCache.clear()

        require(typeVariablesThatAreCountedAsProperTypes == null) {
            "Currently there should be no nested withDisallowingOnlyThisTypeVariablesForProperTypes calls"
        }

        typeVariablesThatAreCountedAsProperTypes = typeVariables

        val result = block()

        typeVariablesThatAreCountedAsProperTypes = null
        properTypesCache.clear()
        notProperTypesCache.clear()

        return result
    }


    override fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage.buildNotFixedVariablesToNonSubtypableTypesSubstitutor(this)
    }


    override fun bindingStubsForPostponedVariables(): Map<TypeVariableMarker, StubTypeMarker> {
        checkState(State.BUILDING, State.COMPLETION)
        // TODO: SUB
        return storage.postponedTypeVariables.associateWith { createStubTypeForBuilderInference(it) }
    }


    // CangJieConstraintSystemCompleter.Context, PostponedArgumentsAnalyzer.Context
    override fun canBeProper(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains { storage.notFixedTypeVariables.containsKey(it.typeConstructor()) }
    }

    // PostponedArgumentsAnalyzer.Context
    override fun hasUpperOrEqualUnitConstraint(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.FREEZED)
        val constraints = storage.notFixedTypeVariables[type.typeConstructor()]?.constraints ?: return false
        return constraints.any {
            (it.kind == ConstraintKind.UPPER || it.kind == ConstraintKind.EQUALITY) &&
                    it.type.lowerBoundIfFlexible().isUnit()
        }
    }

    override fun removePostponedTypeVariablesFromConstraints(postponedTypeVariables: Set<TypeConstructorMarker>) {
        for ((_, variableWithConstraints) in storage.notFixedTypeVariables) {
            variableWithConstraints.removeConstrains { constraint ->
                constraint.type.contains { it is StubTypeMarker && it.getOriginalTypeVariable() in postponedTypeVariables }
            }
        }
    }


    // ConstraintInjector.Context, FixationOrderCalculator.Context

    override fun isTypeVariable(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return notFixedTypeVariables.containsKey(type.typeConstructor())
    }

    override fun isPostponedTypeVariable(typeVariable: TypeVariableMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return typeVariable in postponedTypeVariables
    }

    // ConstraintInjector.Context, CangJieConstraintSystemCompleter.Context
    override val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>
        get() {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            return storage.allTypeVariables
        }

    override val outerSystemVariablesPrefixSize: Int
        get() = storage.outerSystemVariablesPrefixSize

    // ResultTypeResolver.Context, VariableFixationFinder.Context
//    override fun isReified(variable: TypeVariableMarker): Boolean {
//        return with(utilContext) { variable.isReified() }
//    }

    override var maxTypeDepthFromInitialConstraints: Int
        get() = storage.maxTypeDepthFromInitialConstraints
        set(value) {
            checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
            storage.maxTypeDepthFromInitialConstraints = value
        }

    override fun getProperSuperTypeConstructors(type: CangJieTypeMarker): List<TypeConstructorMarker> {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        val variableWithConstraints =
            notFixedTypeVariables[type.typeConstructor()] ?: return listOf(type.typeConstructor())

        return variableWithConstraints.constraints.mapNotNull {
            if (it.kind == ConstraintKind.LOWER) return@mapNotNull null
            it.type.typeConstructor().takeUnless { allTypeVariables.containsKey(it) }
        }
    }

    @AssertionsOnly
    private fun runOuterCSRelatedAssertions(otherSystem: ConstraintStorage, isAddingOuter: Boolean) {
        if (!otherSystem.usesOuterCs) return

        // When integrating a child system back, it's ok that for root CS, `storage.usesOuterCs == false`
        if ((otherSystem as? MutableConstraintStorage)?.outerCS === storage) return

        require(storage.usesOuterCs)

        if (!isAddingOuter) {
            require(storage.outerSystemVariablesPrefixSize == otherSystem.outerSystemVariablesPrefixSize) {
                "Expected to be ${otherSystem.outerSystemVariablesPrefixSize}, but ${storage.outerSystemVariablesPrefixSize} found"
            }
        }
    }

    private fun addOtherSystem(
        otherSystem: ConstraintStorage,
        isAddingOuter: Boolean,
        clearNotFixedTypeVariables: Boolean = false
    ) {
        @OptIn(AssertionsOnly::class)
        runOuterCSRelatedAssertions(otherSystem, isAddingOuter)

        if (otherSystem.allTypeVariables.isNotEmpty()) {
            otherSystem.allTypeVariables.forEach {
                transactionRegisterVariable(it.value)
            }
            storage.allTypeVariables.putAll(otherSystem.allTypeVariables)
            notProperTypesCache.clear()
        }

        // `clearNotFixedTypeVariables` means that we're mostly replacing the content, thus we need to remove variables that have been fixed
        // in `otherSystem` from `this.notFixedTypeVariables`, too
        if (clearNotFixedTypeVariables) {
            notFixedTypeVariables.clear()
        }

        for ((variable, constraints) in otherSystem.notFixedTypeVariables) {
            notFixedTypeVariables[variable] = MutableVariableWithConstraints(this, constraints)
        }

        val currentInitialConstraints = storage.initialConstraints.toSet()

        otherSystem.initialConstraints.filterTo(storage.initialConstraints) {
            it !in currentInitialConstraints
        }

        storage.maxTypeDepthFromInitialConstraints =
            max(storage.maxTypeDepthFromInitialConstraints, otherSystem.maxTypeDepthFromInitialConstraints)
        storage.errors.addAll(otherSystem.errors)
        storage.fixedTypeVariables.putAll(otherSystem.fixedTypeVariables)
        storage.postponedTypeVariables.addAll(otherSystem.postponedTypeVariables)
        storage.constraintsFromAllForkPoints.addAll(otherSystem.constraintsFromAllForkPoints)

    }


    override fun addOtherSystem(otherSystem: ConstraintStorage) {
        addOtherSystem(otherSystem, isAddingOuter = false)
    }


}
