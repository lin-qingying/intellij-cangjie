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

package org.cangnova.cangjie.resolve.calls.inference.components

import org.cangnova.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import org.cangnova.cangjie.types.*
import org.cangnova.cangjie.types.AbstractTypeChecker.RUN_SLOW_ASSERTIONS
import org.cangnova.cangjie.types.checker.CapturedType
import org.cangnova.cangjie.types.checker.CapturedTypeConstructor
import org.cangnova.cangjie.types.checker.CapturedTypeConstructorImpl
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext.isMarkedOption
import org.cangnova.cangjie.types.checker.intersectTypes
import org.cangnova.cangjie.types.checker.isCaptured
import org.cangnova.cangjie.types.error.ErrorTypeKind

abstract class AbstractTypeSubstitutor : TypeSubstitutor  {

    override fun safeSubstitute(type: UnwrappedType): UnwrappedType =
        substitute(type, runCapturedChecks = true, keepAnnotation = true) ?: type

    abstract override fun substituteByConstructor(constructor: TypeConstructor): UnwrappedType?
    abstract override val isEmpty: Boolean
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
            return when (substitutedExpandedType) {
                null if substitutedAbbreviation == null -> null
                is SimpleType? if substitutedAbbreviation is SimpleType? ->
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

        if (typeConstructor is CapturedTypeConstructorImpl) {
            if (!runCapturedChecks) return null

            assert(type is CapturedType) {
                "Type is inconsistent -- somewhere we create type with typeConstructor = $typeConstructor " +
                        "and class: ${type::class.java.canonicalName}. type.toString() = $type"
            }
            val capturedType = type as CapturedType

            val innerType = capturedType.lowerType ?: capturedType.constructor.argument.type.unwrap()
            val substitutedInnerType = substitute(innerType, keepAnnotation, runCapturedChecks = false)
            val substitutedSuperTypes =
                capturedType.constructor.supertypes.map {
                    substitute(it, keepAnnotation, runCapturedChecks = false) ?: it
                }

            if (substitutedInnerType != null) {
                return if (substitutedInnerType.isCaptured()) substitutedInnerType else {
                    CapturedType(
                        capturedType.captureStatus,
                        CapturedTypeConstructorImpl(
                            argument = TypeArgumentImpl(substitutedInnerType),

                            typeParameter = typeConstructor.typeParameter
                        ).also { it.initializeSupertypes(substitutedSuperTypes) },
                        lowerType = if (capturedType.lowerType != null) substitutedInnerType else null,
                        isOption = type.isMarkedOption()
                    )
                }
            }

            if (RUN_SLOW_ASSERTIONS) {
                typeConstructor.supertypes.forEach { supertype ->
                    substitute(supertype.unwrap(), keepAnnotation, runCapturedChecks = false)?.let {
                        throwExceptionAboutInvalidCapturedSubstitution(capturedType, supertype.unwrap(), it)
                    }
                }
            }

            return null
        }

        if (typeConstructor is IntersectionTypeConstructor) {
            fun updateNullability(substituted: UnwrappedType) =
                if (type.isMarkedOption()) substituted.makeOptionAsSpecified(true) else substituted

            substituteByConstructor(typeConstructor)?.let { return updateNullability(it) }
            var thereAreChanges = false
            val newTypes = typeConstructor.supertypes.map {
                substitute(it.unwrap(), keepAnnotation, runCapturedChecks)?.apply { thereAreChanges = true }
                    ?: it.unwrap()
            }
            if (!thereAreChanges) return null
            return updateNullability(intersectTypes(newTypes))
        }

        // 简单分类器类型
        var replacement = substituteByConstructor(typeConstructor) ?: return null
        if (keepAnnotation) {
            replacement = replacement.replaceAttributes(
                replacement.attributes.add(type.attributes)
            )
        }
        if (type.isMarkedOption()) {
            replacement = replacement.makeOptionAsSpecified(true)
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
        val newArguments = arrayOfNulls<TypeArgument?>(arguments.size)

        for (index in arguments.indices) {
            val argument = arguments[index]


            val specialArgumentSubstitution = substituteArgumentProjection(argument)
            if (specialArgumentSubstitution != null) {
                newArguments[index] = specialArgumentSubstitution
                continue
            }

            val substitutedArgumentType =
                substitute(argument.type.unwrap(), keepAnnotation, runCapturedChecks) ?: continue

            newArguments[index] = TypeArgumentImpl(substitutedArgumentType)
        }

        if (newArguments.all { it == null }) return null

        val newArgumentsList = arguments.mapIndexed { index, oldArgument -> newArguments[index] ?: oldArgument }
        return type.replace(newArgumentsList)
    }

    /**
     * Returns not null when substitutor manages specific type argument substitution by itself.
     * Intended for corner cases involving interactions with legacy type substitutor,
     * please consider using substituteByConstructor instead of making manual argument substitutions.
     */
    open fun substituteArgumentProjection(argument: TypeArgument): TypeArgument? {
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

object EmptySubstitutor : AbstractTypeSubstitutor() {
    override fun substituteByConstructor(constructor: TypeConstructor): UnwrappedType? = null

    override val isEmpty: Boolean get() = true
}

class  TypeSubstitutorByConstructorMap(val map: Map<TypeConstructor, UnwrappedType>) : AbstractTypeSubstitutor() {
    override fun substituteByConstructor(constructor: TypeConstructor): UnwrappedType? = map[constructor]

    override val isEmpty: Boolean get() = map.isEmpty()
}

class FreshVariableTypeSubstitutor(val freshVariables: List<TypeVariableFromCallableDescriptor>) :
    AbstractTypeSubstitutor (){

    val freshVariablesByMap = freshVariables.associateBy { it.originalTypeParameter.typeConstructor }
    override fun substituteByConstructor(constructor: TypeConstructor): UnwrappedType? {
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
        val Empty = FreshVariableTypeSubstitutor(emptyList())
    }
}

fun DefaultTypeSubstitutor.composeWith(appliedAfter: AbstractTypeSubstitutor) = createCompositeSubstitutor(this, appliedAfter)
fun createCompositeSubstitutor(appliedFirst: DefaultTypeSubstitutor, appliedLast: AbstractTypeSubstitutor): AbstractTypeSubstitutor {
    if (appliedFirst.isEmpty) return appliedLast

    return object : AbstractTypeSubstitutor() {
        override fun substituteArgumentProjection(argument: TypeArgument): TypeArgument? {
            val substitutedArgument = appliedFirst.substitute(argument)

            if (substitutedArgument == null || substitutedArgument === argument) {
                return null
            }

            val resultingType = appliedLast.safeSubstitute(substitutedArgument.type.unwrap())
            return TypeArgumentImpl(resultingType)
        }

        override fun substituteByConstructor(constructor: TypeConstructor): UnwrappedType? {
            val substitutedOnce = constructor.declarationDescriptor?.defaultType?.let {
                appliedFirst.substitute(it)?.unwrap()
            }

            return if (substitutedOnce == null) {
                appliedLast.substituteByConstructor(constructor)
            } else {
                appliedLast.safeSubstitute(substitutedOnce)
            }
        }

        override val isEmpty: Boolean
            get() = appliedFirst.isEmpty && appliedLast.isEmpty
    }
}
