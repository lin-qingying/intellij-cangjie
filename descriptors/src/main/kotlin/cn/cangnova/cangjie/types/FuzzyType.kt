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

package cn.cangnova.cangjie.types

import cn.cangnova.cangjie.descriptors.CallableDescriptor
import cn.cangnova.cangjie.descriptors.CallableMemberDescriptor
import cn.cangnova.cangjie.descriptors.TypeParameterDescriptor
import cn.cangnova.cangjie.resolve.calls.inference.CallHandle
import cn.cangnova.cangjie.resolve.calls.inference.ConstraintSystemBuilderImpl
import cn.cangnova.cangjie.resolve.calls.inference.constraintPosition.ConstraintPositionKind
import cn.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isUnit
import cn.cangnova.cangjie.types.checker.StrictEqualityTypeChecker
import cn.cangnova.cangjie.types.util.*
import java.util.HashSet
/**
 * Replaces free parameters inside the type with corresponding type parameters of the class (when possible)
 */
fun FuzzyType.presentationType(): CangJieType {
    if (freeParameters.isEmpty()) return type

    val map = HashMap<TypeConstructor, TypeProjection>()
    for ((argument, typeParameter) in type.arguments.zip(type.constructor.parameters)) {
        if (argument.projectionKind == Variance.INVARIANT) {
            val equalToFreeParameter = freeParameters.firstOrNull {
                StrictEqualityTypeChecker.strictEqualTypes(it.defaultType, argument.type.unwrap())
            } ?: continue

            map[equalToFreeParameter.typeConstructor] = createProjection(typeParameter.defaultType, Variance.INVARIANT, null)
        }
    }
    val substitutor = TypeSubstitutor.create(map)
    return substitutor.substitute(type, Variance.INVARIANT)!!
}
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
    fun checkIsSubtypeOf(otherType: CangJieType): TypeSubstitutor? = checkIsSubtypeOf(otherType.toFuzzyType(emptyList()))

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
fun CallableDescriptor.fuzzyReturnType() = returnType?.toFuzzyType(typeParameters)
fun FuzzyType.isAlmostEverything(): Boolean {
    if (freeParameters.isEmpty()) return false
    val typeParameter = type.constructor.declarationDescriptor as? TypeParameterDescriptor ?: return false
    if (typeParameter !in freeParameters) return false
            return typeParameter.upperBounds.singleOrNull()?.isAnyOrOptionAny() ?: false
}
fun FuzzyType.makeNonOption() = type.makeNonOption().toFuzzyType(freeParameters)
fun CallableDescriptor.fuzzyExtensionReceiverType() = extensionReceiverParameter?.type?.toFuzzyType(typeParameters)
