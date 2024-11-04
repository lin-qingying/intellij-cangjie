package com.linqingying.cangjie.resolve.calls.model

import com.linqingying.cangjie.resolve.calls.components.*
import com.linqingying.cangjie.resolve.calls.components.ArgumentsToCandidateParameterDescriptor
import com.linqingying.cangjie.resolve.calls.components.CheckArgumentsInParenthesis
import com.linqingying.cangjie.resolve.calls.components.CreateFreshVariablesSubstitutor
import com.linqingying.cangjie.resolve.calls.components.MapArguments
import com.linqingying.cangjie.resolve.calls.components.NoArguments


enum class CangJieCallKind(vararg resolutionPart: ResolutionPart) {
    VARIABLE(
        CheckStaticCall,
        CheckVisibility,
        CheckExtensionPrivateVisibility,
        CheckSuperExpressionCallPart,
        NoTypeArguments,
        MapTypeArguments,
        NoArguments,
        CreateFreshVariablesSubstitutor,
//        CollectionTypeVariableUsagesInfo,
//        CheckExplicitReceiverKindConsistency,
        CheckReceivers,

        PostponedVariablesInitializerResolutionPart,
//        CheckContextReceiversResolutionPart,
        CheckIncompatibleTypeVariableUpperBounds
    ),
    FUNCTION(

        CheckExtensionPrivateVisibility,
        CheckOperatorCallPart,
        CheckVisibility,
//        CheckInfixResolutionPart,
//        CheckOperatorResolutionPart,
        CheckSuperExpressionCallPart,
        MapTypeArguments,
        MapArguments,
        ArgumentsToCandidateParameterDescriptor,
        CreateFreshVariablesSubstitutor,
//        CollectionTypeVariableUsagesInfo,
//        CheckExplicitReceiverKindConsistency,
        CheckReceivers,
        CheckArgumentsInParenthesis,
        CheckExternalArgument,
//        EagerResolveOfCallableReferences,
//        CompatibilityOfPartiallyApplicableSamConversion,
        PostponedVariablesInitializerResolutionPart,
//        CheckContextReceiversResolutionPart,
        CheckIncompatibleTypeVariableUpperBounds,
        CheckStaticCall,
    ),
    INVOKE(*FUNCTION.resolutionSequence.toTypedArray()),
    ENUM(/**FUNCTION.resolutionSequence.toTypedArray(),CheckEnumCall */
        MapTypeArguments,
        MapArguments,
        ArgumentsToCandidateParameterDescriptor,
        CreateFreshVariablesSubstitutor,



    ),
    CALLABLE_REFERENCE(
        CheckVisibility,
        NoTypeArguments,
        NoArguments,
        CreateFreshVariablesSubstitutor,
//        CollectionTypeVariableUsagesInfo,
        CheckReceivers,
//        CheckCallableReference,
        CheckIncompatibleTypeVariableUpperBounds
    ),
    UNSUPPORTED();

    val resolutionSequence = resolutionPart.asList()
}
