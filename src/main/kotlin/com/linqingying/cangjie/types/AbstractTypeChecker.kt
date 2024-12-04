/*
 * Copyright 2024 LinQingYing. and contributors.
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

package com.linqingying.cangjie.types

import com.intellij.util.SmartList
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.types.checker.AbstractTypePreparator
import com.linqingying.cangjie.types.model.*
import com.linqingying.cangjie.types.util.isOptionType
import com.linqingying.cangjie.types.util.optionOriginalType
import com.linqingying.cangjie.utils.SmartSet
import java.util.*

object AbstractTypeChecker {

    /**
     * If we have several paths to some interface, we should prefer pure cangjie path.
     * Example:
     *
     * class MyList : AbstractList<String>(), MutableList<String>
     *
     * We should see `String` in `get` function and others, also MyList is not subtype of MutableList<String?>
     *
     * More tests: javaAndCangJieSuperType & purelyImplementedCollection folder
     */
    private fun selectOnlyPureCangJieSupertypes(
        state: TypeCheckerState,
        supertypes: List<SimpleTypeMarker>
    ): List<SimpleTypeMarker> = with(state.typeSystemContext) {
        if (supertypes.size < 2) return supertypes

        val allPureSupertypes = supertypes.filter {
            it.asArgumentList().all(this) { argumentMarker -> argumentMarker.getType().asFlexibleType() == null }
        }
        return allPureSupertypes.ifEmpty { supertypes }
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
        state: TypeCheckerState,
        typeConstructor: TypeConstructorMarker,
        superConstructor: TypeConstructorMarker
    ): Boolean {
        if (typeConstructor == superConstructor) return true
        with(state.typeSystemContext) {
            for (superType in typeConstructor.supertypes()) {
                if (isSubtypeOfClass(state, superType.typeConstructor(), superConstructor)) {
                    return true
                }
            }
        }
        return false
    }

    private fun collectAndFilter(
        state: TypeCheckerState,
        classType: SimpleTypeMarker,
        constructor: TypeConstructorMarker
    ) =
        selectOnlyPureCangJieSupertypes(
            state,
            collectAllSupertypesWithGivenTypeConstructor(state, classType, constructor)
        )

    private fun collectAllSupertypesWithGivenTypeConstructor(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<SimpleTypeMarker> = with(state.typeSystemContext) {
        subType.fastCorrespondingSupertypes(superConstructor)?.let {
            return it
        }

        if (!superConstructor.isClassTypeConstructor() && subType.isClassType()) return emptyList()

//        if (superConstructor.isCommonFinalClassConstructor()) {
//            return if (areEqualTypeConstructors(subType.typeConstructor(), superConstructor))
//                listOf(captureFromArguments(subType, CaptureStatus.FOR_SUBTYPING) ?: subType)
//            else
//                emptyList()
//        }

        val result: MutableList<SimpleTypeMarker> = SmartList()

        state.anySupertype(subType, { false }) {

            val current = captureFromArguments(it, CaptureStatus.FOR_SUBTYPING) ?: it

            when {
                areEqualTypeConstructors(current.typeConstructor(), superConstructor) -> {
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

    // nullability was checked earlier via nullabilityChecker
    // should be used only if you really sure that it is correct
    fun findCorrespondingSupertypes(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superConstructor: TypeConstructorMarker
    ): List<SimpleTypeMarker> = with(state.typeSystemContext) {
        if (subType.isClassType()) {
            return collectAndFilter(state, subType, superConstructor)
        }

        // i.e. superType is not a classType
        if (!superConstructor.isClassTypeConstructor() && !superConstructor.isIntegerLiteralTypeConstructor()) {
            return collectAllSupertypesWithGivenTypeConstructor(state, subType, superConstructor)
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

        return classTypeSupertypes.flatMap { collectAndFilter(state, it, superConstructor) }
    }

    private fun isApplicableAsEndNode(
        state: TypeCheckerState,
        type: SimpleTypeMarker,
        end: TypeConstructorMarker
    ): Boolean =
        with(state.typeSystemContext) {
            if (type.isNothing()) return true
            if (type.isMarkedNullable()) return false

            if (state.isStubTypeEqualsToAnything && type.isStubType()) return true

            return areEqualTypeConstructors(type.typeConstructor(), end)
        }

    fun hasPathByNotMarkedNullableNodes(state: TypeCheckerState, start: SimpleTypeMarker, end: TypeConstructorMarker) =
        with(state.typeSystemContext) {
            state.anySupertype(
                start,
                { isApplicableAsEndNode(state, it, end) },
                { if (it.isMarkedNullable()) TypeCheckerState.SupertypesPolicy.None else TypeCheckerState.SupertypesPolicy.LowerIfFlexible }
            )
        }


    fun TypeCheckerProviderContext.hasPathByNotMarkedNullableNodes(
        start: SimpleTypeMarker,
        end: TypeConstructorMarker
    ) =
        hasPathByNotMarkedNullableNodes(
            newTypeCheckerState(errorTypesEqualToAnything = false, stubTypesEqualToAnything = true), start, end
        )

    @JvmField
    var RUN_SLOW_ASSERTIONS = false
    fun equalTypes(
        context: TypeCheckerProviderContext,
        a: CangJieTypeMarker,
        b: CangJieTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ): Boolean {
        return equalTypes(context.newTypeCheckerState(false, stubTypesEqualToAnything), a, b)
    }

    fun prepareType(
        context: TypeCheckerProviderContext,
        type: CangJieTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ) = context.newTypeCheckerState(true, stubTypesEqualToAnything).prepareType(type)

    fun effectiveVariance(declared: TypeVariance, useSite: TypeVariance): TypeVariance? {
        if (declared == TypeVariance.INV) return useSite
        if (useSite == TypeVariance.INV) return declared

        // both not INVARIANT
        if (declared == useSite) return declared


        return null
    }

    fun equalsIgnoringGenerics(state: TypeCheckerState, a: CangJieTypeMarker, b: CangJieTypeMarker): Boolean =
        with(state.typeSystemContext) {
            if (a === b) return true


            if (areEqualTypeConstructors(
                    a.typeConstructor(),
                    b.typeConstructor()
                )
            ) return true
            return isSubtypeOf(state, a, b) && isSubtypeOf(state, b, a)
        }

    fun equalTypes(state: TypeCheckerState, a: CangJieTypeMarker, b: CangJieTypeMarker): Boolean =
        with(state.typeSystemContext) {
            if (a === b) return true

            return isSubtypeOf(state, a, b) && isSubtypeOf(state, b, a)
        }

    private fun completeIsSubTypeOf(
        state: TypeCheckerState,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean
    ): Boolean = with(state.typeSystemContext) {
        val preparedSubType = state.prepareType(state.refineType(subType))
        val preparedSuperType = state.prepareType(state.refineType(superType))

//        checkSubtypeForSpecialCases(state, preparedSubType.lowerBoundIfFlexible(), preparedSuperType.upperBoundIfFlexible())?.let {
//            state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)
//            return it
//        }

        // we should add constraints with flexible types, otherwise we never get flexible type as answer in constraint system
        state.addSubtypeConstraint(preparedSubType, preparedSuperType, isFromNullabilityConstraint)?.let { return it }

        return isSubtypeOfForSingleClassifierType(
            state,
            preparedSubType.lowerBoundIfFlexible(),
            preparedSuperType.upperBoundIfFlexible()
        )
    }

    private fun hasNothingSupertype(state: TypeCheckerState, type: SimpleTypeMarker): Boolean =
        with(state.typeSystemContext) {
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

    private fun checkOptionType(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean = with(state.typeSystemContext) {

        if (!CangJieBuiltIns.isOptionType(superType as CangJieType)) return false

//        验证Option的泛型参数
        if (superType.arguments.size != 1) return false

//        TODO 修复出现套娃的情况
        if (CangJieBuiltIns.isOptionType(superType.arguments[0].type)) return checkOptionType(
            state,
            subType,
            superType.arguments[0].type as SimpleTypeMarker
        )


        val superConstructor = superType.arguments[0].type.constructor



        return areEqualTypeConstructors(
            subType.typeConstructor(),
            superConstructor
        )
    }

    private fun isSubtypeOfForSingleClassifierType(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean = with(state.typeSystemContext) {
        if (RUN_SLOW_ASSERTIONS) {
            assert(
                subType.isSingleClassifierType() || subType.typeConstructor()
                    .isIntersection() || state.isAllowedTypeVariable(subType)
            ) {
                "Not singleClassifierType and not intersection subType: $subType"
            }
            assert(superType.isSingleClassifierType() || state.isAllowedTypeVariable(superType)) {
                "Not singleClassifierType superType: $superType"
            }
        }

        if (!AbstractNullabilityChecker.isPossibleSubtype(state, subType, superType)) return false

        checkSubtypeForIntegerLiteralType(
            state,
            subType.lowerBoundIfFlexible(),
            superType.upperBoundIfFlexible()
        )?.let {
            state.addSubtypeConstraint(subType, superType)
            return it
        }

        val superConstructor = superType.typeConstructor()

        if (areEqualTypeConstructors(
                subType.typeConstructor(),
                superConstructor
            ) && superConstructor.parametersCount() == 0
        ) return true
        if (superType.typeConstructor().isAnyConstructor()) return true

        val supertypesWithSameConstructor = with(findCorrespondingSupertypes(state, subType, superConstructor)) {

//            if (size > 1 && (state.typeSystemContext as? TypeSystemInferenceExtensionContext)?.isK2 == true) {
//
//                mapTo(mutableSetOf()) { state.prepareType(it).asSimpleType() ?: it }
//            } else {

            map {
                state.prepareType(it).asSimpleType() ?: it
            }
//            }
        }


//        为Option进行特殊处理
        if (checkOptionType(
                state, subType, superType
            )
        ) return true


        when (supertypesWithSameConstructor.size) {
            0 -> return hasNothingSupertype(state, subType) // todo Nothing & Array<Number> <: Array<String>
            1 -> return state.isSubtypeForSameConstructor(
                supertypesWithSameConstructor.first().asArgumentList(),
                superType
            )

            else -> { // at least 2 supertypes with same constructors. Such case is rare
                val newArguments = ArgumentList(superConstructor.parametersCount())

                for (index in 0 until superConstructor.parametersCount()) {

                    val allProjections = supertypesWithSameConstructor.map {
                        it.getArgumentOrNull(index)
                            ?.takeIf { argumentMarker -> argumentMarker.getVariance() == TypeVariance.INV }?.getType()
                            ?: error("Incorrect type: $it, subType: $subType, superType: $superType")
                    }

                    // todo discuss
                    val intersection = intersectTypes(allProjections).asTypeArgument()
                    newArguments.add(intersection)
                }

                if (state.isSubtypeForSameConstructor(newArguments, superType)) return true

                return state.runForkingPoint {
                    for (subTypeArguments in supertypesWithSameConstructor) {
                        fork { state.isSubtypeForSameConstructor(subTypeArguments.asArgumentList(), superType) }
                    }
                }
            }
        }

    }

    private fun TypeCheckerState.isSubtypeForSameConstructor(
        capturedSubArguments: TypeArgumentListMarker,
        superType: SimpleTypeMarker
    ): Boolean = with(this.typeSystemContext) {
        // No way to check, as no index sometimes
        //if (capturedSubArguments === superType.arguments) return true

        val superTypeConstructor = superType.typeConstructor()

        // 有时我们可能会从不同的模块中获取两个具有不同数量类型参数的类
        // 因此在这样的情况下，我们假设这些类型彼此之间不是子类型
        val argumentsCount = capturedSubArguments.size()
        val parametersCount = superTypeConstructor.parametersCount()
        if (argumentsCount != parametersCount || argumentsCount != superType.argumentsCount()) {
            return false
        }

        for (index in 0 until parametersCount) {
            val superProjection = superType.getArgument(index) // todo error index


            val superArgumentType = superProjection.getType()
            val subArgumentType = capturedSubArguments[index].let {
                assert(it.getVariance() == TypeVariance.INV) { "Incorrect sub argument: $it" }
                it.getType()
            }

            val variance =
                effectiveVariance(superTypeConstructor.getParameter(index).getVariance(), superProjection.getVariance())
                    ?: return isErrorTypeEqualsToAnything // todo exception?

            val isTypeVariableAgainstStarProjectionForSelfType = if (variance == TypeVariance.INV) {
                isTypeVariableAgainstStarProjectionForSelfType(
                    subArgumentType,
                    superArgumentType,
                    superTypeConstructor
                ) ||
                        isTypeVariableAgainstStarProjectionForSelfType(
                            superArgumentType,
                            subArgumentType,
                            superTypeConstructor
                        )
            } else false

            /*
             * 我们不检查像 CapturedType(*) 和 TypeVariable(E) 之间的子类型关系，如果相应的类型参数形成自类型，例如 Enum<E: Enum<E>>。
             * 这种检查可能会返回 false 并在类型推断上下文中产生不必要的约束，例如 UPPER(Nothing)（由 CapturedType(*) <: TypeVariable(E) 引起），
             * 这是由于捕获类型的近似导致的。
             * 相反，我们无论如何都会移动到自类型级别：检查 CapturedType(out Enum<*>) 与 TypeVariable(E)。
             * 这种子类型关系已经可以成功并且不会在类型推断上下文中添加不必要的约束。
             */
            if (isTypeVariableAgainstStarProjectionForSelfType)
                continue

            val correctArgument = runWithArgumentsSettings(subArgumentType) {
                when (variance) {
                    TypeVariance.INV -> equalTypes(this, subArgumentType, superArgumentType)

                }
            }
            if (!correctArgument) return false
        }
        return true
    }

    /**
     * 检查整数字面量类型的子类型关系
     * 此函数旨在确定一个类型是否为另一个类型的子类型，特别是当涉及到整数字面量类型时
     * 整数字面量类型是表示具体整数值的类型，如`1`或`2`
     *
     * @param state 类型检查器状态，包含类型系统上下文
     * @param subType 潜在的子类型
     * @param superType 潜在的父类型
     * @return 如果[subType]是[superType]的子类型，则返回true；否则返回false如果无法确定子类型关系，则返回null
     */
    private fun checkSubtypeForIntegerLiteralType(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean? = with(state.typeSystemContext) {
        // 如果两个类型都不是整数字面量类型，则无法进行特定的子类型检查，返回null
        if (!subType.isIntegerLiteralType() && !superType.isIntegerLiteralType()) return null

        /**
         * 检查一个类型是否在另一个整数字面量类型的可能整数类型中
         *
         * @param integerLiteralType 整数字面量类型
         * @param type 要检查的类型
         * @param checkSupertypes 是否检查父类型
         * @return 如果[type]在[integerLiteralType]的可能整数类型中，则返回true；否则返回false
         */
        fun isTypeInIntegerLiteralType(
            integerLiteralType: SimpleTypeMarker,
            type: SimpleTypeMarker,
            checkSupertypes: Boolean
        ): Boolean =
            integerLiteralType.possibleIntegerTypes().any { possibleType ->
                (possibleType.typeConstructor() == type.typeConstructor()) || (checkSupertypes && isSubtypeOf(
                    state,
                    type,
                    possibleType
                ))
            }

        /**
         * 检查一个类型是否包含在交集类型的组件中
         *
         * @param type 要检查的类型
         * @return 如果[type]是交集类型且其组件类型中包含整数字面量类型，则返回true；否则返回false
         */
        fun isIntegerLiteralTypeInIntersectionComponents(type: SimpleTypeMarker): Boolean {
            val typeConstructor = type.typeConstructor()

            return typeConstructor is IntersectionTypeConstructorMarker
                    && typeConstructor.supertypes().any { it.asSimpleType()?.isIntegerLiteralType() == true }
        }

        /**
         * 检查一个类型是否为捕获的整数字面量类型
         *
         * @param type 要检查的类型
         * @return 如果[type]是捕获类型且其上界为整数字面量类型，则返回true；否则返回false
         */
        fun isCapturedIntegerLiteralType(type: SimpleTypeMarker): Boolean {
            if (type !is CapturedTypeMarker) return false
            val projection = type.typeConstructor().projection()
            return projection.getType().upperBoundIfFlexible().isIntegerLiteralType()
        }

        /**
         * 检查一个类型是否为整数字面量类型或捕获的整数字面量类型
         *
         * @param type 要检查的类型
         * @return 如果[type]是整数字面量类型或捕获的整数字面量类型，则返回true；否则返回false
         */
        fun isIntegerLiteralTypeOrCapturedOne(type: SimpleTypeMarker) =
            type.isIntegerLiteralType() || isCapturedIntegerLiteralType(type)

        // 根据子类型和父类型的性质进行不同的逻辑判断
        when {
            // 如果子类型和父类型都是整数字面量类型或捕获的整数字面量类型，则子类型关系成立
            isIntegerLiteralTypeOrCapturedOne(subType) && isIntegerLiteralTypeOrCapturedOne(superType) -> {
                return true
            }
//如果父类型是Option包裹的类型
            superType.isOptionType -> {

                if ((superType.optionOriginalType as? SimpleTypeMarker)?.let {
                        isTypeInIntegerLiteralType(
                            subType,
                            it, checkSupertypes = false
                        )
                    } == true) {
                    return true
                }
            }
            // 如果子类型是整数字面量类型，并且父类型在其可能的整数类型中，则子类型关系成立
            subType.isIntegerLiteralType() -> {
                if (isTypeInIntegerLiteralType(subType, superType, checkSupertypes = false)) {
                    return true
                }
            }


            // 如果父类型是整数字面量类型，并且子类型包含在交集类型组件中或在父类型的可能整数类型中，则子类型关系成立
            superType.isIntegerLiteralType() -> {
                // 这里也要检查交集类型的父类型：{ Int & String } <: IntegerLiteralTypes
                if (isIntegerLiteralTypeInIntersectionComponents(subType)
                    || isTypeInIntegerLiteralType(superType, subType, checkSupertypes = true)
                ) {
                    return true
                }
            }
        }
        // 如果以上条件都不满足，则无法确定子类型关系，返回null
        return null
    }

    private fun TypeSystemContext.isTypeVariableAgainstStarProjectionForSelfType(
        subArgumentType: CangJieTypeMarker,
        superArgumentType: CangJieTypeMarker,
        selfConstructor: TypeConstructorMarker
    ): Boolean {
        val simpleSubArgumentType = subArgumentType.asSimpleType()

        if (simpleSubArgumentType !is CapturedTypeMarker || simpleSubArgumentType.isOldCapturedType()

        ) return false
        // Only 'for subtyping' captured types are approximated before adding constraints (see ConstraintInjector.addNewIncorporatedConstraint)
        // that can lead to adding problematic constraints like UPPER(Nothing) given by CapturedType(*) <: TypeVariable(A)
        if (simpleSubArgumentType.captureStatus() != CaptureStatus.FOR_SUBTYPING) return false

        val typeVariableConstructor =
            superArgumentType.typeConstructor() as? TypeVariableTypeConstructorMarker ?: return false

        return typeVariableConstructor.typeParameter?.hasRecursiveBounds(selfConstructor) == true
    }

    fun isSubtypeOf(
        context: TypeCheckerProviderContext,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        stubTypesEqualToAnything: Boolean = true
    ): Boolean {
        return isSubtypeOf(context.newTypeCheckerState(true, stubTypesEqualToAnything), subType, superType)
    }

    @JvmOverloads
    fun isSubtypeOf(
        state: TypeCheckerState,
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean {
        if (subType === superType) return true

        if (!state.customIsSubtypeOf(subType, superType)) return false

        return completeIsSubTypeOf(state, subType, superType, isFromNullabilityConstraint)
    }
}

/**
 * Context that defines how type-checker operates, stores type-checker state,
 * created by [TypeCheckerProviderContext.newTypeCheckerState] in most cases
 *
 * Stateful and shouldn't be reused
 *
 * Once some type-checker operation is performed using a [TypeCheckerProviderContext], for example a [AbstractTypeChecker.isSubtypeOf],
 * new instance of particular [TypeCheckerState] should be created, with properly specified type system context
 */
open class TypeCheckerState(
    val isErrorTypeEqualsToAnything: Boolean,
    val isStubTypeEqualsToAnything: Boolean,
    val allowedTypeVariable: Boolean,
    val typeSystemContext: TypeSystemContext,
    val cangjieTypePreparator: AbstractTypePreparator,
    val cangjieTypeRefiner: AbstractTypeRefiner
) {

    protected var argumentsDepth = 0

    internal inline fun <T> runWithArgumentsSettings(subArgument: CangJieTypeMarker, f: TypeCheckerState.() -> T): T {
        if (argumentsDepth > 100) {
            error("Arguments depth is too high. Some related argument: $subArgument")
        }

        argumentsDepth++
        val result = f()
        argumentsDepth--
        return result
    }

    /**
     * 处理类似 A<Int> & A<T> <: A<F_var> 的情况
     * 对于 F_var 有两种可能的解决方案（Int 和 T），这两种方案都可能与其他约束一起有效或无效
     * 实际上，我们需要将约束系统分成两个副本：一个设置 F_var=Int，另一个设置 F_var=T
     * 然后同时维护这两个副本，直到发现其中一个版本存在矛盾。
     *
     * 但这可能导致约束系统的指数级增长，因此我们使用以下启发式方法：
     * 我们累积分叉数据，直到候选解析的最后阶段，然后尝试回溯应用这些分叉
     * 直到某个约束集没有矛盾为止。
     *
     * `atForkPoint` 在非推断上下文和 FE1.0 中工作简单：它只是为每个子类型参数组件运行基本的子类型机制
     * 直到第一次成功
     */
    open fun runForkingPoint(block: ForkPointContext.() -> Unit): Boolean = with(ForkPointContext.Default()) {
        block()
        result
    }


    interface ForkPointContext {
        fun fork(block: () -> Boolean)

        class Default : ForkPointContext {
            var result: Boolean = false
            override fun fork(block: () -> Boolean) {
                if (result) return
                result = block()
            }
        }
    }

    sealed class SupertypesPolicy {
        abstract fun transformType(state: TypeCheckerState, type: CangJieTypeMarker): SimpleTypeMarker

        data object None : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker) =
                throw UnsupportedOperationException("Should not be called")
        }

        data object UpperIfFlexible : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker) =
                with(state.typeSystemContext) { type.upperBoundIfFlexible() }
        }

        data object LowerIfFlexible : SupertypesPolicy() {
            override fun transformType(state: TypeCheckerState, type: CangJieTypeMarker) =
                with(state.typeSystemContext) { type.lowerBoundIfFlexible() }
        }

        abstract class DoCustomTransform : SupertypesPolicy()
    }

    private var supertypesLocked = false
    var supertypesDeque: ArrayDeque<SimpleTypeMarker>? = null
        private set
    var supertypesSet: MutableSet<SimpleTypeMarker>? = null
        private set

    open fun addSubtypeConstraint(
        subType: CangJieTypeMarker,
        superType: CangJieTypeMarker,
        isFromNullabilityConstraint: Boolean = false
    ): Boolean? = null

    fun initialize() {
        assert(!supertypesLocked) {
            "Supertypes were locked for ${this::class}"
        }
        supertypesLocked = true

        if (supertypesDeque == null) {
            supertypesDeque = ArrayDeque(4)
        }
        if (supertypesSet == null) {
            supertypesSet = SmartSet.create()
        }
    }

    inline fun anySupertype(
        start: SimpleTypeMarker,
        predicate: (SimpleTypeMarker) -> Boolean,
        supertypesPolicy: (SimpleTypeMarker) -> SupertypesPolicy
    ): Boolean {
        if (predicate(start)) return true

        initialize()

        val deque = supertypesDeque!!
        val visitedSupertypes = supertypesSet!!

        deque.push(start)
        while (deque.isNotEmpty()) {
            if (visitedSupertypes.size > 1000) {
                error("Too many supertypes for type: $start. Supertypes = ${visitedSupertypes.joinToString()}")
            }
            val current = deque.pop()
            if (!visitedSupertypes.add(current)) continue

            val policy = supertypesPolicy(current).takeIf { it != SupertypesPolicy.None } ?: continue
            val supertypes = with(typeSystemContext) { current.typeConstructor().supertypesAndExtend() }
            for (supertype in supertypes) {
                val newType = policy.transformType(this, supertype)
                if (predicate(newType)) {
                    clear()
                    return true
                }
                deque.add(newType)
            }
        }

        clear()
        return false
    }

    fun clear() {
        supertypesDeque!!.clear()
        supertypesSet!!.clear()
        supertypesLocked = false
    }

    @OptIn(TypeRefinement::class)
    fun refineType(type: CangJieTypeMarker): CangJieTypeMarker {
        return cangjieTypeRefiner.refineType(type)
    }

    fun isAllowedTypeVariable(type: CangJieTypeMarker): Boolean {
        return allowedTypeVariable && with(typeSystemContext) { type.isTypeVariableType() }
    }

    open fun customIsSubtypeOf(subType: CangJieTypeMarker, superType: CangJieTypeMarker): Boolean = true
    fun prepareType(type: CangJieTypeMarker): CangJieTypeMarker {
        return cangjieTypePreparator.prepareType(type)
    }
}

object AbstractNullabilityChecker {
    // this method checks only nullability
    fun isPossibleSubtype(state: TypeCheckerState, subType: SimpleTypeMarker, superType: SimpleTypeMarker): Boolean =
        runIsPossibleSubtype(state, subType, superType)

    /**
     * 检查子类型关系是否可能。
     *
     * 此函数用于确定 [subType] 是否可能是 [superType] 的子类型，基于 [state] 提供的类型信息和状态。
     * 函数执行一系列检查，以确保子类型关系的有效性。
     *
     * @param state 类型检查器的状态，包含类型系统上下文和其他相关信息。
     * @param subType 被检查的子类型。
     * @param superType 被检查的超类型。
     * @return 如果 [subType] 可能是 [superType] 的子类型，则返回 `true`，否则返回 `false`。
     */
    private fun runIsPossibleSubtype(
        state: TypeCheckerState,
        subType: SimpleTypeMarker,
        superType: SimpleTypeMarker
    ): Boolean =
        with(state.typeSystemContext) {
            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                // 断言子类型是单分类类型、交集类型或允许的类型变量
                assert(
                    subType.isSingleClassifierType() || subType.typeConstructor()
                        .isIntersection() || state.isAllowedTypeVariable(subType)
                ) {
                    "Not singleClassifierType and not intersection subType: $subType"
                }
                // 断言超类型是单分类类型或允许的类型变量
                assert(superType.isSingleClassifierType() || state.isAllowedTypeVariable(superType)) {
                    "Not singleClassifierType superType: $superType"
                }
            }

            // 如果子类型是 OptionType，直接返回 true
            if (subType is OptionType) return true

            // 如果超类型是可空的，直接返回 true
            if (superType.isMarkedNullable()) return true

            // 如果子类型肯定是非空的，直接返回 true
            @OptIn(ObsoleteTypeKind::class)
            if (subType.isDefinitelyNotNullType() || subType.isNotNullTypeParameter()) return true

            // 如果子类型是捕获类型且投影是非空的，直接返回 true
            if (subType is CapturedTypeMarker && subType.isProjectionNotNull()) return true

            // 如果子类型有非空地超类型，直接返回 true
            if (state.hasNotNullSupertype(subType, TypeCheckerState.SupertypesPolicy.LowerIfFlexible)) return true

            // 如果子类型没有非空地超类型且不是肯定非空的，但超类型肯定是非空的，直接返回 false
            if (superType.isDefinitelyNotNullType()) return false

            // 如果子类型没有非空地超类型，但超类型有非空地超类型，直接返回 false
            if (state.hasNotNullSupertype(superType, TypeCheckerState.SupertypesPolicy.UpperIfFlexible)) return false

            // 如果子类型和超类型都没有非空地超类型且都不是肯定非空的

            /**
             * 如果我们仍然不确定，这意味着超类型不是类类型，例如——类型参数。
             *
             * 对于带有下界捕获类型，此函数可能会返回错误结果。例如：
             *  class A<T>, A<in Number> => \exist Q : Number <: Q. A<Q>
             *      isPossibleSubtype(Number, Q) = false。
             *      这样的情况应在 [NewCangJieTypeChecker.isSubtypeOf] 中考虑（交集类型同理）。
             */

            // 类类型不能在其超类型列表中包含特殊类型
            if (subType.isClassType()) return false

            // 最后检查是否存在一条路径，使得子类型和超类型之间的关系成立
            return hasPathByNotMarkedNullableNodes(state, subType, superType.typeConstructor())
        }


    fun isSubtypeOfAny(state: TypeCheckerState, type: CangJieTypeMarker): Boolean =
        with(state.typeSystemContext) {
            state.hasNotNullSupertype(type.lowerBoundIfFlexible(), TypeCheckerState.SupertypesPolicy.LowerIfFlexible)
        }

    fun isSubtypeOfAny(context: TypeCheckerProviderContext, type: CangJieTypeMarker): Boolean =
        isSubtypeOfAny(
            context.newTypeCheckerState(
                errorTypesEqualToAnything = false,
                stubTypesEqualToAnything = true
            ),
            type
        )

    private fun isApplicableAsEndNode(
        state: TypeCheckerState,
        type: SimpleTypeMarker,
        end: TypeConstructorMarker
    ): Boolean =
        with(state.typeSystemContext) {
            if (type.isNothing()) return true
            if (type.isMarkedNullable()) return false

            if (state.isStubTypeEqualsToAnything && type.isStubType()) return true

            return areEqualTypeConstructors(type.typeConstructor(), end)
        }

    fun TypeCheckerState.hasNotNullSupertype(
        type: SimpleTypeMarker, supertypesPolicy:
        TypeCheckerState.SupertypesPolicy
    ) =
        with(typeSystemContext) {
            anySupertype(type, {
                (it.isClassType() && !it.isMarkedNullable()) || it.isDefinitelyNotNullType()
            }) {
                if (it.isMarkedNullable()) TypeCheckerState.SupertypesPolicy.None else supertypesPolicy
            }
        }

    /**
     * 检查是否存在一条通过未标记为可空的节点的路径
     * 此函数用于在类型检查过程中，判断从一个简单类型到一个类型构造器是否存在一条路径，
     * 该路径仅通过那些未被标记为可空的类型节点此方法主要用于避免在类型推断时，
     * 通过可空类型的路径，以确保类型安全性
     *
     * @param state 类型检查的状态，包含类型检查过程中的上下文信息
     * @param start 路径的起始点，表示一个简单类型
     * @param end 路径的终点，表示一个类型构造器
     * @return 如果存在一条通过未标记为可空的节点的路径，则返回true；否则返回false
     */
    private fun hasPathByNotMarkedNullableNodes(
        state: TypeCheckerState,
        start: SimpleTypeMarker,
        end: TypeConstructorMarker
    ) =
        with(state.typeSystemContext) {
            // 遍历起始类型的所用超类型，寻找符合条件的路径
            state.anySupertype(
                start,
                // 判断当前超类型是否符合路径终点的条件
                { isApplicableAsEndNode(state, it, end) },
                // 确定处理超类型的策略：如果类型被标记为可空，则不进一步遍历其超类型
                { if (it.isMarkedNullable()) TypeCheckerState.SupertypesPolicy.None else TypeCheckerState.SupertypesPolicy.LowerIfFlexible }
            )
        }

}

object AbstractFlexibilityChecker {
    fun TypeSystemCommonSuperTypesContext.hasDifferentFlexibilityAtDepth(types: Collection<CangJieTypeMarker>): Boolean {
        if (types.isEmpty()) return false
        if (hasDifferentFlexibility(types)) return true

        for (i in 0 until types.first().argumentsCount()) {
            val typeArgumentForOtherTypes = types.mapNotNull {
                if (it.argumentsCount() > i) it.getArgument(i)
                    .getType() else null
            }

            if (hasDifferentFlexibilityAtDepth(typeArgumentForOtherTypes)) return true
        }

        return false
    }

    private fun TypeSystemCommonSuperTypesContext.hasDifferentFlexibility(types: Collection<CangJieTypeMarker>): Boolean {
        val firstType = types.first()
        if (types.all { it === firstType }) return false

        return !types.all { it.isFlexible() } && !types.all { !it.isFlexible() }
    }
}
