package com.linqingying.cangjie.resolve.calls.inference

import com.linqingying.cangjie.descriptors.CallableDescriptor
import com.linqingying.cangjie.resolve.calls.components.PostponedArgumentsAnalyzerContext
import com.linqingying.cangjie.resolve.calls.inference.components.NewTypeSubstitutor
import com.linqingying.cangjie.resolve.calls.inference.model.ConstraintStorage
import com.linqingying.cangjie.resolve.calls.model.CallableReferenceCangJieCallArgument
import com.linqingying.cangjie.resolve.calls.model.CangJieCallArgument
import com.linqingying.cangjie.resolve.calls.model.LHSResult
import com.linqingying.cangjie.resolve.calls.model.SubCangJieCallArgument
import com.linqingying.cangjie.types.*
import com.linqingying.cangjie.types.model.TypeSystemInferenceExtensionContext

fun TypeSubstitutor.substitute(type: UnwrappedType): UnwrappedType = safeSubstitute(type, Variance.INVARIANT).unwrap()

fun CallableDescriptor.substituteAndApproximateTypes(
    substitutor: NewTypeSubstitutor,
    typeApproximator: TypeApproximator?,
    positionDependentApproximation: Boolean = false
): CallableDescriptor {
    if (substitutor.isEmpty) return this

    val wrappedSubstitution = object : TypeSubstitution() {
        override fun get(key: CangJieType): TypeProjection? = null

        override fun prepareTopLevelType(topLevelType: CangJieType, position: Variance) =
            substitutor.safeSubstitute(topLevelType.unwrap()).let { substitutedType ->
                typeApproximator?.approximateTo(
                    substitutedType,
                    TypeApproximatorConfiguration.FinalApproximationAfterResolutionAndInference,
                      !positionDependentApproximation
                ) ?: substitutedType
            }
    }

    return substitute(TypeSubstitutor.create(wrappedSubstitution)) ?: this
}

fun ConstraintStorage.buildResultingSubstitutor(
    context: TypeSystemInferenceExtensionContext,
    transformTypeVariablesToErrorTypes: Boolean = true
): NewTypeSubstitutor {
    return buildAbstractResultingSubstitutor(context, transformTypeVariablesToErrorTypes) as NewTypeSubstitutor
}

fun CallableDescriptor.substitute(substitutor: NewTypeSubstitutor): CallableDescriptor {
    if (substitutor.isEmpty) return this

    val wrappedSubstitution = object : TypeSubstitution() {
        override fun get(key: CangJieType): TypeProjection? = null
        override fun prepareTopLevelType(topLevelType: CangJieType, position: Variance) = substitutor.safeSubstitute(topLevelType.unwrap())
    }
    return substitute(TypeSubstitutor.create(wrappedSubstitution))
}
fun PostponedArgumentsAnalyzerContext.addSubsystemFromArgument(argument: CangJieCallArgument?): Boolean {
    return when (argument) {
        is SubCangJieCallArgument -> {
            addOtherSystem(argument.callResult.constraintSystem.getBuilder().currentStorage())
            true
        }

        is CallableReferenceCangJieCallArgument -> {
            addSubsystemFromArgument((argument.lhsResult as? LHSResult.Expression)?.lshCallArgument)
        }

        else -> false
    }
}
