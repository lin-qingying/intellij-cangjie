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

import com.intellij.util.SmartList
import org.cangnova.cangjie.types.model.*
import org.cangnova.cangjie.utils.SmartSet
import java.util.*
import kotlin.collections.get

/**
 * 类型检查器上下文
 *
 * 定义了类型检查器的运行方式，存储类型检查器的状态
 * 通常由 [TypeCheckerProviderContext.newTypeCheckerState] 创建
 *
 * 有状态的，不应该被重用
 *
 * 当使用 [TypeCheckerProviderContext] 执行某些类型检查操作时，例如 [AbstractTypeChecker.isSubtypeOf]，
 * 应该创建特定 [TypeCheckerState] 的新实例，并正确指定类型系统上下文
 */
open class TypeCheckerState(
    /** 错误类型是否等同于任何类型 */
    val isErrorTypeEqualsToAnything: Boolean,
    /** 桩类型是否等同于任何类型 */
    val isStubTypeEqualsToAnything: Boolean,

    val isDnnTypesEqualToFlexible: Boolean,
    /** 是否允许类型变量 */
    val allowedTypeVariable: Boolean,
    /** 类型系统上下文 */
    val typeSystemContext: TypeSystemContext,
    /** 仓颉类型预处理器 */
    val cangjieTypePreparator: AbstractTypePreparator,
    /** 仓颉类型精化器 */
    val cangjieTypeRefiner: AbstractTypeRefiner
) {

    /**
     * 精化类型
     * 将类型转换为更精确的表示形式
     */
    fun refineType(type: CangJieTypeMarker): CangJieTypeMarker {
        return cangjieTypeRefiner.refineType(type)
    }

    /**
     * 准备类型
     * 在类型检查前对类型进行预处理
     */
    fun prepareType(type: CangJieTypeMarker): CangJieTypeMarker {
        return cangjieTypePreparator.prepareType(type)
    }

    /**
     * 自定义子类型判断
     * 子类可以重写此方法提供自定义的子类型检查逻辑
     * 默认返回 true，表示没有额外的限制
     */
    open fun customIsSubtypeOf(subType: CangJieTypeMarker, superType: CangJieTypeMarker): Boolean = true

    /** 类型参数嵌套深度计数器，用于防止无限递归 */
    protected var argumentsDepth = 0

    /**
     * 在参数设置下运行代码块
     *
     * 跟踪类型参数的递归深度，防止无限递归
     * 如果深度超过 100 层，会抛出错误
     *
     * @param subArgument 子类型参数，用于错误报告
     * @param f 要执行的代码块
     * @return 代码块的执行结果
     */
    internal inline fun <T> runWithArgumentsSettings(subArgument: CangJieTypeMarker, f: TypeCheckerState.() -> T): T {
        if (argumentsDepth > 100) {
            error("参数深度过高。相关参数：$subArgument")
        }

        argumentsDepth++
        val result = f()
        argumentsDepth--
        return result
    }

    /**
     * 获取捕获类型下界的检查策略
     *
     * @param subType 子类型
     * @param superType 捕获的超类型
     * @return 下界检查策略
     */
    open fun getLowerCapturedTypePolicy(subType: SimpleTypeMarker, superType: CapturedTypeMarker): LowerCapturedTypePolicy =
        LowerCapturedTypePolicy.CHECK_SUBTYPE_AND_LOWER

    /**
     * 添加子类型约束
     *
     * 在类型推断过程中被调用，用于收集约束
     *
     * @param subType 子类型
     * @param superType 超类型
     * @param isFromNullabilityConstraint 是否来自可空性约束
     * @return 如果在推断上下文中，返回约束是否成功添加；否则返回 null
     */
    open fun addSubtypeConstraint(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean? = null

    /**
     * 分叉点处理
     *
     * 处理类似 A<Int> & A<T> <: A<F_var> 的情况
     * F_var 有两个可能的解：Int 和 T，两者都可能与其他约束一起工作
     *
     * 实际上，我们需要将约束系统分叉为两份：一份 F_var=Int，另一份 F_var=T
     * 然后维护这两份，直到找到其中一个版本的矛盾
     *
     * 但这可能导致约束系统呈指数级增长，因此我们使用以下启发式方法：
     * 我们累积分叉数据，直到候选解析的最后阶段，然后尝试应用它们
     * 直到某个约束集没有矛盾
     *
     * `runForkingPoint` 在非推断上下文和 FE1.0 中的工作方式很简单：
     * 它只是对每个 subTypeArguments 组件运行基本的子类型机制，直到第一次成功
     *
     * @param block 分叉逻辑的代码块
     * @return 是否有任何一个分支成功
     */
    open fun runForkingPoint(block: ForkPointContext.() -> Unit): Boolean = with(ForkPointContext.Default()) {
        block()
        result
    }

    /**
     * 分叉点上下文接口
     * 用于在类型检查中处理多个可能的解决方案
     */
    interface ForkPointContext {
        /**
         * 创建一个分支
         * @param block 分支逻辑，返回该分支是否成功
         */
        fun fork(block: () -> Boolean)

        /**
         * 默认实现
         * 顺序尝试每个分支，直到找到第一个成功的
         */
        class Default : ForkPointContext {
            /** 是否有任何分支成功 */
            var result: Boolean = false

            override fun fork(block: () -> Boolean) {
                if (result) return  // 已经找到成功的分支，跳过后续分支
                result = block()
            }
        }
    }

    /**
     * 捕获类型下界检查策略枚举
     */
    enum class LowerCapturedTypePolicy {
        /** 仅检查下界 */
        CHECK_ONLY_LOWER,
        /** 检查子类型和下界 */
        CHECK_SUBTYPE_AND_LOWER,
        /** 跳过下界检查 */
        SKIP_LOWER
    }

    /** 超类型遍历是否被锁定（防止嵌套调用） */
    private var supertypesLocked = false

    /** 超类型遍历的双端队列，用于广度优先搜索 */
    var supertypesDeque: ArrayDeque<SimpleTypeMarker>? = null
        private set

    /** 已访问的超类型集合，用于避免重复访问 */
    var supertypesSet: MutableSet<SimpleTypeMarker>? = null
        private set

    /**
     * 初始化超类型遍历的数据结构
     *
     * 必须在调用 anySupertype 前调用
     */
    fun initialize() {
        assert(!supertypesLocked) {
            "超类型已被锁定：${this::class}"
        }
        supertypesLocked = true

        if (supertypesDeque == null) {
            supertypesDeque = ArrayDeque(4)
        }
        if (supertypesSet == null) {
            supertypesSet = SmartSet.create()
        }
    }

    /**
     * 清理超类型遍历的状态
     *
     * 必须在 anySupertype 完成后调用
     */
    fun clear() {
        supertypesDeque!!.clear()
        supertypesSet!!.clear()
        supertypesLocked = false
    }

    /**
     * 检查是否存在满足条件的超类型
     *
     * 使用广度优先搜索遍历类型的超类型层次结构
     *
     * @param start 起始类型
     * @param predicate 判断条件，返回 true 表示找到目标类型
     * @param supertypesPolicy 超类型策略，决定如何转换和遍历超类型
     * @return 是否存在满足条件的超类型
     */
    inline fun anySupertype(
        start: SimpleTypeMarker,
        predicate: (SimpleTypeMarker) -> Boolean,
        supertypesPolicy: (SimpleTypeMarker) -> SupertypesPolicy
    ): Boolean {
        // 首先检查起始类型本身
        if (predicate(start)) return true

        // 初始化遍历数据结构
        initialize()

        val deque = supertypesDeque!!
        val visitedSupertypes = supertypesSet!!

        deque.push(start)
        while (deque.isNotEmpty()) {
            val current = deque.pop()
            // 跳过已访问的类型，避免无限循环
            if (!visitedSupertypes.add(current)) continue

            // 获取当前类型的遍历策略
            val policy = supertypesPolicy(current).takeIf { it != SupertypesPolicy.None } ?: continue
            // 获取当前类型的所有直接超类型
            val supertypes = with(typeSystemContext) { current.typeConstructor().supertypes() }
            for (supertype in supertypes) {
                // 根据策略转换超类型
                val newType = policy.transformType(this, supertype)
                // 检查转换后的类型是否满足条件
                if (predicate(newType)) {
                    clear()
                    return true
                }
                // 将超类型加入队列以便继续遍历
                deque.add(newType)
            }
        }

        clear()
        return false
    }

    /**
     * 超类型遍历策略
     *
     * 定义如何转换和处理超类型
     */
    sealed class SupertypesPolicy {
        /**
         * 转换类型
         * @param state 类型检查器状态
         * @param type 要转换的类型
         * @return 转换后的简单类型
         */
        abstract fun transformType(state: TypeCheckerState, type: CangJieTypeMarker): SimpleTypeMarker

        /**
         * 不遍历策略
         * 停止遍历当前分支
         */
        object None : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker) =
                throw UnsupportedOperationException("不应该被调用")
        }

        /**
         * 灵活类型取上界策略
         * 对于灵活类型（如 Java 平台类型），取其上界进行遍历
         */
        object UpperIfFlexible : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker) =
                with(state.typeSystemContext) { type.upperBoundIfFlexible() }
        }

        /**
         * 灵活类型取下界策略
         * 对于灵活类型，取其下界进行遍历
         */
        object LowerIfFlexible : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker) =
                with(state.typeSystemContext) { type.lowerBoundIfFlexible() }
        }

        /**
         * 自定义转换策略抽象类
         * 子类可以实现自定义的类型转换逻辑
         */
        abstract class DoCustomTransform : SupertypesPolicy()
    }

    /**
     * 检查类型是否为允许的类型变量
     *
     * @param type 要检查的类型
     * @return 如果允许类型变量且该类型是类型变量，返回 true
     */
    fun isAllowedTypeVariable(type: CangJieTypeMarker): Boolean {
        return allowedTypeVariable && with(typeSystemContext) { type.isTypeVariableType() }
    }
}




