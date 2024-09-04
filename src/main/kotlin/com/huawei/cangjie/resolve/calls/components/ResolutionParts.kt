package com.huawei.cangjie.resolve.calls.components

import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.descriptors.impl.TypeAliasConstructorDescriptor
import com.huawei.cangjie.resolve.calls.components.candidate.ResolutionCandidate
import com.huawei.cangjie.resolve.calls.inference.ConstraintSystemOperation
import com.huawei.cangjie.resolve.calls.inference.components.*
import com.huawei.cangjie.resolve.calls.inference.model.DeclaredUpperBoundConstraintPositionImpl
import com.huawei.cangjie.resolve.calls.inference.model.TypeVariableFromCallableDescriptor
import com.huawei.cangjie.resolve.calls.inference.substitute
import com.huawei.cangjie.resolve.calls.model.CangJieCall
import com.huawei.cangjie.resolve.calls.model.CangJieCallArgument
import com.huawei.cangjie.resolve.calls.model.ResolutionPart
import com.huawei.cangjie.types.*
import com.huawei.cangjie.utils.compactIfPossible

internal object CreateFreshVariablesSubstitutor : ResolutionPart() {


    fun createToFreshVariableSubstitutorAndAddInitialConstraints(
        candidateDescriptor: CallableDescriptor,
        kotlinCall: CangJieCall,
        csBuilder: ConstraintSystemOperation
    ): FreshVariableNewTypeSubstitutor {
        val typeParameters = candidateDescriptor.typeParameters

        val freshTypeVariables = typeParameters.map { TypeVariableFromCallableDescriptor(it) }

        val toFreshVariables = FreshVariableNewTypeSubstitutor(freshTypeVariables)

        for (freshVariable in freshTypeVariables) {
            csBuilder.registerVariable(freshVariable)
        }

        fun TypeVariableFromCallableDescriptor.addSubtypeConstraint(
            upperBound: CangJieType,
            position: DeclaredUpperBoundConstraintPositionImpl
        ) {
            csBuilder.addSubtypeConstraint(defaultType, toFreshVariables.safeSubstitute(upperBound.unwrap()), position)
        }

        for (index in typeParameters.indices) {
            val typeParameter = typeParameters[index]
            val freshVariable = freshTypeVariables[index]
            val position = DeclaredUpperBoundConstraintPositionImpl(typeParameter, kotlinCall)

            for (upperBound in typeParameter.upperBounds) {
                freshVariable.addSubtypeConstraint(upperBound, position)
            }
        }

        if (candidateDescriptor is TypeAliasConstructorDescriptor) {
            val typeAliasDescriptor = candidateDescriptor.typeAliasDescriptor
            val originalTypes = typeAliasDescriptor.underlyingType.arguments.map { it.type }
            val originalTypeParameters = candidateDescriptor.underlyingConstructorDescriptor.typeParameters
            for (index in typeParameters.indices) {
                val typeParameter = typeParameters[index]
                val freshVariable = freshTypeVariables[index]
                val typeMapping = originalTypes.mapIndexedNotNull { i: Int, kotlinType: CangJieType ->
                    if (kotlinType == typeParameter.defaultType) i else null
                }
                for (originalIndex in typeMapping) {
                    // there can be null in case we already captured type parameter in outer class (in case of inner classes)
                    // see test innerClassTypeAliasConstructor.kt
                    val originalTypeParameter = originalTypeParameters.getOrNull(originalIndex) ?: continue
                    val position = DeclaredUpperBoundConstraintPositionImpl(originalTypeParameter, kotlinCall)
                    for (upperBound in originalTypeParameter.upperBounds) {
                        freshVariable.addSubtypeConstraint(upperBound, position)
                    }
                }
            }
        }
        return toFreshVariables
    }
    private fun createKnownParametersFromFreshVariablesSubstitutor(
        freshVariableSubstitutor: FreshVariableNewTypeSubstitutor,
        knownTypeParametersSubstitutor: TypeSubstitutor,
    ): NewTypeSubstitutor {
        if (knownTypeParametersSubstitutor.isEmpty)
            return EmptySubstitutor

        val knownTypeParameterByTypeVariable = mutableMapOf<TypeConstructor, UnwrappedType>().let { map ->
            for (typeVariable in freshVariableSubstitutor.freshVariables) {
                val typeParameterType = typeVariable.originalTypeParameter.defaultType
                val substitutedKnownTypeParameter = knownTypeParametersSubstitutor.substitute(typeParameterType)

                if (substitutedKnownTypeParameter !== typeParameterType)
                    map[typeVariable.defaultType.constructor] = substitutedKnownTypeParameter
            }
            map
        }

        return knownTypeParametersSubstitutor.composeWith(
            NewTypeSubstitutorByConstructorMap(
                knownTypeParameterByTypeVariable
            )
        )
    }

