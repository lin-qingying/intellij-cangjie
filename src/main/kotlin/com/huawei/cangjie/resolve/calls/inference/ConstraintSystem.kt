package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.calls.inference.constraintPosition.ConstraintPosition
import com.huawei.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import com.huawei.cangjie.resolve.calls.inference.constraintPosition.derivedFrom
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariable
import com.huawei.cangjie.types.*
import com.huawei.cangjie.types.checker.CangJieTypeChecker
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.util.TypeUtils
import com.huawei.cangjie.types.util.TypeUtils.DONT_CARE
import com.huawei.cangjie.types.util.makeNotNullable

interface ConstraintSystem {

    val status: ConstraintSystemStatus
    val typeVariables: Set<TypeVariable>
    fun toBuilder(filterConstraintPosition: (ConstraintPosition) -> Boolean = { true }): Builder
    /**
     * Returns the result of solving the constraint system (mapping from the type variable to the resulting type projection).
     * In the resulting substitution the following should be of concern:
     * - type constraints
     * - variance of the type variable  // not implemented yet
     * - type parameter bounds (that can bind type variables with each other) // not implemented yet
     * If the addition of the 'expected type' constraint made the system fail,
     * this constraint is not included in the resulting substitution.
     */
    val resultingSubstitutor: TypeSubstitutor
    /**
     * Returns the resulting type constraints of solving the constraint system for specific type parameter descriptor.
     * Throws IllegalArgumentException if the type parameter descriptor is not known to the system.
     */
    fun getTypeBounds(typeVariable: TypeVariable): TypeBounds
    interface Builder {
        /**
         * Registers variables in a constraint system. Returns a substitutor which maps type parameter descriptors passed as parameters
         * to the corresponding types of variables of the system. Use that substitutor to provide constraints to the system
         */
        fun registerTypeVariables(
            call: CallHandle, typeParameters: Collection<TypeParameterDescriptor>, external: Boolean = false
        ): TypeSubstitutor

        /**
         * Adds a constraint that the constraining type is a subtype of the subject type.
         * Asserts that only subject type may contain registered type variables.
         *
         * For example, for `fun <T> id(t: T) {}` to infer `T` in invocation `id(1)`
         * the constraint "Int is a subtype of T" should be generated where T is a subject type, and Int is a constraining type.
         */
        fun addSubtypeConstraint(
            constrainingType: CangJieType?,
            subjectType: CangJieType?,
            constraintPosition: ConstraintPosition
        )

        /**
         * For each call for which type variables were registered, a type substitutor is stored in this map which maps
         * type parameter descriptors of the candidate descriptor of that call -> type variables of the system.
         * Those are the same substitutors that are returned by [registerTypeVariables] at the time of variable registration.
         */
        val typeVariableSubstitutors: Map<CallHandle, TypeSubstitutor>

        /**
         * Add all variables and constraints from the other system to this one. The other system may not have any common variables
         * with this one, or even variables registered for calls, for which this system also has some registered variables
         */
        fun add(other: Builder)

        fun fixVariables()

        fun build(): ConstraintSystem
    }
}

