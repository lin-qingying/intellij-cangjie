package com.huawei.cangjie.types

import com.huawei.cangjie.descriptors.CallableMemberDescriptor
import com.huawei.cangjie.descriptors.TypeParameterDescriptor
import com.huawei.cangjie.resolve.calls.inference.CallHandle
import com.huawei.cangjie.resolve.calls.inference.ConstraintSystemBuilderImpl
import com.huawei.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import com.huawei.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import com.huawei.cangjie.types.util.TypeNullability
import com.huawei.cangjie.types.util.isSubtypeOf
import com.huawei.cangjie.types.util.nullability
import java.util.HashSet

class FuzzyType(val type: CangJieType, freeParameters: Collection<TypeParameterDescriptor>)
{

    val freeParameters: Set<TypeParameterDescriptor>
    init {
        if (freeParameters.isNotEmpty()) {
            // we allow to pass type parameters from another function with the same original in freeParameters
            val usedTypeParameters = HashSet<TypeParameterDescriptor>().apply { addUsedTypeParameters(type) }
            if (usedTypeParameters.isNotEmpty()) {
                val originalFreeParameters = freeParameters.map { it.toOriginal() }.toSet()
                this.freeParameters = usedTypeParameters.filter { it.toOriginal() in originalFreeParameters }.toSet()
            } else {
                this.freeParameters = emptySet()
            }
        } else {
            this.freeParameters = emptySet()
        }
    }
    private fun MutableSet<TypeParameterDescriptor>.addUsedTypeParameters(type: CangJieType) {
        val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor
        if (typeParameter != null && add(typeParameter)) {
            typeParameter.upperBounds.forEach { addUsedTypeParameters(it) }
        }

        for (argument in type.arguments) {

                addUsedTypeParameters(argument.type)

        }
    }
    fun checkIsSuperTypeOf(otherType: FuzzyType): TypeSubstitutor? = matchedSubstitutor(otherType, MatchKind.IS_SUPERTYPE)
    @Suppress("USELESS_ELVIS")
    private fun TypeParameterDescriptor.toOriginal(): TypeParameterDescriptor {
        val callableDescriptor = containingDeclaration as? CallableMemberDescriptor ?: return this
        val original = callableDescriptor.original ?: error("original = null for $callableDescriptor")
        val typeParameters = original.typeParameters ?: error("typeParameters = null for $original")
        return typeParameters[index]
    }

    fun checkIsSubtypeOf(otherType: FuzzyType): TypeSubstitutor? = matchedSubstitutor(otherType, MatchKind.IS_SUBTYPE)
    private enum class MatchKind {
        IS_SUBTYPE,
        IS_SUPERTYPE
    }

    fun checkIsSuperTypeOf(otherType: CangJieType): TypeSubstitutor? = checkIsSuperTypeOf(otherType.toFuzzyType(emptyList()))
    private fun matchedSubstitutor(otherType: FuzzyType, matchKind: MatchKind): TypeSubstitutor? {
        if (type.isError) return null
        if (otherType.type.isError) return null
        if (otherType.type.isUnit() && matchKind == MatchKind.IS_SUBTYPE) return TypeSubstitutor.EMPTY

        fun CangJieType.checkInheritance(otherType: CangJieType): Boolean {
            return when (matchKind) {
                MatchKind.IS_SUBTYPE -> this.isSubtypeOf(otherType)
                MatchKind.IS_SUPERTYPE -> otherType.isSubtypeOf(this)
            }
        }

        if (freeParameters.isEmpty() && otherType.freeParameters.isEmpty()) {
            return if (type.checkInheritance(otherType.type)) TypeSubstitutor.EMPTY else null
        }

        val builder = ConstraintSystemBuilderImpl()
        val typeVariableSubstitutor = builder.registerTypeVariables(CallHandle.NONE, freeParameters + otherType.freeParameters)

        val typeInSystem = typeVariableSubstitutor.substitute(type, Variance.INVARIANT)
        val otherTypeInSystem = typeVariableSubstitutor.substitute(otherType.type, Variance.INVARIANT)

        when (matchKind) {
            MatchKind.IS_SUBTYPE ->
                builder.addSubtypeConstraint(typeInSystem, otherTypeInSystem, ConstraintPositionKind.RECEIVER_POSITION.position())
            MatchKind.IS_SUPERTYPE ->
                builder.addSubtypeConstraint(otherTypeInSystem, typeInSystem, ConstraintPositionKind.RECEIVER_POSITION.position())
        }

        builder.fixVariables()

        val constraintSystem = builder.build()

        if (constraintSystem.status.hasContradiction()) return null

        // currently ConstraintSystem return successful status in case there are problems with nullability
        // that's why we have to check subtyping manually
        val substitutor = constraintSystem.resultingSubstitutor
        val substitutedType = substitutor.substitute(type, Variance.INVARIANT) ?: return null
        if (substitutedType.isError) return TypeSubstitutor.EMPTY
        val otherSubstitutedType = substitutor.substitute(otherType.type, Variance.INVARIANT) ?: return null
        if (otherSubstitutedType.isError) return TypeSubstitutor.EMPTY
        if (!substitutedType.checkInheritance(otherSubstitutedType)) return null

        val substitutorToKeepCapturedTypes = object : DelegatedTypeSubstitution(substitutor.substitution) {
            override fun approximateCapturedTypes() = false
        }.buildSubstitutor()

        val substitutionMap: Map<TypeConstructor, TypeProjection> = constraintSystem.typeVariables
            .map { it.originalTypeParameter }
            .associateBy(
                keySelector = { it.typeConstructor },
                valueTransform = { parameterDescriptor ->
                    val typeProjection = TypeProjectionImpl(Variance.INVARIANT, parameterDescriptor.defaultType)
                    val substitutedProjection = substitutorToKeepCapturedTypes.substitute(typeProjection)
                    substitutedProjection?.takeUnless { ErrorUtils.containsUninferredTypeVariable(it.type) } ?: typeProjection
                })
        return TypeConstructorSubstitution.createByConstructorsMap(substitutionMap, approximateCapturedTypes = true).buildSubstitutor()
    }
}
fun CangJieType.toFuzzyType(freeParameters: Collection<TypeParameterDescriptor>) = FuzzyType(this, freeParameters)
fun FuzzyType.nullability()  = type.nullability()
