package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.huawei.cangjie.types.model.*

fun ConstraintStorage.buildNotFixedVariablesToNonSubtypableTypesSubstitutor(
    context: TypeSystemInferenceExtensionContext
): TypeSubstitutorMarker {
    return context.typeSubstitutorByTypeConstructor(
        notFixedTypeVariables.mapValues { context.createStubTypeForTypeVariablesInSubtyping(it.value.typeVariable) }
    )
}

fun TypeSystemInferenceExtensionContext.isRecursiveTypeParameter(typeConstructor: TypeConstructorMarker) =
    typeConstructor.getTypeParameterClassifier()?.hasRecursiveBounds() == true
fun TypeSystemInferenceExtensionContext.hasRecursiveTypeParametersWithGivenSelfType(selfTypeConstructor: TypeConstructorMarker): Boolean =
    selfTypeConstructor.getParameters().any { it.hasRecursiveBounds(selfTypeConstructor) } ||
              selfTypeConstructor is CapturedTypeConstructorMarker && selfTypeConstructor.supertypes().any { hasRecursiveTypeParametersWithGivenSelfType(it.typeConstructor()) }


fun TypeSystemInferenceExtensionContext.extractTypeForGivenRecursiveTypeParameter(
    type: CangJieTypeMarker,
    typeParameter: TypeParameterMarker
): CangJieTypeMarker? {
    for (argument in type.getArguments()) {

        val typeConstructor = argument.getType().typeConstructor()
        if (typeConstructor is TypeVariableTypeConstructorMarker
            && typeConstructor.typeParameter == typeParameter
            && typeConstructor.typeParameter?.hasRecursiveBounds(type.typeConstructor()) == true
        ) {
            return type
        }
        extractTypeForGivenRecursiveTypeParameter(argument.getType(), typeParameter)?.let { return it }
    }

    return null
}

fun ConstraintStorage.buildCurrentSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    additionalBindings: Map<TypeConstructorMarker, CangJieTypeMarker>
): TypeSubstitutorMarker {
    return context.typeSubstitutorByTypeConstructor(fixedTypeVariables.entries.associate { it.key to it.value } + additionalBindings)
}

fun ConstraintStorage.buildAbstractResultingSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    transformTypeVariablesToErrorTypes: Boolean = true
): TypeSubstitutorMarker = with(context) {
    if (allTypeVariables.isEmpty()) return createEmptySubstitutor()

    val currentSubstitutorMap = fixedTypeVariables.entries.associate {
        it.key to it.value
    }
    val uninferredSubstitutorMap = if (transformTypeVariablesToErrorTypes) {
        notFixedTypeVariables.entries.associate { (freshTypeConstructor, typeVariable) ->
            freshTypeConstructor to context.createUninferredType(
                (typeVariable.typeVariable).freshTypeConstructor()
            )
        }
    } else {
        notFixedTypeVariables.entries.associate { (freshTypeConstructor, typeVariable) ->
            freshTypeConstructor to typeVariable.typeVariable.defaultType(this)
        }
    }
    return context.typeSubstitutorByTypeConstructor(currentSubstitutorMap + uninferredSubstitutorMap)
}