/**
 * 抽象类型检查器
 *
 * 提供类型检查的核心功能，基于仓颉语言类型系统实现。
 * 这是从 Kotlin 的 AbstractTypeChecker 重构而来，专门适配仓颉语言的类型规则。
 *
 * 主要改进：
 * 1. 移除 Kotlin 特定的类型处理（如 DNN types, platform types 等）
 * 2. 简化类型变量处理（仓颉的泛型全部不变）
 * 3. 使用 CangJieSubtypeChecker 和 SpecialTypeChecker 进行实际检查
 * 4. 保留必要的类型遍历和缓存机制
 */
object AbstractTypeChecker {
    @JvmField
    var RUN_SLOW_ASSERTIONS = false

    /**
     * 准备类型以进行检查
     *
     * 对类型进行预处理和精化，使其适合进行类型比较
     */
    fun prepareType(
        context: TypeCheckerProviderContext,
        type: CangJieTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ) = context.newTypeCheckerState(true, stubTypesEqualToAnything).prepareType(type)

    /**
     * 检查子类型关系
     *
     * 使用仓颉类型检查器判断 subType 是否为 superType 的子类型
     *
     * @param context 类型检查器提供上下文
     * @param subType 子类型
     * @param superType 父类型
     * @param stubTypesEqualToAnything 是否将 stub 类型视为与任何类型相等
     * @return true 如果 subType 是 superType 的子类型
     */
    fun isSubtypeOf(
        context: TypeCheckerProviderContext,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ): Boolean {
        return isSubtypeOf(context.newTypeCheckerState(true, stubTypesEqualToAnything), subType, superType)
    }

