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

package com.linqingying.cangjie.resolve.calls.inference.components

import com.linqingying.cangjie.resolve.calls.inference.isCaptured
import com.linqingying.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import com.linqingying.cangjie.resolve.calls.inference.substitute
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.checker.NewCapturedType
import com.linqingying.cangjie.types.checker.NewCapturedTypeConstructor
import com.linqingying.cangjie.types.checker.intersectTypes
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.model.TypeSubstitutorMarker

interface NewTypeSubstitutor : TypeSubstitutorMarker {

    fun safeSubstitute(type: UnwrappedType): UnwrappedType =
        substitute(type, runCapturedChecks = true, keepAnnotation = true) ?: type

    fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType?
    val isEmpty: Boolean
    private fun substitute(type: UnwrappedType, keepAnnotation: Boolean, runCapturedChecks: Boolean): UnwrappedType? =
        when (type) {
            is SimpleType -> substitute(type, keepAnnotation, runCapturedChecks)
            is FlexibleType -> if (type is DynamicType /*|| type is RawType*/) {
                null
            } else {
                val lowerBound = substitute(type.lowerBound, keepAnnotation, runCapturedChecks)
                val upperBound = substitute(type.upperBound, keepAnnotation, runCapturedChecks)
                val enhancement = if (type is TypeWithEnhancement) {
                    substituteTypeEnhancement(type.enhancement, keepAnnotation, runCapturedChecks)
                } else null

                if (lowerBound == null && upperBound == null) {
                    null
                } else {
                    // todo discuss lowerIfFlexible and upperIfFlexible
                    CangJieTypeFactory.flexibleType(
                        lowerBound?.lowerIfFlexible() ?: type.lowerBound,
                        upperBound?.upperIfFlexible() ?: type.upperBound
                    ).wrapEnhancement(if (enhancement is TypeWithEnhancement) enhancement.enhancement else enhancement)
                }
            }
        }

    private fun substitute(type: SimpleType, keepAnnotation: Boolean, runCapturedChecks: Boolean): UnwrappedType? {
        if (type.isError) return null

        if (type is AbbreviatedType) {
            val substitutedExpandedType = substitute(type.expandedType, keepAnnotation, runCapturedChecks)
            val substitutedAbbreviation = substitute(type.abbreviation, keepAnnotation, runCapturedChecks)
            return when {
                substitutedExpandedType == null && substitutedAbbreviation == null -> null
                substitutedExpandedType is SimpleType? && substitutedAbbreviation is SimpleType? ->
                    AbbreviatedType(
                        substitutedExpandedType ?: type.expandedType,
                        substitutedAbbreviation ?: type.abbreviation
                    )

                else -> substitutedExpandedType
            }
        }

        if (type.arguments.isNotEmpty()) {
            return substituteParametrizedType(type, keepAnnotation, runCapturedChecks)
        }

        val typeConstructor = type.constructor

        if (typeConstructor is NewCapturedTypeConstructor) {
            if (!runCapturedChecks) return null

            assert(type is NewCapturedType || (type is DefinitelyNotNullType && type.original is NewCapturedType)) {

                "Type is inconsistent -- somewhere we create type with typeConstructor = $typeConstructor " +
                        "and class: ${type::class.java.canonicalName}. type.toString() = $type"
            }
            val capturedType =
                if (type is DefinitelyNotNullType) type.original as NewCapturedType else type as NewCapturedType

            val innerType = capturedType.lowerType ?: capturedType.constructor.projection.type.unwrap()
            val substitutedInnerType = substitute(innerType, keepAnnotation, runCapturedChecks = false)
            val substitutedSuperTypes =
                capturedType.constructor.supertypes.map {
                    substitute(it, keepAnnotation, runCapturedChecks = false) ?: it
                }

            if (substitutedInnerType != null) {
                return if (substitutedInnerType.isCaptured()) substitutedInnerType else {
                    NewCapturedType(
                        capturedType.captureStatus,
                        NewCapturedTypeConstructor(
                            TypeProjectionImpl(typeConstructor.projection.projectionKind, substitutedInnerType),
                            typeParameter = typeConstructor.typeParameter
                        ).also { it.initializeSupertypes(substitutedSuperTypes) },
                        lowerType = if (capturedType.lowerType != null) substitutedInnerType else null,
                        isMarkedOption = type.isMarkedOption
                    )
                }
            }

            if (AbstractTypeChecker.RUN_SLOW_ASSERTIONS) {
                typeConstructor.supertypes.forEach { supertype ->
                    substitute(supertype, keepAnnotation, runCapturedChecks = false)?.let {
                        throwExceptionAboutInvalidCapturedSubstitution(capturedType, supertype, it)
                    }
                }
            }

            return null
        }

        if (typeConstructor is IntersectionTypeConstructor) {
            fun updateNullability(substituted: UnwrappedType) =
                if (type.isMarkedOption) substituted.makeOptionalAsSpecified(true) else substituted

            substituteNotNullTypeWithConstructor(typeConstructor)?.let { return updateNullability(it) }
            var thereAreChanges = false
            val newTypes = typeConstructor.supertypes.map {
                substitute(it.unwrap(), keepAnnotation, runCapturedChecks)?.apply { thereAreChanges = true }
                    ?: it.unwrap()
            }
            if (!thereAreChanges) return null
            return updateNullability(intersectTypes(newTypes))
        }

        // 简单分类器类型
        var replacement = substituteNotNullTypeWithConstructor(typeConstructor) ?: return null
        if (keepAnnotation) {
            replacement = replacement.replaceAttributes(
                replacement.attributes.add(type.attributes)
            )
        }
        if (type.isMarkedOption) {
            replacement = replacement.makeOptionalAsSpecified(true)
        }
        if (type.isDefinitelyNotNullType) {
            replacement = replacement.makeDefinitelyNotNullOrNotNull()
        }
        if (type is CustomTypeParameter) {
            replacement = type.substitutionResult(replacement).unwrap()
        }

        return replacement
    }