internal class ConstraintSystemImpl(
    private val allTypeParameterBounds: Map<TypeVariable, TypeBoundsImpl>,
    private val usedInBounds: Map<TypeVariable, MutableList<TypeBounds.Bound>>,
    private val errors: List<ConstraintError>,
    private val initialConstraints: List<ConstraintSystemBuilderImpl.Constraint>,
    private val typeVariableSubstitutors: Map<CallHandle, TypeSubstitutor>
) : ConstraintSystem {

    private val localTypeParameterBounds: Map<TypeVariable, TypeBoundsImpl>
        get() = allTypeParameterBounds.filterNot { it.key.isExternal }

    private fun getSubstitutor(
        substituteOriginal: Boolean,
        getDefaultValue: (TypeVariable) -> CangJieType
    ): TypeSubstitutor {
        val parameterToInferredValueMap =
            getParameterToInferredValueMap(allTypeParameterBounds, getDefaultValue, substituteOriginal)
        return TypeSubstitutor.create(
            SubstitutionWithCapturedTypeApproximation(
//                SubstitutionFilteringInternalResolveAnnotations(
                TypeConstructorSubstitution.createByConstructorsMap(parameterToInferredValueMap)
//                )
            )
        )
    }

    private fun getParameterToInferredValueMap(
        typeParameterBounds: Map<TypeVariable, TypeBoundsImpl>,
        getDefaultType: (TypeVariable) -> CangJieType,
        substituteOriginal: Boolean
    ): Map<TypeConstructor, TypeProjection> {
        val substitutionContext = HashMap<TypeConstructor, TypeProjection>()
        for ((variable, typeBounds) in typeParameterBounds) {
            val value = typeBounds.value
            val typeConstructor =
                if (substituteOriginal) variable.originalTypeParameter.typeConstructor
                else variable.type.constructor
            val type =
                if (value != null && !TypeUtils.contains(value, DONT_CARE)) value
                else getDefaultType(variable)
            substitutionContext[typeConstructor] = TypeProjectionImpl(type)
        }
        return substitutionContext
    }

    private fun satisfyInitialConstraints(): Boolean {
        val substitutor = getSubstitutor(substituteOriginal = false) {
            ErrorUtils.createErrorType(ErrorTypeKind.UNINFERRED_TYPE_VARIABLE, it.originalTypeParameter.name.asString())
        }

        fun CangJieType.substitute(): CangJieType? = substitutor.substitute(this, Variance.INVARIANT)

        return initialConstraints.all { (kind, subtype, superType, position) ->
            val resultSubType = subtype.substitute()?.let {
                // the call might be done via safe access, so we check for notNullable receiver type;
                // 'unsafe call' error is reported otherwise later
                if (position.kind != ConstraintPositionKind.RECEIVER_POSITION) it else it.makeNotNullable()
            } ?: return false
            val resultSuperType = superType.substitute() ?: return false
            when (kind) {
                ConstraintSystemBuilderImpl.ConstraintKind.SUB_TYPE -> CangJieTypeChecker.DEFAULT.isSubtypeOf(
                    resultSubType,
                    resultSuperType
                )

                ConstraintSystemBuilderImpl.ConstraintKind.EQUAL -> CangJieTypeChecker.DEFAULT.equalTypes(
                    resultSubType,
                    resultSuperType
                )
            }
        }
    }

    override val status = object : ConstraintSystemStatus {
        // for debug ConstraintsUtil.getDebugMessageForStatus might be used

        override fun isSuccessful() = !hasContradiction() && !hasUnknownParameters() && satisfyInitialConstraints()

        override fun hasContradiction() = hasParameterConstraintError() || hasConflictingConstraints()
                || hasCannotCaptureTypesError() || errors.any { it is TypeInferenceError }



        override fun hasViolatedUpperBound() =
            !isSuccessful() && filterConstraintsOut(ConstraintPositionKind.TYPE_BOUND_POSITION).status.isSuccessful()

        override fun hasConflictingConstraints() = localTypeParameterBounds.values.any { it.values.size > 1 }

        override fun hasUnknownParameters() =
            localTypeParameterBounds.values.any { it.values.isEmpty() } || hasTypeParameterWithUnsatisfiedOnlyInputTypesError()

        override fun hasParameterConstraintError() = errors.any { it is ParameterConstraintError }

        override fun hasOnlyErrorsDerivedFrom(kind: ConstraintPositionKind): Boolean {
            if (isSuccessful()) return false
            if (filterConstraintsOut(kind).status.isSuccessful()) return true
            return errors.isNotEmpty() && errors.all { it.constraintPosition.derivedFrom(kind) }
        }

        override fun hasErrorInConstrainingTypes() = errors.any { it is ErrorInConstrainingType }

        override fun hasCannotCaptureTypesError() = errors.any { it is CannotCapture }

        override fun hasTypeInferenceIncorporationError() =
            errors.any { it is TypeInferenceError } || !satisfyInitialConstraints()

        override fun hasTypeParameterWithUnsatisfiedOnlyInputTypesError() =
            localTypeParameterBounds.values.any { it.typeVariable.hasOnlyInputTypesAnnotation() && it.value == null }

        override val constraintErrors: List<ConstraintError>
            get() = errors

    }
    override val typeVariables: Set<TypeVariable>
        get() = allTypeParameterBounds.keys

    override fun toBuilder(filterConstraintPosition: (ConstraintPosition) -> Boolean): ConstraintSystem.Builder {
        val result = ConstraintSystemBuilderImpl()
        for ((typeParameter, typeBounds) in allTypeParameterBounds) {
            result.allTypeParameterBounds.put(typeParameter, typeBounds.filter(filterConstraintPosition))
        }
        result.usedInBounds.putAll(usedInBounds.map {
            val (variable, bounds) = it
            variable to bounds.filterTo(arrayListOf<TypeBounds.Bound>()) { filterConstraintPosition(it.position) }
        }.toMap())
        result.errors.addAll(errors.filter { filterConstraintPosition(it.constraintPosition) })

        result.initialConstraints.addAll(initialConstraints.filter { filterConstraintPosition(it.position) })
        result.typeVariableSubstitutors.putAll(typeVariableSubstitutors)

        return result
    }

    override val resultingSubstitutor: TypeSubstitutor
        get() = TODO("Not yet implemented")

    override fun getTypeBounds(typeVariable: TypeVariable): TypeBoundsImpl {
        return allTypeParameterBounds[typeVariable]
            ?: throw IllegalArgumentException("TypeParameterDescriptor is not a type variable for constraint system: $typeVariable")
    }
}
//internal class SubstitutionFilteringInternalResolveAnnotations(substitution: TypeSubstitution) : DelegatedTypeSubstitution(substitution) {
//    override fun filterAnnotations(annotations: Annotations): Annotations {
//        if (!annotations.hasInternalAnnotationForResolve()) return annotations
//        return FilteredAnnotations(annotations) { !it.isInternalAnnotationForResolve() }
//    }
//}
