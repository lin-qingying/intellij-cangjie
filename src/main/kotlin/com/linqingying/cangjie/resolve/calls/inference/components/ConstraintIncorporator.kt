package com.linqingying.cangjie.resolve.calls.inference.components


import com.linqingying.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus
import com.linqingying.cangjie.resolve.calls.inference.model.*
import com.linqingying.cangjie.types.AbstractTypeApproximator
import com.linqingying.cangjie.types.TypeApproximatorConfiguration
import com.linqingying.cangjie.types.model.*
import com.linqingying.cangjie.utils.SmartSet
import com.intellij.util.SmartList

// todo problem: intersection types in constrains: A <: Number, B <: Inv<A & Any> =>? B <: Inv<out Number & Any>
class ConstraintIncorporator(
    val typeApproximator: AbstractTypeApproximator,
    val trivialConstraintTypeInferenceOracle: TrivialConstraintTypeInferenceOracle,
    val utilContext: ConstraintSystemUtilContext
) {
    // A <:(=) \alpha <:(=) B => A <: B
    private fun Context.directWithVariable(
        typeVariable: TypeVariableMarker,
        constraint: Constraint
    ) {
        val shouldBeTypeVariableFlexible =
            if (useRefinedBoundsForTypeVariableInFlexiblePosition())
                false
            else
                with(utilContext) { typeVariable.shouldBeFlexible() }

        // \alpha <: constraint.type
        if (constraint.kind != ConstraintKind.LOWER) {
            forEachConstraint(typeVariable) {
                if (it.kind != ConstraintKind.UPPER) {
                    addNewIncorporatedConstraint(
                        it.type,
                        constraint.type,
                        shouldBeTypeVariableFlexible,
                        it.isNullabilityConstraint
                    )
                }
            }
        }

        // constraint.type <: \alpha
        if (constraint.kind != ConstraintKind.UPPER) {
            forEachConstraint(typeVariable) {
                if (it.kind != ConstraintKind.LOWER) {
                    val isFromDeclaredUpperBound =
                        it.position.from is DeclaredUpperBoundConstraintPosition<*> && !it.type.typeConstructor()
                            .isTypeVariable()

                    addNewIncorporatedConstraint(
                        constraint.type,
                        it.type,
                        shouldBeTypeVariableFlexible,
                        isFromDeclaredUpperBound = isFromDeclaredUpperBound
                    )
                }
            }
        }
    }

    private fun Context.areThereRecursiveConstraints(typeVariable: TypeVariableMarker, constraint: Constraint) =
        constraint.type.contains {
            it.typeConstructor().unwrapStubTypeVariableConstructor() == typeVariable.freshTypeConstructor()
        }

    // \alpha is typeVariable, \beta -- other type variable registered in ConstraintStorage
    fun incorporate(c: Context, typeVariable: TypeVariableMarker, constraint: Constraint) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled()

        // we shouldn't incorporate recursive constraint -- It is too dangerous
        if (c.areThereRecursiveConstraints(typeVariable, constraint)) return

        c.directWithVariable(typeVariable, constraint)
        c.insideOtherConstraint(typeVariable, constraint)
    }

    private fun Context.insideOtherConstraint(
        typeVariable: TypeVariableMarker,
        constraint: Constraint
    ) {
        val freshTypeConstructor = typeVariable.freshTypeConstructor()
        for (typeVariableWithConstraint in this@insideOtherConstraint.allTypeVariablesWithConstraints) {
            val constraintsWhichConstraintMyVariable = typeVariableWithConstraint.constraints.filter {
                containsTypeVariable(it.type, freshTypeConstructor)
            }
            constraintsWhichConstraintMyVariable.forEach {
                generateNewConstraint(typeVariableWithConstraint.typeVariable, it, typeVariable, constraint)
            }
        }
    }

    private fun Context.generateNewConstraint(
        targetVariable: TypeVariableMarker,
        baseConstraint: Constraint,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint
    ) {
        val isBaseGenericType = baseConstraint.type.argumentsCount() != 0
        val isBaseOrOtherCapturedType = baseConstraint.type.isCapturedType() || otherConstraint.type.isCapturedType()
        val (type, needApproximation) = when (otherConstraint.kind) {
            ConstraintKind.EQUALITY -> {
                otherConstraint.type to false
            }

            ConstraintKind.UPPER -> {
                /*
                 * Creating a captured type isn't needed due to its future approximation to `Nothing` or itself
                 * Example:
                 *      targetVariable = TypeVariable(A)
                 *      baseConstraint = LOWER(TypeVariable(B))
                 *      otherConstraint = UPPER(Number)
                 *      incorporatedConstraint = Approx(CapturedType(out Number)) <: TypeVariable(A) => Nothing <: TypeVariable(A)
                 * TODO: implement this for generics and captured types
                 */
                if (baseConstraint.kind == ConstraintKind.LOWER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    nothingType() to false
                } else if (baseConstraint.kind == ConstraintKind.UPPER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    otherConstraint.type to false
                } else {
                    createCapturedType(
                        createTypeArgument(otherConstraint.type, TypeVariance.INV),
                        listOf(otherConstraint.type),
                        null,
                        CaptureStatus.FOR_INCORPORATION
                    ) to true
                }
            }

            ConstraintKind.LOWER -> {
                /*
                 * Creating a captured type isn't needed due to its future approximation to `Any?` or itself
                 * Example:
                 *      targetVariable = TypeVariable(A)
                 *      baseConstraint = UPPER(TypeVariable(B))
                 *      otherConstraint = LOWER(Number)
                 *      incorporatedConstraint = TypeVariable(A) <: Approx(CapturedType(in Number)) => TypeVariable(A) <: Any?
                 * TODO: implement this for generics and captured types
                 */
                if (baseConstraint.kind == ConstraintKind.UPPER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    anyType() to false
                } else if (baseConstraint.kind == ConstraintKind.LOWER && !isBaseGenericType && !isBaseOrOtherCapturedType) {
                    otherConstraint.type to false
                } else {
                    createCapturedType(
                        createTypeArgument(otherConstraint.type, TypeVariance.INV),
                        emptyList(),
                        otherConstraint.type,
                        CaptureStatus.FOR_INCORPORATION
                    ) to true
                }
            }
        }

        approximateIfNeededAndAddNewConstraint(
            baseConstraint,
            type,
            targetVariable,
            otherVariable,
            otherConstraint,
            needApproximation
        )
    }

    private fun CangJieTypeMarker.substitute(
        c: Context,
        typeVariable: TypeVariableMarker,
        value: CangJieTypeMarker
    ): CangJieTypeMarker {
        val substitutor = c.typeSubstitutorByTypeConstructor(mapOf(typeVariable.freshTypeConstructor(c) to value))
        return substitutor.safeSubstitute(c, this)
    }

    private fun Context.getNestedTypeVariables(type: CangJieTypeMarker): List<TypeVariableMarker> =
        getNestedArguments(type).mapNotNullTo(SmartList()) {
            getTypeVariable(it.getType().typeConstructor().unwrapStubTypeVariableConstructor())
        }

    private fun Context.isPotentialUsefulNullabilityConstraint(
        newConstraint: CangJieTypeMarker,
        otherConstraint: CangJieTypeMarker,
        kind: ConstraintKind
    ): Boolean {
        if (trivialConstraintTypeInferenceOracle.isSuitableResultedType(newConstraint)) return false

        val otherConstraintCanAddNullabilityToNewOne =
            !newConstraint.isNullableType() && otherConstraint.isNullableType() && kind == ConstraintKind.LOWER
        val newConstraintCanAddNullabilityToOtherOne =
            newConstraint.isNullableType() && !otherConstraint.isNullableType() && kind == ConstraintKind.UPPER

        return otherConstraintCanAddNullabilityToNewOne || newConstraintCanAddNullabilityToOtherOne
    }

    private fun Context.containsConstrainingTypeWithoutProjection(
        newConstraint: CangJieTypeMarker,
        otherConstraint: Constraint
    ): Boolean {
        return getNestedArguments(newConstraint).any {
            it.getType().typeConstructor() == otherConstraint.type.typeConstructor() && it.getVariance() == TypeVariance.INV
        }
    }

    private fun Context.addNewConstraint(
        targetVariable: TypeVariableMarker,
        baseConstraint: Constraint,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint,
        newConstraint: CangJieTypeMarker,
        isSubtype: Boolean
    ) {
        if (targetVariable in getNestedTypeVariables(newConstraint)) return

        val isUsefulForNullabilityConstraint =
            isPotentialUsefulNullabilityConstraint(newConstraint, otherConstraint.type, otherConstraint.kind)
        val isFromVariableFixation = baseConstraint.position.from is FixVariableConstraintPosition<*>
                || otherConstraint.position.from is FixVariableConstraintPosition<*>

        if (!otherConstraint.kind.isEqual() &&
            !isUsefulForNullabilityConstraint &&
            !isFromVariableFixation &&
            !containsConstrainingTypeWithoutProjection(newConstraint, otherConstraint)
        ) return

        if (trivialConstraintTypeInferenceOracle.isGeneratedConstraintTrivial(
                baseConstraint, otherConstraint, newConstraint, isSubtype
            )
        ) return

        val derivedFrom = SmartSet.create(baseConstraint.derivedFrom).also { it.addAll(otherConstraint.derivedFrom) }
        if (otherVariable in derivedFrom) return

        derivedFrom.add(otherVariable)

        val kind = if (isSubtype) ConstraintKind.LOWER else ConstraintKind.UPPER

        val inputTypePosition =
            baseConstraint.position.from as? OnlyInputTypeConstraintPosition
                ?: baseConstraint.inputTypePositionBeforeIncorporation

        val isNewConstraintUsefulForNullability = isUsefulForNullabilityConstraint && newConstraint.isNullableNothing()
        val isOtherConstraintUsefulForNullability =
            otherConstraint.isNullabilityConstraint && otherConstraint.type.isNullableNothing()
        val isNullabilityConstraint = isNewConstraintUsefulForNullability || isOtherConstraintUsefulForNullability

        val constraintContext = ConstraintContext(kind, derivedFrom, inputTypePosition, isNullabilityConstraint)

        addNewIncorporatedConstraint(targetVariable, newConstraint, constraintContext)
    }

    private fun Context.approximateIfNeededAndAddNewConstraint(
        baseConstraint: Constraint,
        type: CangJieTypeMarker,
        targetVariable: TypeVariableMarker,
        otherVariable: TypeVariableMarker,
        otherConstraint: Constraint,
        needApproximation: Boolean = true
    ) {
        val typeWithSubstitution = baseConstraint.type.substitute(this, otherVariable, type)
        val prepareType = { toSuper: Boolean ->
            if (needApproximation) approximateCapturedTypes(typeWithSubstitution, toSuper) else typeWithSubstitution
        }

        if (baseConstraint.kind != ConstraintKind.LOWER) {
            addNewConstraint(
                targetVariable,
                baseConstraint,
                otherVariable,
                otherConstraint,
                prepareType(true),
                isSubtype = false
            )
        }
        if (baseConstraint.kind != ConstraintKind.UPPER) {
            addNewConstraint(
                targetVariable,
                baseConstraint,
                otherVariable,
                otherConstraint,
                prepareType(false),
                isSubtype = true
            )
        }
    }

    private fun approximateCapturedTypes(type: CangJieTypeMarker, toSuper: Boolean): CangJieTypeMarker =
        if (toSuper) typeApproximator.approximateToSuperType(
            type,
            TypeApproximatorConfiguration.IncorporationConfiguration
        ) ?: type
        else typeApproximator.approximateToSubType(type, TypeApproximatorConfiguration.IncorporationConfiguration)
            ?: type

    private inline fun Context.forEachConstraint(typeVariable: TypeVariableMarker, action: (Constraint) -> Unit) {
        // We use an indexed loop because the collection might be modified during the iteration.
        // However, the only modification is appending, so we should be fine.
        val constraints = getConstraintsForVariable(typeVariable)
        var i = 0
        while (i < constraints.size) {
            action(constraints[i++])
        }
    }

    interface Context : TypeSystemInferenceExtensionContext {
        val allTypeVariablesWithConstraints: Collection<VariableWithConstraints>

        // if such type variable is fixed then it is error
        fun getTypeVariable(typeConstructor: TypeConstructorMarker): TypeVariableMarker?

        fun getConstraintsForVariable(typeVariable: TypeVariableMarker): List<Constraint>

        fun addNewIncorporatedConstraint(
            lowerType: CangJieTypeMarker,
            upperType: CangJieTypeMarker,
            shouldTryUseDifferentFlexibilityForUpperType: Boolean,
            isFromNullabilityConstraint: Boolean = false,
            isFromDeclaredUpperBound: Boolean = false
        )

        fun addNewIncorporatedConstraint(
            typeVariable: TypeVariableMarker,
            type: CangJieTypeMarker,
            constraintContext: ConstraintContext
        )
    }
}