    override fun ResolutionCandidate.process(workIndex: Int) {
        val csBuilder = getSystem().getBuilder()
        val toFreshVariables =
//            if (candidateDescriptor.typeParameters.isEmpty())
            FreshVariableNewTypeSubstitutor.Empty
//            else
//                createToFreshVariableSubstitutorAndAddInitialConstraints(candidateDescriptor, resolvedCall.atom, csBuilder)

        val knownTypeParametersSubstitutor = knownTypeParametersResultingSubstitutor?.let {
            createKnownParametersFromFreshVariablesSubstitutor(toFreshVariables, it)
        } ?: EmptySubstitutor

        resolvedCall.freshVariablesSubstitutor = toFreshVariables
        resolvedCall.knownParametersSubstitutor = knownTypeParametersSubstitutor

        if (candidateDescriptor.typeParameters.isEmpty()) {
            return
        }

        // bad function -- error on declaration side
        if (csBuilder.hasContradiction) return

        // optimization
//        if (resolvedCall.typeArgumentMappingByOriginal == NoExplicitArguments && knownTypeParametersResultingSubstitutor == null) {
//            return
//        }

//        val typeParameters = candidateDescriptor.original.typeParameters
//        for (index in typeParameters.indices) {
//            val typeParameter = typeParameters[index]
//            val freshVariable = toFreshVariables.freshVariables[index]
//
//            val knownTypeArgument = knownTypeParametersResultingSubstitutor?.substitute(typeParameter.defaultType)
//            if (knownTypeArgument != null) {
//                csBuilder.addEqualityConstraint(
//                    freshVariable.defaultType,
//                    getTypePreservingFlexibilityWrtTypeVariable(knownTypeArgument.unwrap(), freshVariable),
//                    KnownTypeParameterConstraintPositionImpl(knownTypeArgument)
//                )
//                continue
//            }
//
//            val typeArgument = resolvedCall.typeArgumentMappingByOriginal.getTypeArgument(typeParameter)
//
//            if (typeArgument is SimpleTypeArgument) {
//                csBuilder.addEqualityConstraint(
//                    freshVariable.defaultType,
//                    getTypePreservingFlexibilityWrtTypeVariable(typeArgument.type, freshVariable),
//                    ExplicitTypeParameterConstraintPositionImpl(typeArgument)
//                )
//            } else {
//                assert(typeArgument == TypeArgumentPlaceholder) {
//                    "Unexpected typeArgument: $typeArgument, ${typeArgument.javaClass.canonicalName}"
//                }
//            }
//        }
    }

//    fun TypeParameterDescriptor.shouldBeFlexible(flexibleCheck: (CangJieType) -> Boolean = { it.isFlexible() }): Boolean {
//        return upperBounds.any {
//            flexibleCheck(it) || ((it.constructor.declarationDescriptor as? TypeParameterDescriptor)?.run { shouldBeFlexible() } ?: false)
//        }
//    }
//
//    private fun getTypePreservingFlexibilityWrtTypeVariable(
//        type: CangJieType,
//        typeVariable: TypeVariableFromCallableDescriptor
//    ): CangJieType {
//        fun createFlexibleType() =
//            CangJieTypeFactory.flexibleType(type.makeNotNullable().lowerIfFlexible(), type.makeOptional().upperIfFlexible())
//
//        return when {
//            typeVariable.originalTypeParameter.shouldBeFlexible { it is FlexibleTypeWithEnhancement } ->
//                createFlexibleType().wrapEnhancement(type)
//            typeVariable.originalTypeParameter.shouldBeFlexible() -> createFlexibleType()
//            else -> type
//        }
//    }
//
//    private fun createKnownParametersFromFreshVariablesSubstitutor(
//        freshVariableSubstitutor: FreshVariableNewTypeSubstitutor,
//        knownTypeParametersSubstitutor: TypeSubstitutor,
//    ): NewTypeSubstitutor {
//        if (knownTypeParametersSubstitutor.isEmpty)
//            return EmptySubstitutor
//
//        val knownTypeParameterByTypeVariable = mutableMapOf<TypeConstructor, UnwrappedType>().let { map ->
//            for (typeVariable in freshVariableSubstitutor.freshVariables) {
//                val typeParameterType = typeVariable.originalTypeParameter.defaultType
//                val substitutedKnownTypeParameter = knownTypeParametersSubstitutor.substitute(typeParameterType)
//
//                if (substitutedKnownTypeParameter !== typeParameterType)
//                    map[typeVariable.defaultType.constructor] = substitutedKnownTypeParameter
//            }
//            map
//        }
//
//        return knownTypeParametersSubstitutor.composeWith(NewTypeSubstitutorByConstructorMap(knownTypeParameterByTypeVariable))
//    }
//
//    fun createToFreshVariableSubstitutorAndAddInitialConstraints(
//        candidateDescriptor: CallableDescriptor,
//        cangjieCall: CangJieCall,
//        csBuilder: ConstraintSystemOperation
//    ): FreshVariableNewTypeSubstitutor {
//        val typeParameters = candidateDescriptor.typeParameters
//
//        val freshTypeVariables = typeParameters.map { TypeVariableFromCallableDescriptor(it) }
//
//        val toFreshVariables = FreshVariableNewTypeSubstitutor(freshTypeVariables)
//
//        for (freshVariable in freshTypeVariables) {
//            csBuilder.registerVariable(freshVariable)
//        }
//
//        fun TypeVariableFromCallableDescriptor.addSubtypeConstraint(
//            upperBound: CangJieType,
//            position: DeclaredUpperBoundConstraintPositionImpl
//        ) {
//            csBuilder.addSubtypeConstraint(defaultType, toFreshVariables.safeSubstitute(upperBound.unwrap()), position)
//        }
//
//        for (index in typeParameters.indices) {
//            val typeParameter = typeParameters[index]
//            val freshVariable = freshTypeVariables[index]
//            val position = DeclaredUpperBoundConstraintPositionImpl(typeParameter, cangjieCall)
//
//            for (upperBound in typeParameter.upperBounds) {
//                freshVariable.addSubtypeConstraint(upperBound, position)
//            }
//        }
//
//        if (candidateDescriptor is TypeAliasConstructorDescriptor) {
//            val typeAliasDescriptor = candidateDescriptor.typeAliasDescriptor
//            val originalTypes = typeAliasDescriptor.underlyingType.arguments.map { it.type }
//            val originalTypeParameters = candidateDescriptor.underlyingConstructorDescriptor.typeParameters
//            for (index in typeParameters.indices) {
//                val typeParameter = typeParameters[index]
//                val freshVariable = freshTypeVariables[index]
//                val typeMapping = originalTypes.mapIndexedNotNull { i: Int, cangjieType: CangJieType ->
//                    if (cangjieType == typeParameter.defaultType) i else null
//                }
//                for (originalIndex in typeMapping) {
//                    // there can be null in case we already captured type parameter in outer class (in case of inner classes)
//                    // see test innerClassTypeAliasConstructor.kt
//                    val originalTypeParameter = originalTypeParameters.getOrNull(originalIndex) ?: continue
//                    val position = DeclaredUpperBoundConstraintPositionImpl(originalTypeParameter, cangjieCall)
//                    for (upperBound in originalTypeParameter.upperBounds) {
//                        freshVariable.addSubtypeConstraint(upperBound, position)
//                    }
//                }
//            }
//        }
//        return toFreshVariables
//    }
}

