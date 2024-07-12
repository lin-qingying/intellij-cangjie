package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.resolve.calls.components.CreateFreshVariablesSubstitutor


enum class CangJieCallKind(vararg resolutionPart: ResolutionPart) {
    VARIABLE(
//        CheckVisibility,
//        CheckSuperExpressionCallPart,
//        NoTypeArguments,
//        NoArguments,
        CreateFreshVariablesSubstitutor,
//        CollectionTypeVariableUsagesInfo,
//        CheckExplicitReceiverKindConsistency,
//        CheckReceivers,
//        PostponedVariablesInitializerResolutionPart,
//        CheckContextReceiversResolutionPart,
//        CheckIncompatibleTypeVariableUpperBounds
    ),
    FUNCTION(
//        CheckVisibility,
//        CheckInfixResolutionPart,
//        CheckOperatorResolutionPart,
//        CheckSuperExpressionCallPart,
//        MapTypeArguments,
//        MapArguments,
//        ArgumentsToCandidateParameterDescriptor,
        CreateFreshVariablesSubstitutor,
//        CollectionTypeVariableUsagesInfo,
//        CheckExplicitReceiverKindConsistency,
//        CheckReceivers,
//        CheckArgumentsInParenthesis,
//        CheckExternalArgument,
//        EagerResolveOfCallableReferences,
//        CompatibilityOfPartiallyApplicableSamConversion,
//        PostponedVariablesInitializerResolutionPart,
//        CheckContextReceiversResolutionPart,
//        CheckIncompatibleTypeVariableUpperBounds
    ),
//    INVOKE(*FUNCTION.resolutionSequence.toTypedArray()),
//    CALLABLE_REFERENCE(
//        CheckVisibility,
//        NoTypeArguments,
//        NoArguments,
//        CreateFreshVariablesSubstitutor,
//        CollectionTypeVariableUsagesInfo,
//        CheckReceivers,
//        CheckCallableReference,
//        CheckIncompatibleTypeVariableUpperBounds
//    ),
    UNSUPPORTED();

    val resolutionSequence = resolutionPart.asList()
}
