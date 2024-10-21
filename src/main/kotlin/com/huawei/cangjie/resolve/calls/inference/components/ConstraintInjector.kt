package com.huawei.cangjie.resolve.calls.inference.components

import com.huawei.cangjie.config.LanguageFeature
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.resolve.calls.inference.ConstraintSystemOperation
import com.huawei.cangjie.resolve.calls.inference.ForkPointBranchDescription
import com.huawei.cangjie.resolve.calls.inference.ForkPointData
import com.huawei.cangjie.resolve.calls.inference.model.*
import com.huawei.cangjie.types.AbstractTypeApproximator
import com.huawei.cangjie.types.AbstractTypeChecker
import com.huawei.cangjie.types.TypeApproximatorConfiguration
import com.huawei.cangjie.types.TypeCheckerState
import com.huawei.cangjie.types.model.*
import com.huawei.cangjie.utils.addIfNotNull
import com.huawei.cangjie.utils.popLast
import com.intellij.util.SmartList
import kotlin.math.max

fun CangJieTypeMarker.typeConstructor(context: TypeSystemContext): TypeConstructorMarker =
    with(context) { typeConstructor() }

class ConstraintInjector(
    val constraintIncorporator: ConstraintIncorporator,
    val typeApproximator: AbstractTypeApproximator,
    private val languageVersionSettings: LanguageVersionSettings,
) {
    private val ALLOWED_DEPTH_DELTA_FOR_INCORPORATION = 1

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

    fun processMissedConstraints(
        c: Context,
        position: IncorporationConstraintPosition,
        missedConstraints: List<Pair<TypeVariableMarker, Constraint>>
    ) {
        val properConstraintsProcessingEnabled =
            languageVersionSettings.supportsFeature(LanguageFeature.ProperTypeInferenceConstraintsProcessing)

        // If proper constraints processing is enabled, then we don't have missed constraints
        if (properConstraintsProcessingEnabled) return

        val typeCheckerState = TypeCheckerStateForConstraintInjector(c, position)
        for ((variable, constraint) in missedConstraints) {
            typeCheckerState.addPossibleNewConstraint(variable, constraint)
        }
        processConstraints(c, typeCheckerState, skipProperEqualityConstraints = false)
    }

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

    fun addInitialEqualityConstraint(
        c: Context,
        a: CangJieTypeMarker,
        b: CangJieTypeMarker,
        position: ConstraintPosition
    ) = with(c) {
        val (typeVariable, equalType) = when {
            a.typeConstructor(c) is TypeVariableTypeConstructorMarker -> a to b
            b.typeConstructor(c) is TypeVariableTypeConstructorMarker -> b to a
            else -> return
        }
        val initialConstraint = InitialConstraint(typeVariable, equalType, ConstraintKind.EQUALITY, position).also {
            c.addInitialConstraint(it)
        }
        val typeCheckerState =
            TypeCheckerStateForConstraintInjector(c, IncorporationConstraintPosition(initialConstraint))

        // We add constraints like `T? == Foo!` in the old way
        if (!typeVariable.isSimpleType() || typeVariable.isNullableType()) {
            addInitialEqualityConstraintThroughSubtyping(typeVariable, equalType, typeCheckerState)
            return
        }

        updateAllowedTypeDepth(c, equalType)
        addEqualityConstraintAndIncorporateIt(c, typeVariable, equalType, typeCheckerState)
    }

    private fun addEqualityConstraintAndIncorporateIt(
        c: Context,
        typeVariable: CangJieTypeMarker,
        equalType: CangJieTypeMarker,
        typeCheckerState: TypeCheckerStateForConstraintInjector
    ) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(typeVariable, equalType)
        typeCheckerState.addEqualityConstraint(typeVariable.typeConstructor(c), equalType)

        // Missed constraints are constraints which we skipped in the constraints processor by mistake (incorrect optimization)
        val missedConstraints = processConstraints(c, typeCheckerState)

        if (missedConstraints != null) {
            c.addMissedConstraints(typeCheckerState.position, missedConstraints)
        }
    }

    private fun updateAllowedTypeDepth(c: Context, initialType: CangJieTypeMarker) = with(c) {
        c.maxTypeDepthFromInitialConstraints = max(c.maxTypeDepthFromInitialConstraints, initialType.typeDepth())
    }

    private fun processConstraints(
        c: Context,
        typeCheckerState: TypeCheckerStateForConstraintInjector,
        skipProperEqualityConstraints: Boolean = true
    ): MutableList<Pair<TypeVariableMarker, Constraint>>? {
        return processConstraintsIgnoringForksData(typeCheckerState, c, skipProperEqualityConstraints).also {
            typeCheckerState.extractForkPointsData()?.let { allForkPointsData ->
                allForkPointsData.mapTo(c.constraintsFromAllForkPoints) { forkPointData ->
                    typeCheckerState.position to forkPointData
                }

                // During completion, we start processing fork constrains immediately
                if (c.atCompletionState) {
                    c.resolveForkPointsConstraints()
                }
            }
        }
    }

    private fun Context.shouldWeSkipConstraint(typeVariable: TypeVariableMarker, constraint: Constraint): Boolean {
        if (constraint.kind == ConstraintKind.EQUALITY)
            return false

        val constraintType = constraint.type

        if (constraintType.typeConstructor() == typeVariable.freshTypeConstructor()) {
            if (constraintType.lowerBoundIfFlexible()
                    .isMarkedNullable() && constraint.kind == ConstraintKind.LOWER
            ) return false // T? <: T

            return true // T <: T(?!)
        }

        if (constraint.position.from is DeclaredUpperBoundConstraintPosition<*> &&
            constraint.kind == ConstraintKind.UPPER && constraintType.isNullableAny()
        ) {
            return true // T <: Any?
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
        val properConstraintsProcessingEnabled =
            languageVersionSettings.supportsFeature(LanguageFeature.ProperTypeInferenceConstraintsProcessing)

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


    private fun addSubTypeConstraintAndIncorporateIt(
        c: Context,
        lowerType: CangJieTypeMarker,
        upperType: CangJieTypeMarker,
        typeCheckerState: TypeCheckerStateForConstraintInjector
    ) {
        typeCheckerState.setConstrainingTypesToPrintDebugInfo(lowerType, upperType)
        typeCheckerState.runIsSubtypeOf(lowerType, upperType)

        // Missed constraints are constraints which we skipped in the constraints processor by mistake (incorrect optimization)
        val missedConstraints = processConstraints(c, typeCheckerState)

        if (missedConstraints != null) {
            c.addMissedConstraints(typeCheckerState.position, missedConstraints)
        }
    }

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

    interface Context : TypeSystemInferenceExtensionContext {
        val allTypeVariables: Map<TypeConstructorMarker, TypeVariableMarker>

        var maxTypeDepthFromInitialConstraints: Int
        val notFixedTypeVariables: MutableMap<TypeConstructorMarker, MutableVariableWithConstraints>
        val fixedTypeVariables: MutableMap<TypeConstructorMarker, CangJieTypeMarker>
        val constraintsFromAllForkPoints: MutableList<Pair<IncorporationConstraintPosition, ForkPointData>>
        val atCompletionState: Boolean

        fun addInitialConstraint(initialConstraint: InitialConstraint)
        fun addError(error: ConstraintSystemError)

        fun addMissedConstraints(
            position: IncorporationConstraintPosition,
            constraints: MutableList<Pair<TypeVariableMarker, Constraint>>
        )

        fun resolveForkPointsConstraints()
    }


    private inner class TypeCheckerStateForConstraintInjector(
        baseState: TypeCheckerState,
        val c: Context,
        val position: IncorporationConstraintPosition
    ) : TypeCheckerStateForConstraintSystem(
        c,
        baseState.cangjieTypePreparator,
        baseState.cangjieTypeRefiner
    ), ConstraintIncorporator.Context, TypeSystemInferenceExtensionContext by c {
        constructor(c: Context, position: IncorporationConstraintPosition) : this(
            c.newTypeCheckerState(errorTypesEqualToAnything = true, stubTypesEqualToAnything = true),
            c,
            position
        )

        // We use `var` intentionally to avoid extra allocations as this property is quite "hot"
        private var possibleNewConstraints: MutableList<Pair<TypeVariableMarker, Constraint>>? = null

        private var forkPointsData: MutableList<ForkPointData>? = null
        private var stackForConstraintsSetsFromCurrentForkPoint: Stack<MutableList<ForkPointBranchDescription>>? = null
        private var stackForConstraintSetFromCurrentForkPointBranch: Stack<MutableList<Pair<TypeVariableMarker, Constraint>>>? =
            null

//        override val isInferenceCompatibilityEnabled =
//            languageVersionSettings.supportsFeature(LanguageFeature.InferenceCompatibility)

        private val allowForking: Boolean
            get() = constraintIncorporator.utilContext.isForcedAllowForkingInferenceSystem

        private var baseLowerType = position.initialConstraint.a
        private var baseUpperType = position.initialConstraint.b

        private var isIncorporatingConstraintFromDeclaredUpperBound = false

        fun extractAllConstraints() = possibleNewConstraints.also { possibleNewConstraints = null }
        fun extractForkPointsData() = forkPointsData.also { forkPointsData = null }

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
            if(constraint.position.from is ReceiverConstraintPosition<*>) return
            possibleNewConstraints!!.add(variable to constraint)
        }

        override fun addLowerConstraint(
            typeVariable: TypeConstructorMarker,
            subType: CangJieTypeMarker,
            isFromNullabilityConstraint: Boolean
        ) = addConstraint(typeVariable, subType, ConstraintKind.LOWER, isFromNullabilityConstraint)

        override fun addEqualityConstraint(typeVariable: TypeConstructorMarker, type: CangJieTypeMarker) =
            addConstraint(typeVariable, type, ConstraintKind.EQUALITY, false)

        override val isInferenceCompatibilityEnabled =
            languageVersionSettings.supportsFeature(LanguageFeature.InferenceCompatibility)

        override fun addUpperConstraint(typeVariable: TypeConstructorMarker, superType: CangJieTypeMarker) =
            addConstraint(typeVariable, superType, ConstraintKind.UPPER)

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
                AbstractTypeChecker.isSubtypeOf(
                    this@TypeCheckerStateForConstraintInjector as TypeCheckerState,
                    lowerType,
                    upperType,
                    isFromNullabilityConstraint
                )

            if (!isSubtypeOf(upperType)) {
                // todo improve error reporting -- add information about base types
                if (shouldTryUseDifferentFlexibilityForUpperType && upperType.isSimpleType()) {
                    /*
                     * Please don't reuse this logic.
                     * It's necessary to solve constraint systems when flexibility isn't propagated through a type variable.
                     * It's OK in the old inference because it uses already substituted types, that are with the correct flexibility.
                     */
                    require(upperType is SimpleTypeMarker)
                    val flexibleUpperType = createFlexibleType(upperType, upperType.withNullability(true))
                    if (!isSubtypeOf(flexibleUpperType)) {
                        c.addError(NewConstraintError(lowerType, flexibleUpperType, position))
                    }
                } else {
                    c.addError(NewConstraintError(lowerType, upperType, position))
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
                if (position.from is ExpectedTypeConstraintPosition<*>) ConstraintKind.EQUALITY else kind,
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

    private fun Context.isAllowedType(type: CangJieTypeMarker) =
        type.typeDepth() <= maxTypeDepthFromInitialConstraints + ALLOWED_DEPTH_DELTA_FOR_INCORPORATION

}

private typealias Stack<E> = MutableList<E>

data class ConstraintContext(
    val kind: ConstraintKind,
    val derivedFrom: Set<TypeVariableMarker>,
    val inputTypePositionBeforeIncorporation: OnlyInputTypeConstraintPosition? = null,
    val isNullabilityConstraint: Boolean
)

fun TypeVariableMarker.freshTypeConstructor(c: TypeSystemInferenceExtensionContext) = with(c) { freshTypeConstructor() }
