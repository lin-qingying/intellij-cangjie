package com.huawei.cangjie.resolve.calls.inference.model

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import com.huawei.cangjie.resolve.calls.inference.*
import com.huawei.cangjie.resolve.calls.inference.components.*
import com.huawei.cangjie.types.AbstractTypeChecker
import com.huawei.cangjie.types.model.*
import com.huawei.cangjie.utils.SmartList
import com.huawei.cangjie.utils.SmartSet
import com.huawei.cangjie.utils.trimToSize
import kotlin.math.max

class NewConstraintSystemImpl(
    private val constraintInjector: ConstraintInjector,
    val typeSystemContext: TypeSystemInferenceExtensionContext,
    private val languageVersionSettings: LanguageVersionSettings,
) : ConstraintSystemCompletionContext(),
    TypeSystemInferenceExtensionContext by typeSystemContext,
    NewConstraintSystem,
    ConstraintSystemBuilder,
    ConstraintInjector.Context,
    ResultTypeResolver.Context,
    PostponedArgumentsAnalyzerContext {
    private var state = State.BUILDING
    private val typeVariablesTransaction: MutableList<TypeVariableMarker> = SmartList()
    private val notProperTypesCache: MutableSet<CangJieTypeMarker> = SmartSet.create()
    private var couldBeResolvedWithUnrestrictedBuilderInference: Boolean = false
    private val properTypesCache: MutableSet<CangJieTypeMarker> = SmartSet.create()
    private val utilContext = constraintInjector.constraintIncorporator.utilContext

    private val storage = MutableConstraintStorage()
    private val postponedComputationsAfterAllVariablesAreFixed = mutableListOf<() -> Unit>()
    override var typeVariablesThatAreCountedAsProperTypes: Set<TypeConstructorMarker>? = null

    private enum class State {
        BUILDING,
        TRANSACTION,
        FREEZED,
        COMPLETION
    }

    /*
        * If remove spread operator then call `checkState` will resolve to itself
        *   instead of fun checkState(vararg allowedState: State)
        */
    private fun checkState(a: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a))
    }

    private fun checkState(a: State, b: State) {
        if (!AbstractTypeChecker.RUN_SLOW_ASSERTIONS) return
        checkState(*arrayOf(a, b))
    }
    override fun asReadOnlyStorage(): ConstraintStorage {
        checkState(State.BUILDING, State.FREEZED)
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
        override fun closeTransaction() {
            checkState(State.TRANSACTION)
            typeVariablesTransaction.trimToSize(beforeTypeVariablesTransactionSize)
            state = beforeState
        }

        override fun rollbackTransaction() {
            for (addedTypeVariable in typeVariablesTransaction.subList(
                beforeTypeVariablesTransactionSize,
                typeVariablesTransaction.size
            )) {
                storage.allTypeVariables.remove(addedTypeVariable.freshTypeConstructor())
                storage.notFixedTypeVariables.remove(addedTypeVariable.freshTypeConstructor())
            }
            storage.maxTypeDepthFromInitialConstraints = beforeMaxTypeDepthFromInitialConstraints
            storage.errors.trimToSize(beforeErrorsCount)
            storage.missedConstraints.trimToSize(beforeMissedConstraintsCount)
            storage.constraintsFromAllForkPoints.trimToSize(beforeConstraintsFromAllForks)

            val addedInitialConstraints = storage.initialConstraints.subList(
                beforeInitialConstraintCount,
                storage.initialConstraints.size
            )

            for (variableWithConstraint in storage.notFixedTypeVariables.values) {
                val sinceIndexToRemoveConstraints =
                    beforeConstraintCountByVariables[variableWithConstraint.typeVariable.freshTypeConstructor()]
                if (sinceIndexToRemoveConstraints != null) {
                    variableWithConstraint.removeLastConstraints(sinceIndexToRemoveConstraints)
                }
            }

            addedInitialConstraints.clear() // remove constraint from storage.initialConstraints
            closeTransaction(beforeState, beforeTypeVariablesTransactionSize)
        }
    }

    private fun closeTransaction(beforeState: State, beforeTypeVariables: Int) {
        checkState(State.TRANSACTION)
        typeVariablesTransaction.trimToSize(beforeTypeVariables)
        state = beforeState
    }

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

    // PostponedArgumentsAnalyzer.Context
    override fun buildCurrentSubstitutor(): TypeSubstitutorMarker {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return buildCurrentSubstitutor(emptyMap())
    }

    override fun buildCurrentSubstitutor(additionalBindings: Map<TypeConstructorMarker, CangJieTypeMarker>): TypeSubstitutorMarker {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage.buildCurrentSubstitutor(this, additionalBindings)
    }


    override fun currentStorage(): ConstraintStorage {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        return storage
    }

    // ConstraintSystemBuilder, CangJieConstraintSystemCompleter.Context
    override val hasContradiction: Boolean
        get() = storage.hasContradiction.also {
            checkState(
                State.FREEZED,
                State.BUILDING,
                State.COMPLETION,
                State.TRANSACTION
            )
        }

    // ConstraintSystemBuilder
    private fun transactionRegisterVariable(variable: TypeVariableMarker) {
        if (state != State.TRANSACTION) return
        if (variable.freshTypeConstructor() in storage.allTypeVariables) return
        typeVariablesTransaction.add(variable)
    }

    // ConstraintSystemOperation
    override fun registerVariable(variable: TypeVariableMarker) {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)

        transactionRegisterVariable(variable)
        storage.allTypeVariables.put(variable.freshTypeConstructor(), variable)
            ?.let { error("Type variable already registered: old: $it, new: $variable") }
        notProperTypesCache.clear()
        storage.notFixedTypeVariables[variable.freshTypeConstructor()] = MutableVariableWithConstraints(this, variable)
    }

    override fun markPostponedVariable(variable: TypeVariableMarker) {
        storage.postponedTypeVariables += variable
    }

    override fun markCouldBeResolvedWithUnrestrictedBuilderInference() {
        couldBeResolvedWithUnrestrictedBuilderInference = true
    }

    override fun unmarkPostponedVariable(variable: TypeVariableMarker) {
        storage.postponedTypeVariables -= variable
    }

    override fun removePostponedVariables() {
        storage.postponedTypeVariables.clear()
    }

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

    override fun addSubtypeConstraint(
        lowerType: CangJieTypeMarker,
        upperType: CangJieTypeMarker,
        position: ConstraintPosition
    ) =
        constraintInjector.addInitialSubtypeConstraint(
            apply { checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION) },
            lowerType,
            upperType,
            position
        )

    override fun addEqualityConstraint(a: CangJieTypeMarker, b: CangJieTypeMarker, position: ConstraintPosition) =
        constraintInjector.addInitialEqualityConstraint(
            apply { checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION) },
            a,
            b,
            position
        )

    // ResultTypeResolver.Context, ConstraintSystemBuilder
    override fun isProperType(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION, State.TRANSACTION)
        if (storage.allTypeVariables.isEmpty()) return true
        if (notProperTypesCache.contains(type)) return false
        if (properTypesCache.contains(type)) return true
        return isProperTypeImpl(type).also {
            (if (it) properTypesCache else notProperTypesCache).add(type)
        }
    }

    private fun isProperTypeImpl(type: CangJieTypeMarker): Boolean =
        !type.contains {
            val capturedType = it.asSimpleType()?.asCapturedType()

            val typeToCheck =
                if (capturedType is CapturedTypeMarker && capturedType.captureStatus() == CaptureStatus.FROM_EXPRESSION)
                    capturedType.typeConstructorProjection().takeUnless { projection -> projection.isStarProjection() }
                        ?.getType()
                else
                    it

            if (typeToCheck == null) return@contains false
            if (typeVariablesThatAreCountedAsProperTypes?.contains(typeToCheck.typeConstructor()) == true) {
                return@contains false
            }

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


    override fun containsOnlyFixedOrPostponedVariables(type: CangJieTypeMarker): Boolean {
        checkState(State.BUILDING, State.COMPLETION)
        return !type.contains {
            val typeConstructor = it.typeConstructor()
            val variable = storage.notFixedTypeVariables[typeConstructor]?.typeVariable
            variable !in storage.postponedTypeVariables && storage.notFixedTypeVariables.containsKey(typeConstructor)
        }
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


    private fun checkMissedConstraints() {
        val constraintSystem = this@NewConstraintSystemImpl
        val errorsByMissedConstraints = buildList {
            runTransaction {
                for ((position, constraints) in storage.missedConstraints) {
                    val fixedVariableConstraints =
                        constraints.filter { (typeVariable, _) -> typeVariable.freshTypeConstructor() in notFixedTypeVariables }
                    constraintInjector.processMissedConstraints(constraintSystem, position, fixedVariableConstraints)
                }
                errors.filterIsInstance<NewConstraintError>().forEach(::add)
                false
            }
        }
        val constraintErrors = constraintSystem.errors.filterIsInstance<NewConstraintError>()
        // Don't report warning if an error on the same call has already been reported
        if (constraintErrors.isEmpty()) {
            errorsByMissedConstraints.forEach {
                constraintSystem.addError(it.transformToWarning())
            }
        }
    }

    // CangJieConstraintSystemCompleter.Context
    override fun fixVariable(
        variable: TypeVariableMarker,
        resultType: CangJieTypeMarker,
        position: FixVariableConstraintPosition<*>,
        resultTypeForOnlyInputTypes: CangJieTypeMarker,
    ) = with(utilContext) {
        checkState(State.BUILDING, State.COMPLETION)

        checkInferredEmptyIntersection(variable, resultType)

        constraintInjector.addInitialEqualityConstraint(
            this@NewConstraintSystemImpl,
            variable.defaultType(),
            resultType,
            position
        )

        /*
         * Checking missed constraint can introduce new type mismatch warnings.
         * It's needed to deprecate green code which works only due to incorrect optimization in the constraint injector.
         * TODO: remove this code (and `substituteMissedConstraints`) with removing `ProperTypeInferenceConstraintsProcessing` feature
         */
        checkMissedConstraints()

        val freshTypeConstructor = variable.freshTypeConstructor()
        val variableWithConstraints = notFixedTypeVariables.remove(freshTypeConstructor)

        for (otherVariableWithConstraints in notFixedTypeVariables.values) {
            otherVariableWithConstraints.removeConstrains { containsTypeVariable(it.type, freshTypeConstructor) }
        }

        storage.fixedTypeVariables[freshTypeConstructor] = resultType

        // Substitute freshly fixed type variable into missed constraints
        substituteMissedConstraints()

        postponeOnlyInputTypesCheck(variableWithConstraints, resultTypeForOnlyInputTypes)

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
//        if (variableWithConstraints != null && variableWithConstraints.typeVariable.hasOnlyInputTypesAttribute()) {
//            postponedComputationsAfterAllVariablesAreFixed.add { checkOnlyInputTypesAnnotation(variableWithConstraints, resultType) }
//        }
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
            if (!applyConstraintsFromFirstSuccessfulBranchOfTheFork(forkPointData, position)) {
                addError(NoSuccessfulFork(position))
            }
        }
    }

    /**
     * @return true if there is a successful constraints set for the fork
     */
    private fun applyConstraintsFromFirstSuccessfulBranchOfTheFork(
        forkPointData: ForkPointData,
        position: IncorporationConstraintPosition,
    ): Boolean {
        return forkPointData.any { constraintSetForForkBranch ->
            runTransaction {
                constraintInjector.processGivenForkPointBranchConstraints(
                    this@NewConstraintSystemImpl.apply {
                        checkState(
                            State.BUILDING,
                            State.COMPLETION,
                            State.TRANSACTION
                        )
                    },
                    constraintSetForForkBranch,
                    position,
                )

                if (constraintsFromAllForkPoints.isNotEmpty()) {
                    resolveForkPointsConstraints()
                }

                !hasContradiction
            }
        }
    }

    @COnly
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
    override fun isReified(variable: TypeVariableMarker): Boolean {
        return with(utilContext) { variable.isReified() }
    }

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
