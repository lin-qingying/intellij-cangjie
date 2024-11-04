package com.linqingying.cangjie.resolve.calls.inference.components

import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.resolve.calls.NewCommonSuperTypeCalculator
import com.linqingying.cangjie.resolve.calls.inference.ConstraintSystemBuilder
import com.linqingying.cangjie.resolve.calls.inference.model.*
import com.linqingying.cangjie.resolve.calls.inference.runTransaction
import com.linqingying.cangjie.types.AbstractTypeApproximator
import com.linqingying.cangjie.types.AbstractTypeChecker
import com.linqingying.cangjie.types.TypeApproximatorConfiguration
import com.linqingying.cangjie.types.error.ErrorType
import com.linqingying.cangjie.types.model.*


class ResultTypeResolver(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    private val languageVersionSettings: LanguageVersionSettings
) {
    interface Context : TypeSystemInferenceExtensionContext, ConstraintSystemBuilder {
        val notFixedTypeVariables: Map<TypeConstructorMarker, VariableWithConstraints>
        val outerSystemVariablesPrefixSize: Int
        fun buildNotFixedVariablesToStubTypesSubstitutor(): TypeSubstitutorMarker
//        fun isReified(variable: TypeVariableMarker): Boolean
    }

    private val isTypeInferenceForSelfTypesSupported: Boolean
        get() = languageVersionSettings.supportsFeature(LanguageFeature.TypeInferenceOnCallsWithSelfTypes)
//
//    private fun Context.getDefaultTypeForSelfType(
//        constraints: List<Constraint>,
//        typeVariable: TypeVariableMarker
//    ): CangJieTypeMarker? {
//        val typeVariableConstructor = typeVariable.freshTypeConstructor() as TypeVariableTypeConstructorMarker
//        val typesForRecursiveTypeParameters = constraints.mapNotNull { constraint ->
//            if (constraint.position.from !is DeclaredUpperBoundConstraintPosition<*>) return@mapNotNull null
//            val typeParameter = typeVariableConstructor.typeParameter ?: return@mapNotNull null
//            extractTypeForGivenRecursiveTypeParameter(constraint.type, typeParameter)
//        }.takeIf { it.isNotEmpty() } ?: return null
//
//        return createCapturedStarProjectionForSelfType(typeVariableConstructor, typesForRecursiveTypeParameters)
//    }

    private fun Context.getDefaultType(
        direction: TypeVariableDirectionCalculator.ResolveDirection,
        constraints: List<Constraint>,
        typeVariable: TypeVariableMarker
    ): CangJieTypeMarker {
//        if (isTypeInferenceForSelfTypesSupported) {
//            getDefaultTypeForSelfType(constraints, typeVariable)?.let { return it }
//        }

        return if (direction == TypeVariableDirectionCalculator.ResolveDirection.TO_SUBTYPE) nothingType() else anyType()
    }

    fun findResultType(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection
    ): CangJieTypeMarker {
        findResultTypeOrNull(c, variableWithConstraints, direction)?.let { return it }

        // no proper constraints
        return c.getDefaultType(direction, variableWithConstraints.constraints, variableWithConstraints.typeVariable)
    }

    private fun CangJieTypeMarker.approximateToSuperTypeOrSelf(
        c: Context,
        superTypeCandidate: CangJieTypeMarker?
    ): CangJieTypeMarker {
        // In case we have an ILT as the subtype, we approximate it using the upper type as the expected type.
        // This is more precise than always approximating it to Int or UInt.
        // Note, we shouldn't have nested ILTs because they can only appear as a constraint on a type variable
        // that we would have fixed earlier.
        if (typeConstructor(c).isIntegerLiteralTypeConstructor(c)) {
            return typeApproximator.approximateToSuperType(
                this,
                TypeApproximatorConfiguration.TopLevelIntegerLiteralTypeApproximationWithExpectedType(superTypeCandidate)
            ) ?: this
        }

        return typeApproximator.approximateToSuperType(this, TypeApproximatorConfiguration.InternalTypesApproximation)
            ?: this
    }

    private fun CangJieTypeMarker.approximateToSubTypeOrSelf(): CangJieTypeMarker {
        return typeApproximator.approximateToSubType(this, TypeApproximatorConfiguration.InternalTypesApproximation)
            ?: this
    }

    private val useImprovedCapturedTypeApproximation: Boolean =
        languageVersionSettings.supportsFeature(LanguageFeature.ImprovedCapturedTypeApproximationInInference)

    fun findResultTypeOrNull(
        c: Context,
        variableWithConstraints: VariableWithConstraints,
        direction: TypeVariableDirectionCalculator.ResolveDirection,
    ): CangJieTypeMarker? {
        val resultTypeFromEqualConstraint = findResultIfThereIsEqualsConstraint(c, variableWithConstraints)
        if (resultTypeFromEqualConstraint != null) {
            with(c) {
                if (!resultTypeFromEqualConstraint.contains { type ->
                        type.typeConstructor().isIntegerLiteralConstantTypeConstructor()
                    }
                ) {

                    return resultTypeFromEqualConstraint
                }
            }
        }

        val subType = c.findSubType(variableWithConstraints)
        val superType = c.findSuperType(variableWithConstraints)

        val (preparedSubType, preparedSuperType) = c.prepareSubAndSuperTypesLegacy(
            subType,
            superType,
            variableWithConstraints
        )


        val resultTypeFromDirection =
            if (direction == TypeVariableDirectionCalculator.ResolveDirection.TO_SUBTYPE || direction == TypeVariableDirectionCalculator.ResolveDirection.UNKNOWN) {
                c.resultType(preparedSubType, preparedSuperType, variableWithConstraints)
            } else {
                c.resultType(preparedSuperType, preparedSubType, variableWithConstraints)
            }

        // In the general case, we can have here two types, one from EQUAL constraint which must be ILT-based,
        // and the second one from UPPER/LOWER constraints (subType/superType based)
        // The logic of choice here is:
        // - if one type is null, we return another one
        // - we return type from UPPER/LOWER constraints if it's more precise (in fact, only Int/Short/Byte/Long is allowed here)
        // - otherwise we return ILT-based type
        return when {
            resultTypeFromEqualConstraint == null -> resultTypeFromDirection
            resultTypeFromDirection == null -> resultTypeFromEqualConstraint
            with(c) { !resultTypeFromDirection.typeConstructor().isNothingConstructor() } &&
                    AbstractTypeChecker.isSubtypeOf(
                        c,
                        resultTypeFromDirection,
                        resultTypeFromEqualConstraint
                    ) -> resultTypeFromDirection

            else -> resultTypeFromEqualConstraint
        }
    }

    /**
     * The general approach to approximation of resulting types (in K2) is to
     * - always approximate ILTs
     * - always approximate captured types unless this leads to a contradiction.
     * A contradiction can appear if we have some captured type C = CapturedType(*) in the subtype and in the supertype.
     *
     * Example: A<C> <: T <: A<C>
     *
     * If we were to approximate the result type, we would end up with a contradiction
     * A<*> </: A<C>
     *
     * In comparison, types from equality constraints are never approximated because it would always lead to a contradiction.
     * We evaluated a never-approximate approach but found it to be infeasible as it introduces many new errors
     * (type mismatches, REIFIED_TYPE_FORBIDDEN_SUBSTITUTION, TYPE_INFERENCE_ONLY_INPUT_TYPES_ERROR, etc.).
     */
    private fun Context.prepareSubAndSuperTypes(
        subType: CangJieTypeMarker?,
        superType: CangJieTypeMarker?,
        variableWithConstraints: VariableWithConstraints,
    ): Pair<CangJieTypeMarker?, CangJieTypeMarker?> {
        val approximatedSubType = subType?.approximateToSuperTypeOrSelf(this, superType)
        val approximatedSuperType = superType?.approximateToSubTypeOrSelf()

        val preparedSubType = when {
            approximatedSubType == null -> null
            shouldBeUsedWithoutApproximation(subType, approximatedSubType, variableWithConstraints, this) -> subType
            else -> approximatedSubType
        }

        val preparedSuperType = when {
            approximatedSuperType == null -> null
            shouldBeUsedWithoutApproximation(
                superType,
                approximatedSuperType,
                variableWithConstraints,
                this
            ) -> superType
//            hasRecursiveTypeParametersWithGivenSelfType(superType.typeConstructor(this)) -> superType
            else -> approximatedSuperType
            // Super type should be the most flexible, sub type should be the least one
        }.makeFlexibleIfNecessary(this, variableWithConstraints.constraints)

        return preparedSubType to preparedSuperType
    }

    /**
     * Returns `true` if using [approximatedResultType] as result type leads to a contradiction.
     *
     * If [resultType] and [approximatedResultType] are referentially equal, it means there is nothing to approximate in the first place.
     * Therefore `false` is returned.
     *
     * Only used when [LanguageFeature.ImprovedCapturedTypeApproximationInInference] is enabled.
     */
    private fun shouldBeUsedWithoutApproximation(
        resultType: CangJieTypeMarker,
        approximatedResultType: CangJieTypeMarker,
        variableWithConstraints: VariableWithConstraints,
        c: Context,
    ): Boolean {
        if (resultType === approximatedResultType || c.hasContradiction) return false

            if (resultType.typeConstructor(c).isIntegerLiteralTypeConstructor(c)) return false

        var createsContradiction = false
        c.runTransaction {
            addEqualityConstraint(
                approximatedResultType,
                variableWithConstraints.typeVariable.defaultType(c),
                SimpleConstraintSystemConstraintPosition
            )
            createsContradiction = hasContradiction
            false
        }
        return createsContradiction
    }

    private fun Context.prepareSubAndSuperTypesLegacy(
        subType: CangJieTypeMarker?,
        superType: CangJieTypeMarker?,
        variableWithConstraints: VariableWithConstraints,
    ): Pair<CangJieTypeMarker?, CangJieTypeMarker?> {


        val preparedSubType = when {
            subType == null -> null

            else -> typeApproximator.approximateToSuperType(
                subType,
                TypeApproximatorConfiguration.InternalTypesApproximation
            ) ?: subType
        }

        val preparedSuperType = when {
            superType == null -> null

            else -> typeApproximator.approximateToSubType(
                superType,
                TypeApproximatorConfiguration.InternalTypesApproximation
            ) ?: superType
            // Super type should be the most flexible, sub type should be the least one
        }.makeFlexibleIfNecessary(this, variableWithConstraints.constraints)

        return preparedSubType to preparedSuperType
    }

    /**
     * Old heuristic used to determine when result types from lower/upper constraints should be approximated or not.
     *
     * Becomes obsolete after [LanguageFeature.ImprovedCapturedTypeApproximationInInference] is enabled.
     */
    private fun Context.similarOrCloselyBoundCapturedTypes(
        subType: CangJieTypeMarker?,
        superType: CangJieTypeMarker?
    ): Boolean {
        if (subType == null) return false
        if (superType == null) return false
        val subTypeLowerConstructor = subType.lowerBoundIfFlexible().typeConstructor()
        if (!subTypeLowerConstructor.isCapturedTypeConstructor()) return false

        if (superType in subTypeLowerConstructor.supertypes() && superType.contains {
                it.typeConstructor().isCapturedTypeConstructor()
            }) {
            return true
        }

        return subTypeLowerConstructor == subType.upperBoundIfFlexible().typeConstructor() &&
                subTypeLowerConstructor == superType.lowerBoundIfFlexible().typeConstructor() &&
                subTypeLowerConstructor == superType.upperBoundIfFlexible().typeConstructor()
    }

    /*
     * We propagate nullness flexibility into the result type from type variables in other constraints
     * to prevent variable fixation into less flexible type.
     *  Constraints:
     *      UPPER(TypeVariable(T)..TypeVariable(T)?)
     *      UPPER(Foo?)
     *  Result type = makeFlexibleIfNecessary(Foo?) = Foo!
     *
     * We don't propagate nullness flexibility in depth as it's non-determined for now  :
     *  CST(Bar<Foo>, Bar<Foo!>) = Bar<Foo!>
     *  CST(Bar<Foo!>, Bar<Foo>) = Bar<Foo>
     * But: CST(Foo, Foo!) = CST(Foo!, Foo) = Foo!
     */
    private fun CangJieTypeMarker?.makeFlexibleIfNecessary(c: Context, constraints: List<Constraint>) = with(c) {
        when (val type = this@makeFlexibleIfNecessary) {
            is SimpleTypeMarker -> {
                if (constraints.any {
                        it.type.typeConstructor().isTypeVariable() && it.type.hasFlexibleNullability()
                    }) {
                    createFlexibleType(type.makeSimpleTypeDefinitelyNotNullOrNotNull(), type.withNullability(true))
                } else type
            }

            else -> type
        }
    }

    private fun Context.resultType(
        firstCandidate: CangJieTypeMarker?,
        secondCandidate: CangJieTypeMarker?,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        if (firstCandidate == null || secondCandidate == null) return firstCandidate ?: secondCandidate
        if (firstCandidate is ErrorType) return firstCandidate
        specialResultForIntersectionType(firstCandidate, secondCandidate)?.let { intersectionWithAlternative ->
            return intersectionWithAlternative
        }

        if (isSuitableType(firstCandidate, variableWithConstraints)) return firstCandidate

        return if (isSuitableType(secondCandidate, variableWithConstraints)) {
            secondCandidate
        } else {
            firstCandidate
        }
    }

    private fun Context.specialResultForIntersectionType(
        firstCandidate: CangJieTypeMarker,
        secondCandidate: CangJieTypeMarker,
    ): CangJieTypeMarker? {
        if (firstCandidate.typeConstructor().isIntersection()) {
            if (!AbstractTypeChecker.isSubtypeOf(this, firstCandidate.toPublicType(), secondCandidate.toPublicType())) {
                return createTypeWithUpperBoundForIntersectionResult(firstCandidate, secondCandidate)
            }
        }

        return null
    }

    private fun CangJieTypeMarker.toPublicType(): CangJieTypeMarker =
        typeApproximator.approximateToSuperType(
            this,
            TypeApproximatorConfiguration.PublicDeclaration.SaveAnonymousTypes
        ) ?: this

    private fun Context.isSuitableType(
        resultType: CangJieTypeMarker,
        variableWithConstraints: VariableWithConstraints
    ): Boolean {
        val filteredConstraints = variableWithConstraints.constraints.filter { isProperTypeForFixation(it.type) }


        for (constraint in filteredConstraints) {
            if (!checkConstraint(this, constraint.type, constraint.kind, resultType)) return false
        }

        // if resultType is not Nothing
        if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(resultType)) return true

        // Nothing and Nothing? is not allowed for reified parameters
//        if (isReified(variableWithConstraints.typeVariable)) return false

        // It's ok to fix result to non-nullable Nothing and parameter is not reified
        if (!resultType.isNullableType()) return true

        return isNullableNothingMayBeConsideredAsSuitableResultType(filteredConstraints)
    }

    private fun Context.isNullableNothingMayBeConsideredAsSuitableResultType(constraints: List<Constraint>): Boolean =
        when {

            else -> !isThereSingleLowerNullabilityConstraint(constraints)
        }

    private fun allUpperConstraintsAreFromBounds(constraints: List<Constraint>): Boolean =
        constraints.all {
            // Actually, at least for green code that should be an assertion that lower constraints (!isUpper) has `Nothing?` type
            // Because otherwise if we had `Nothing? <: T` and `SomethingElse <: T` than it would end with `SomethingElse? <: T`
            !it.kind.isUpper() || isFromTypeParameterUpperBound(it)
        }

    private fun isFromTypeParameterUpperBound(constraint: Constraint): Boolean =
        constraint.position.isFromDeclaredUpperBound || constraint.position.from is DeclaredUpperBoundConstraintPosition<*>

    private fun isThereSingleLowerNullabilityConstraint(constraints: List<Constraint>): Boolean {
        return constraints.singleOrNull { it.kind.isLower() }?.isNullabilityConstraint ?: false
    }

    //    推导类型
    @OptIn(COnly::class)
    private fun Context.findSubType(variableWithConstraints: VariableWithConstraints): CangJieTypeMarker? {
        val lowerConstraintTypes = prepareLowerConstraints(variableWithConstraints.constraints)

        if (lowerConstraintTypes.isNotEmpty()) {
            val types = sinkIntegerLiteralTypes(lowerConstraintTypes)
            var commonSuperType = computeCommonSuperType(types)

            if (commonSuperType.contains { it.asSimpleType()?.isStubTypeForVariableInSubtyping() == true }) {
                val typesWithoutStubs = types.filter { lowerType ->
                    !lowerType.contains { it.asSimpleType()?.isStubTypeForVariableInSubtyping() == true }
                }

                when {
                    typesWithoutStubs.isNotEmpty() -> {
                        commonSuperType = computeCommonSuperType(typesWithoutStubs)
                    }
                    // `typesWithoutStubs.isEmpty()` means that there are no lower constraints without type variables.
                    // It's only possible for the PCLA case, because otherwise none of the constraints would be considered as proper.
                    // So, we just get currently computed `commonSuperType` and substitute all local stub types
                    // with corresponding type variables.
                    outerSystemVariablesPrefixSize > 0 -> {
                        // outerSystemVariablesPrefixSize > 0 only for PCLA (K2)

                        commonSuperType =
                            createSubstitutionFromSubtypingStubTypesToTypeVariables().safeSubstitute(commonSuperType)
                    }
                }
            }

            return commonSuperType
        }

        return null
    }

    private fun Context.computeCommonSuperType(types: List<CangJieTypeMarker>): CangJieTypeMarker =
        with(NewCommonSuperTypeCalculator) { commonSuperType(types) }

    private fun Context.prepareLowerConstraints(constraints: List<Constraint>): List<CangJieTypeMarker> {
        var atLeastOneProper = false
        var atLeastOneNonProper = false

        val lowerConstraintTypes = mutableListOf<CangJieTypeMarker>()

        for (constraint in constraints) {
            if (constraint.kind != ConstraintKind.LOWER) continue

            val type = constraint.type

            lowerConstraintTypes.add(type)

            if (isProperTypeForFixation(type)) {
                atLeastOneProper = true
            } else {
                atLeastOneNonProper = true
            }
        }

        if (!atLeastOneProper) return emptyList()

        // PCLA slow path
        // We only allow using TVs fixation for nested PCLA calls
        if (outerSystemVariablesPrefixSize > 0) {
            val notFixedToStubTypesSubstitutor = buildNotFixedVariablesToStubTypesSubstitutor()
            return lowerConstraintTypes.map { notFixedToStubTypesSubstitutor.safeSubstitute(it) }
        }

        if (!atLeastOneNonProper) return lowerConstraintTypes

        val notFixedToStubTypesSubstitutor = buildNotFixedVariablesToStubTypesSubstitutor()

        return lowerConstraintTypes.map {
            if (isProperTypeForFixation(it)) it else notFixedToStubTypesSubstitutor.safeSubstitute(
                it
            )
        }
    }

    private fun Context.sinkIntegerLiteralTypes(types: List<CangJieTypeMarker>): List<CangJieTypeMarker> {
        return types.sortedBy { type ->

            val containsILT = type.contains { it.asSimpleType()?.isIntegerLiteralType() ?: false }
            if (containsILT) 1 else 0
        }
    }

    private fun Context.computeUpperType(upperConstraints: List<Constraint>): CangJieTypeMarker {
        return if (languageVersionSettings.supportsFeature(LanguageFeature.AllowEmptyIntersectionsInResultTypeResolver)) {
            intersectTypes(upperConstraints.map { it.type })
        } else {
            val intersectionUpperType = intersectTypes(upperConstraints.map { it.type })
            val resultIsActuallyIntersection = intersectionUpperType.typeConstructor().isIntersection()

            val isThereUnwantedIntersectedTypes = if (resultIsActuallyIntersection) {
                val intersectionSupertypes = intersectionUpperType.typeConstructor().supertypes()
                val intersectionClasses = intersectionSupertypes.count {
                    it.typeConstructor().isClassTypeConstructor() && !it.typeConstructor().isInterface()
                }
//                val areThereIntersectionFinalClasses = intersectionSupertypes.any { it.typeConstructor().isCommonFinalClassConstructor() }
                intersectionClasses > 1 /*|| areThereIntersectionFinalClasses*/
            } else false

//            val upperType = if (isThereUnwantedIntersectedTypes) {
//                /*
//                 * We shouldn't infer a type variable into the intersection type if there is an explicit expected type,
//                 * otherwise it can lead to something like this:
//                 *
//                 * fun <T : String> materialize(): T = null as T
//                 * val bar: Int = materialize() // no errors, T is inferred into String & Int
//                 */
//                val filteredUpperConstraints = upperConstraints.filterNot { it.isExpectedTypePosition() }.map { it.type }
//                if (filteredUpperConstraints.isNotEmpty()) intersectTypes(filteredUpperConstraints) else intersectionUpperType
//            } else intersectionUpperType
//            upperType

            intersectionUpperType
        }
    }

    private fun Context.findSuperType(variableWithConstraints: VariableWithConstraints): CangJieTypeMarker? {
        val upperConstraints =
            variableWithConstraints.constraints.filter {
                it.kind == ConstraintKind.UPPER && this@findSuperType.isProperTypeForFixation(
                    it.type
                )
            }

        if (upperConstraints.isNotEmpty()) {
            return computeUpperType(upperConstraints)
        }

        return null
    }

    private fun Context.isProperTypeForFixation(type: CangJieTypeMarker): Boolean =
        isProperTypeForFixation(type, notFixedTypeVariables.keys) { isProperType(it) }

    private fun findResultIfThereIsEqualsConstraint(
        c: Context,
        variableWithConstraints: VariableWithConstraints
    ): CangJieTypeMarker? {
        val properEqualityConstraints = variableWithConstraints.constraints.filter {
            it.kind == ConstraintKind.EQUALITY && c.isProperTypeForFixation(it.type)
        }

        return c.representativeFromEqualityConstraints(properEqualityConstraints)
    }

    // Discriminate integer literal types as they are less specific than separate integer types (Int, Short...)
    private fun Context.representativeFromEqualityConstraints(constraints: List<Constraint>): CangJieTypeMarker? {
        if (constraints.isEmpty()) return null

        val constraintTypes = constraints.map { it.type }
        val nonLiteralTypes = constraintTypes.filter { !it.typeConstructor().isIntegerLiteralTypeConstructor() }
        return nonLiteralTypes.singleBestRepresentative()
            ?: constraintTypes.singleBestRepresentative()
            ?: constraintTypes.first() // seems like constraint system has contradiction
    }
}