    private fun substituteParametrizedType(
        type: SimpleType,
        keepAnnotation: Boolean,
        runCapturedChecks: Boolean
    ): UnwrappedType? {
        val parameters = type.constructor.parameters
        val arguments = type.arguments
        if (parameters.size != arguments.size) {
            // todo error type or exception?
            return ErrorUtils.createErrorType(
                ErrorTypeKind.TYPE_WITH_MISMATCHED_TYPE_ARGUMENTS_AND_PARAMETERS,
                type.toString(),
                parameters.size.toString(),
                arguments.size.toString()
            )
        }
        val newArguments = arrayOfNulls<TypeProjection?>(arguments.size)

        for (index in arguments.indices) {
            val argument = arguments[index]


            val specialProjectionSubstitution = substituteArgumentProjection(argument)
            if (specialProjectionSubstitution != null) {
                newArguments[index] = specialProjectionSubstitution
                continue
            }

            val substitutedArgumentType =
                substitute(argument.type.unwrap(), keepAnnotation, runCapturedChecks) ?: continue

            newArguments[index] = TypeProjectionImpl(argument.projectionKind, substitutedArgumentType)
        }

        if (newArguments.all { it == null }) return null

        val newArgumentsList = arguments.mapIndexed { index, oldArgument -> newArguments[index] ?: oldArgument }
        return type.replace(newArgumentsList)
    }

    /**
     * Returns not null when substitutor manages specific type projection substitution by itself.
     * Intended for corner cases involving interactions with legacy type substitutor,
     * please consider using substituteNotNullTypeWithConstructor instead of making manual projection substitutions.
     */
    fun substituteArgumentProjection(argument: TypeProjection): TypeProjection? {
        return null
    }

    private fun throwExceptionAboutInvalidCapturedSubstitution(
        capturedType: SimpleType,
        innerType: UnwrappedType,
        substitutedInnerType: UnwrappedType
    ): Nothing =
        throw IllegalStateException(
            "Illegal type substitutor: $this, " +
                    "because for captured type '$capturedType' supertype approximation should be null, but it is: '$innerType'," +
                    "original supertype: '$substitutedInnerType'"
        )

    private fun substituteTypeEnhancement(
        enhancementType: CangJieType,
        keepAnnotation: Boolean,
        runCapturedChecks: Boolean
    ) = when (val type = enhancementType.unwrap()) {
        is SimpleType -> substitute(type, keepAnnotation, runCapturedChecks) ?: enhancementType
        is FlexibleType -> {
            val substitutedLowerBound =
                substitute(type.lowerBound, keepAnnotation, runCapturedChecks) ?: type.lowerBound
            val substitutedUpperBound =
                substitute(type.upperBound, keepAnnotation, runCapturedChecks) ?: type.upperBound
            CangJieTypeFactory.flexibleType(
                substitutedLowerBound.lowerIfFlexible(),
                substitutedUpperBound.upperIfFlexible()
            )
        }
    }
}

object EmptySubstitutor : NewTypeSubstitutor {
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? = null

    override val isEmpty: Boolean get() = true
}

class NewTypeSubstitutorByConstructorMap(val map: Map<TypeConstructor, UnwrappedType>) : NewTypeSubstitutor {
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? = map[constructor]

    override val isEmpty: Boolean get() = map.isEmpty()
}

class FreshVariableNewTypeSubstitutor(val freshVariables: List<TypeVariableFromCallableDescriptor>) :
    NewTypeSubstitutor {

    val freshVariablesByMap = freshVariables.associateBy { it.originalTypeParameter.typeConstructor }
    override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? {
//        val indexProposal = (constructor.declarationDescriptor as? TypeParameterDescriptor)?.index ?: return null
//        val typeVariable = freshVariables.getOrNull(indexProposal) ?: return null
//        if (typeVariable.originalTypeParameter.typeConstructor != constructor) return null
//
//        return typeVariable.defaultType

        val typeVariable = freshVariablesByMap[constructor] ?: return null
        return typeVariable.defaultType
    }

    override val isEmpty: Boolean get() = freshVariables.isEmpty()

    companion object {
        val Empty = FreshVariableNewTypeSubstitutor(emptyList())
    }
}

fun TypeSubstitutor.composeWith(appliedAfter: NewTypeSubstitutor) = createCompositeSubstitutor(this, appliedAfter)
fun createCompositeSubstitutor(appliedFirst: TypeSubstitutor, appliedLast: NewTypeSubstitutor): NewTypeSubstitutor {
    if (appliedFirst.isEmpty) return appliedLast

    return object : NewTypeSubstitutor {
        override fun substituteArgumentProjection(argument: TypeProjection): TypeProjection? {
            val substitutedProjection = appliedFirst.substitute(argument)

            if (substitutedProjection == null || substitutedProjection === argument) {
                return null
            }

            val resultingType = appliedLast.safeSubstitute(substitutedProjection.type.unwrap())
            return TypeProjectionImpl(substitutedProjection.projectionKind, resultingType)
        }

        override fun substituteNotNullTypeWithConstructor(constructor: TypeConstructor): UnwrappedType? {
            val substitutedOnce = constructor.declarationDescriptor?.defaultType?.let {
                appliedFirst.substitute(it)
            }

            return if (substitutedOnce == null) {
                appliedLast.substituteNotNullTypeWithConstructor(constructor)
            } else {
                appliedLast.safeSubstitute(substitutedOnce)
            }
        }

        override val isEmpty: Boolean
            get() = appliedFirst.isEmpty && appliedLast.isEmpty
    }
}