    /**
     * It matches class types but ignores their type parameters
     *
     * Consider the following example:
     *
     * ```
     * abstract class Foo<T>
     * class FooBar : Foo<Any>()
     * ```
     *
     * In this case `isSubtypeOfClass` returns `true` for `FooBar` and `Foo<T>` input arguments
     * But `isSubtypeOf` returns `false` for the same input arguments
     */
    fun isSubtypeOfClass(
        state:  TypeCheckerState,
        typeConstructor: TypeConstructorMarker,
        superConstructor: TypeConstructorMarker
    ): Boolean {
        return isSubtypeOfClass(state.typeSystemContext, typeConstructor, superConstructor)
    }

    fun isSubtypeOfClass(
        typeSystemContext: TypeSystemContext,
        typeConstructor: TypeConstructorMarker,
        superConstructor: TypeConstructorMarker,
    ): Boolean {
        if (typeConstructor == superConstructor) return true
        with(typeSystemContext) {
            for (superType in typeConstructor.supertypes()) {
                if (isSubtypeOfClass(typeSystemContext, superType.typeConstructor(), superConstructor)) {
                    return true
                }
            }
        }
        return false
    }

    fun equalTypes(
        context: TypeCheckerProviderContext,
        a: CangJieTypeMarker,
        b: CangJieTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ): Boolean {
        return equalTypes(
            context.newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything),
            a, b
        )
    }

    @JvmOverloads
    fun isSubtypeOf(
        state:  TypeCheckerState,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean {
        if (subType === superType) return true

        if (!state.customIsSubtypeOf(subType, superType)) return false

        return with(state) {
            with(state.typeSystemContext) {
                completeIsSubTypeOf(subType, superType, isFromNullabilityConstraint)
            }
        }
    }

    fun equalTypes(state:  TypeCheckerState, a: CangJieTypeMarker, b: CangJieTypeMarker): Boolean =
        with(state.typeSystemContext) {
            if (a === b) return true

            // 仓颉语言简化：直接检查类型相等性
            // 移除 Kotlin 的 denotable type 优化，因为相关方法不存在于 TypeSystemContext
            val refinedA = state.prepareType(state.refineType(a))
            val refinedB = state.prepareType(state.refineType(b))

            // 快速检查：类型构造器必须相同
            if (!areEqualTypeConstructors(refinedA.typeConstructor(), refinedB.typeConstructor())) {
                return isSubtypeOf(state, a, b) && isSubtypeOf(state, b, a)
            }

            val simpleA = refinedA.lowerBoundIfFlexible()
            val simpleB = refinedB.lowerBoundIfFlexible()

            // 无参数类型：检查 Option 状态
            if (simpleA.argumentsCount() == 0) {
                return simpleA.isMarkedOption() == simpleB.isMarkedOption()
            }

            // 有参数类型：使用完整的双向子类型检查
            return isSubtypeOf(state, a, b) && isSubtypeOf(state, b, a)
        }


    /**
     * 完整的子类型检查
     *
     * 基于仓颉语言的类型系统规则:
     * 1. Nothing <: T <: Any (对所有类型 T)
     * 2. 用户自定义泛型全部不变 (invariant)
     * 3. 函数类型: 参数逆变, 返回值协变
     * 4. 元组类型: 元素协变
     * 5. Option 类型: T <: ?T (非 Option 可以赋值给 Option)
     */
    context(state:  TypeCheckerState, c: TypeSystemContext)
    private fun completeIsSubTypeOf(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean
    ): Boolean {
        // 准备和精化类型
        val preparedSubType = state.prepareType(state.refineType(subType))
        val preparedSuperType = state.prepareType(state.refineType(superType))

        // 检查特殊情况 (Error类型, Nothing, Any等)
        checkSubtypeForSpecialCases(preparedSubType.lowerBoundIfFlexible(), preparedSuperType.upperBoundIfFlexible())?.let {
            state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)
            return it
        }

        // 在推断上下文中添加约束
        state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)?.let { return it }

        // 执行实际的子类型检查
        return isSubtypeOfForSingleClassifierType(preparedSubType.lowerBoundIfFlexible(), preparedSuperType.upperBoundIfFlexible())
    }

    context(state:  TypeCheckerState, c: TypeSystemContext)
    private fun checkSubtypeForIntegerLiteralType(
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean? {
        if (!subType.isIntegerLiteralType() && !superType.isIntegerLiteralType()) return null

        fun isTypeInIntegerLiteralType(integerLiteralType: SimpleTypeMarker, type: SimpleTypeMarker, checkSupertypes: Boolean): Boolean =
            integerLiteralType.possibleIntegerTypes().any { possibleType ->
                (possibleType.typeConstructor() == type.typeConstructor()) || (checkSupertypes && isSubtypeOf(state, type, possibleType))
            }

        fun isIntegerLiteralTypeInIntersectionComponents(type: SimpleTypeMarker): Boolean {
            val typeConstructor = type.typeConstructor()

            return typeConstructor is IntersectionTypeConstructorMarker
                    && typeConstructor.supertypes().any { it.asSimpleType()?.isIntegerLiteralType() == true }
        }

        fun isCapturedIntegerLiteralType(type: SimpleTypeMarker): Boolean {
            if (type !is CapturedTypeMarker) return false
            val projection = type.typeConstructor().projection()
            return projection.getType()?.upperBoundIfFlexible()?.isIntegerLiteralType() == true
        }

        fun isIntegerLiteralTypeOrCapturedOne(type: SimpleTypeMarker) = type.isIntegerLiteralType() || isCapturedIntegerLiteralType(type)

        when {
            isIntegerLiteralTypeOrCapturedOne(subType) && isIntegerLiteralTypeOrCapturedOne(superType) -> {
                return true
            }

            subType.isIntegerLiteralType() -> {
                if (isTypeInIntegerLiteralType(subType, superType, checkSupertypes = false)) {
                    return true
                }
            }

            superType.isIntegerLiteralType() -> {
                // Here we also have to check supertypes for intersection types: { Int & String } <: IntegerLiteralTypes
                if (isIntegerLiteralTypeInIntersectionComponents(subType)
                    || isTypeInIntegerLiteralType(superType, subType, checkSupertypes = true)
                ) {
                    return true
                }
            }
        }
        return null
    }

    context(state:  TypeCheckerState, c: TypeSystemContext)
    private fun hasNothingSupertype(type: SimpleTypeMarker): Boolean {
        val typeConstructor = type.typeConstructor()
        if (typeConstructor.isClassTypeConstructor()) {
            return typeConstructor.isNothingConstructor()
        }
        return state.anySupertype(type, { it.typeConstructor().isNothingConstructor() }) {
            if (it.isClassType()) {
                TypeCheckerState.SupertypesPolicy.None
            } else {
                TypeCheckerState.SupertypesPolicy.LowerIfFlexible
            }
        }
    }

    context(state:  TypeCheckerState, c: TypeSystemContext)
    private fun isSubtypeOfForSingleClassifierType(
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean {
        if (RUN_SLOW_ASSERTIONS) {
            assert(subType.isSingleClassifierType() || subType.typeConstructor().isIntersection() || state.isAllowedTypeVariable(subType)) {
                "Not singleClassifierType and not intersection subType: $subType"
            }
            assert(superType.isSingleClassifierType() || state.isAllowedTypeVariable(superType)) {
                "Not singleClassifierType superType: $superType"
            }
        }

        if (!AbstractOptionChecker.isPossibleSubtype(state, subType, superType)) return false

        checkSubtypeForIntegerLiteralType(subType, superType)?.let {
            state.addSubtypeConstraint(subType, superType)
            return it
        }

        val superConstructor = superType.typeConstructor()

        if (c.areEqualTypeConstructors(subType.typeConstructor(), superConstructor) && superConstructor.parametersCount() == 0) return true
        if (superType.typeConstructor().isAnyConstructor()) return true

        val supertypesWithSameConstructor = with(findCorrespondingSupertypes(state, subType, superConstructor)) {
            // 仓颉语言简化：不需要区分 K1/K2 分支
            // 对于多个候选超类型，去重以避免不必要的分叉
            if (size > 1) {
                mapTo(mutableSetOf()) { state.prepareType(it).asSimpleType() ?: it }
            } else {
                map { state.prepareType(it).asSimpleType() ?: it }
            }
        }
        when (supertypesWithSameConstructor.size) {
            0 -> return hasNothingSupertype(subType)
            1 -> return isSubtypeForSameConstructor(supertypesWithSameConstructor.first().asArgumentList(), superType)

            else -> {
                // 多个超类型具有相同的构造器（罕见情况）
                // 仓颉语言：由于泛型不变，直接使用分叉点检查每个候选
                return state.runForkingPoint {
                    for (subTypeArguments in supertypesWithSameConstructor) {
                        fork { isSubtypeForSameConstructor(subTypeArguments.asArgumentList(), superType) }
                    }
                }
            }
        }
    }

    /**
     * 检查相同类型构造器的类型参数
     *
     * 仓颉语言简化规则：
     * - **所有泛型类型参数都是不变的 (invariant)**
     * - 这与 Kotlin 的协变/逆变不同
     * - 函数类型和元组类型有特殊处理，在其他地方实现
     *
     * 例如：
     * - List<Int> 和 List<String> 不存在子类型关系
     * - List<Int> 只能与 List<Int> 相等
     */
    context(state:  TypeCheckerState, c: TypeSystemContext)
    fun isSubtypeForSameConstructor(
        capturedSubArguments: TypeArgumentListMarker,
        superType: SimpleTypeMarker
    ): Boolean {
        val superTypeConstructor = superType.typeConstructor()

        // 检查类型参数数量匹配
        val argumentsCount = capturedSubArguments.size()
        val parametersCount = superTypeConstructor.parametersCount()
        if (argumentsCount != parametersCount || argumentsCount != superType.argumentsCount()) {
            return false
        }

        // 仓颉语言：所有泛型类型参数都是不变的
        // 因此直接检查每个类型参数是否相等
        for (index in 0 until parametersCount) {
            val superProjection = superType.getArgument(index)
            val superArgumentType = superProjection.getType() ?: continue // A<B> <: A<*>

            val subArgumentType = capturedSubArguments[index].getType()!!

            // 所有类型参数必须完全相等 (不变性)
            val correctArgument = state.runWithArgumentsSettings(subArgumentType) {
                equalTypes(state, subArgumentType, superArgumentType)
            }

            if (!correctArgument) return false
        }

        return true
    }

    context(context: TypeSystemContext)
    private fun isStubTypeSubtypeOfAnother(a: SimpleTypeMarker, b: SimpleTypeMarker): Boolean {
        if (a.typeConstructor() !== b.typeConstructor()) return false
        // 仓颉语言：使用 Option 而非 DNN
        if (a.isMarkedOption() && !b.isMarkedOption()) return false

        return true // A? == B? or A == B
    }

    context(state:  TypeCheckerState, c: TypeSystemContext)
    private fun checkSubtypeForSpecialCases(
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean? {
        if (subType.isError() || superType.isError()) {
            if (state.isErrorTypeEqualsToAnything) return true

            // 仓颉语言：使用 Option 而非 nullable
            if (subType.isMarkedOption() && !superType.isMarkedOption()) return false

            // 对于错误类型，检查类型构造器是否相同
            return c.areEqualTypeConstructors(
                if (subType.isError()) subType.typeConstructor() else subType.withOption(false).typeConstructor(),
                if (superType.isError()) superType.typeConstructor() else superType.withOption(false).typeConstructor()
            )
        }

        if (subType.isStubTypeForBuilderInference() && superType.isStubTypeForBuilderInference())
            return isStubTypeSubtypeOfAnother(subType, superType) || state.isStubTypeEqualsToAnything

        if (subType.isStubType() || superType.isStubType())
            return state.isStubTypeEqualsToAnything

        // 仓颉语言：处理捕获类型的下界
        // 移除 DNN 类型处理，使用 Option 替代
        val superTypeCaptured = superType.asCapturedType()
        val lowerType = superTypeCaptured?.lowerType()
        if (superTypeCaptured != null && lowerType != null) {
            // 如果超类型是 Option，将下界也标记为 Option
            val optionLowerType = if (superType.isMarkedOption()) {
                lowerType.withOption(true)
            } else {
                lowerType
            }
            when (state.getLowerCapturedTypePolicy(subType, superTypeCaptured)) {
                TypeCheckerState.LowerCapturedTypePolicy.CHECK_ONLY_LOWER -> return isSubtypeOf(state, subType, optionLowerType)
                TypeCheckerState.LowerCapturedTypePolicy.CHECK_SUBTYPE_AND_LOWER -> if (isSubtypeOf(state, subType, optionLowerType)) return true
                TypeCheckerState.LowerCapturedTypePolicy.SKIP_LOWER -> Unit
            }
        }

        val superTypeConstructor = superType.typeConstructor()
        if (superTypeConstructor.isIntersection()) {
            assert(!superType.isMarkedOption()) { "Intersection type should not be marked Option!: $superType" }

            return superTypeConstructor.supertypes().all { isSubtypeOf(state, subType, it) }
        }

        /*
         * We handle cases like CapturedType(out Bar) <: Foo<CapturedType(out Bar)> separately here.
         * If Foo is a self type i.g. Foo<E: Foo<E>>, then argument for E will certainly be subtype of Foo<same_argument_for_E>,
         * so if CapturedType(out Bar) is the same as a type of Foo's argument and Foo is a self type, then subtyping should return true.
         * If we don't handle this case separately, subtyping may not converge due to the nature of the capturing.
         */
        val subTypeConstructor = subType.typeConstructor()
        if (subType is CapturedTypeMarker
            || (subTypeConstructor.isIntersection() && subTypeConstructor.supertypes().all { it is CapturedTypeMarker })
        ) {
            val typeParameter =
                getTypeParameterForArgumentInBaseIfItEqualToTarget(baseType = superType, targetType = subType)
            if (typeParameter != null && typeParameter.hasRecursiveBounds(superType.typeConstructor())) {
                return true
            }
        }

        return null
    }

    context(context: TypeSystemContext)
    private fun getTypeParameterForArgumentInBaseIfItEqualToTarget(
        baseType: CangJieTypeMarker,
        targetType: CangJieTypeMarker
    ): TypeParameterMarker? {
        for (i in 0 until baseType.argumentsCount()) {
            // 仓颉语言：没有星投影，getType() 返回 null 表示未指定类型参数
            val typeArgument = baseType.getArgument(i).getType() ?: continue
            val areBothTypesCaptured = typeArgument.lowerBoundIfFlexible().isCapturedType() &&
                    targetType.lowerBoundIfFlexible().isCapturedType()

            if (typeArgument == targetType || (areBothTypesCaptured && typeArgument.typeConstructor() == targetType.typeConstructor())) {
                return baseType.typeConstructor().getParameter(i)
            }

            getTypeParameterForArgumentInBaseIfItEqualToTarget(typeArgument, targetType)?.let { return it }
        }

        return null
    }

    context(state:  TypeCheckerState, c: TypeSystemContext)
    private fun collectAllSupertypesWithGivenTypeConstructor(
        subType: SimpleTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<SimpleTypeMarker> {
        subType.fastCorrespondingSupertypes(superConstructor)?.let {
            return it
        }

        if (!superConstructor.isClassTypeConstructor() && subType.isClassType()) return emptyList()

        if (superConstructor.isCommonFinalClassConstructor()) {
            return if (c.areEqualTypeConstructors(subType.typeConstructor(), superConstructor))
                listOf(c.captureFromArguments(subType, CaptureStatus.FOR_SUBTYPING) ?: subType)
            else
                emptyList()
        }

        val result: MutableList<SimpleTypeMarker> = SmartList()

        state.anySupertype(subType, { false }) {

            val current = c.captureFromArguments(it, CaptureStatus.FOR_SUBTYPING) ?: it

            when {
                c.areEqualTypeConstructors(current.typeConstructor(), superConstructor) -> {
                    result.add(current)
                    TypeCheckerState.SupertypesPolicy.None
                }
                current.argumentsCount() == 0 -> {
                    TypeCheckerState.SupertypesPolicy.LowerIfFlexible
                }
                else -> {
                    state.typeSystemContext.substitutionSupertypePolicy(current)
                }
            }
        }

        return result
    }

    context(state:  TypeCheckerState, c: TypeSystemContext)
    private fun collectAndFilter(
        classType: SimpleTypeMarker,
        constructor: TypeConstructorMarker
    ) =
        selectOnlyPureCangJieSupertypes(collectAllSupertypesWithGivenTypeConstructor(classType, constructor))


    /**
     * 当有多条路径到达同一接口时，优先选择纯仓颉类型路径
     *
     * 示例：
     * class MyList : AbstractList<String>(), MutableList<String>
     *
     * 在仓颉语言中，虽然没有 Java 互操作，但仍可能存在 FlexibleType（灵活类型）
     * 此方法过滤掉包含灵活类型的路径，保留纯仓颉类型路径
     */
    context(c: TypeSystemContext)
    private fun selectOnlyPureCangJieSupertypes(
        supertypes: List<SimpleTypeMarker>
    ): List<SimpleTypeMarker> {
        if (supertypes.size < 2) return supertypes

        val allPureSupertypes = supertypes.filter {
            it.asArgumentList().all(c) { it.getType()?.asFlexibleType() == null }
        }
        return if (allPureSupertypes.isNotEmpty()) allPureSupertypes else supertypes
    }

    fun findCorrespondingSupertypes(
        state:  TypeCheckerState,
        subType: SimpleTypeMarker,
        superConstructor: TypeConstructorMarker,
    ): List<SimpleTypeMarker> = context(state, state.typeSystemContext) {
        findCorrespondingSupertypes(subType, superConstructor)
    }

    // nullability was checked earlier via nullabilityChecker
    // should be used only if you really sure that it is correct
    context(state:  TypeCheckerState, c: TypeSystemContext)
    fun findCorrespondingSupertypes(
        subType: SimpleTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<SimpleTypeMarker> {
        if (subType.isClassType()) {
            return collectAndFilter(subType, superConstructor)
        }

        // i.e. superType is not a classType
        if (!superConstructor.isClassTypeConstructor() && !superConstructor.isIntegerLiteralTypeConstructor()) {
            return collectAllSupertypesWithGivenTypeConstructor(subType, superConstructor)
        }

        // todo add tests
        val classTypeSupertypes = SmartList<SimpleTypeMarker>()
        state.anySupertype(subType, { false }) {
            if (it.isClassType()) {
                classTypeSupertypes.add(it)
                TypeCheckerState.SupertypesPolicy.None
            } else {
                TypeCheckerState.SupertypesPolicy.LowerIfFlexible
            }
        }

        return classTypeSupertypes.flatMap { collectAndFilter(it, superConstructor) }
    }
}