private fun TypeSystemInferenceExtensionContext.getNestedArguments(type: CangJieTypeMarker): List<TypeArgumentMarker> {
    val result = SmartList<TypeArgumentMarker>()
    val stack = ArrayDeque<TypeArgumentMarker>()

    when (type) {
        is FlexibleTypeMarker -> {
            stack.add(createTypeArgument(type.lowerBound(), TypeVariance.INV))
            stack.add(createTypeArgument(type.upperBound(), TypeVariance.INV))
        }

        else -> stack.add(createTypeArgument(type, TypeVariance.INV))
    }

    stack.add(createTypeArgument(type, TypeVariance.INV))

    val addArgumentsToStack = { projectedType: CangJieTypeMarker ->
        for (argumentIndex in 0 until projectedType.argumentsCount()) {
            stack.add(projectedType.getArgument(argumentIndex))
        }
    }

    while (!stack.isEmpty()) {
        val typeProjection = stack.removeFirst()


        result.add(typeProjection)

        when (val projectedType = typeProjection.getType()) {
            is FlexibleTypeMarker -> {
                addArgumentsToStack(projectedType.lowerBound())
                addArgumentsToStack(projectedType.upperBound())
            }

            else -> addArgumentsToStack(projectedType)
        }
    }
    return result
}