internal object NoArguments : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        assert(cangjieCall.argumentsInParenthesis.isEmpty()) {
            "Variable call cannot has arguments: ${cangjieCall.argumentsInParenthesis}. Call: $cangjieCall"
        }
        assert(cangjieCall.externalArgument == null) {
            "Variable call cannot has external argument: ${cangjieCall.externalArgument}. Call: $cangjieCall"
        }
        resolvedCall.argumentMappingByOriginal = emptyMap()
        resolvedCall.argumentToCandidateParameter = emptyMap()
    }
}

internal object MapArguments : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val mapping = callComponents.argumentsToParametersMapper.mapArguments(cangjieCall, candidateDescriptor)
        mapping.diagnostics.forEach(this::addDiagnostic)

        resolvedCall.argumentMappingByOriginal = mapping.parameterToCallArgumentMap
    }
}

internal object ErrorDescriptorResolutionPart : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        assert(ErrorUtils.isError(candidateDescriptor)) {
            "Should be error descriptor: $candidateDescriptor"
        }
//        resolvedCall.typeArgumentMappingByOriginal = TypeArgumentsToParametersMapper.TypeArgumentsMapping.NoExplicitArguments
        resolvedCall.argumentMappingByOriginal = emptyMap()
        resolvedCall.freshVariablesSubstitutor = FreshVariableNewTypeSubstitutor.Empty
        resolvedCall.knownParametersSubstitutor = EmptySubstitutor
        resolvedCall.argumentToCandidateParameter = emptyMap()

//        (cangjieCall.explicitReceiver as? SimpleCangJieCallArgument)?.let {
//            resolveCangJieArgument(it, null, ReceiverInfo.notReceiver)
//        }
//        for (argument in cangjieCall.argumentsInParenthesis) {
//            resolveCangJieArgument(argument, null, ReceiverInfo.notReceiver)
//        }
//
//        cangjieCall.externalArgument?.let {
//            resolveCangJieArgument(it, null, ReceiverInfo.notReceiver)
//        }
    }
}

internal object ArgumentsToCandidateParameterDescriptor : ResolutionPart() {
    override fun ResolutionCandidate.process(workIndex: Int) {
        val map = hashMapOf<CangJieCallArgument, ValueParameterDescriptor>()
        for ((originalValueParameter, resolvedCallArgument) in resolvedCall.argumentMappingByOriginal) {
            val valueParameter = candidateDescriptor.valueParameters.getOrNull(originalValueParameter.index) ?: continue
            for (argument in resolvedCallArgument.arguments) {
                map[argument] = valueParameter
            }
        }
        resolvedCall.argumentToCandidateParameter = map.compactIfPossible()
    }
}
