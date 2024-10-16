package com.huawei.cangjie.resolve.calls.model

import com.huawei.cangjie.resolve.calls.components.*
import com.huawei.cangjie.resolve.calls.components.ArgumentsToCandidateParameterDescriptor
import com.huawei.cangjie.resolve.calls.components.CheckArgumentsInParenthesis
import com.huawei.cangjie.resolve.calls.components.CreateFreshVariablesSubstitutor
import com.huawei.cangjie.resolve.calls.components.MapArguments
import com.huawei.cangjie.resolve.calls.components.NoArguments


enum class CangJieCallKind(vararg resolutionPart: ResolutionPart) {
    VARIABLE(
        CheckStaticCall,
        CheckVisibility,
        CheckExtensionPrivateVisibility,
        CheckSuperExpressionCallPart,
        NoTypeArguments,
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
        CheckStaticCall,
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
        CheckIncompatibleTypeVariableUpperBounds
    ),
    INVOKE(*FUNCTION.resolutionSequence.toTypedArray()),
    ENUM(*FUNCTION.resolutionSequence.toTypedArray(), ),
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
