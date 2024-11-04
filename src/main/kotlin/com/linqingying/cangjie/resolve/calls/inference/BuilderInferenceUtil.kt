package com.linqingying.cangjie.resolve.calls.inference

import com.linqingying.cangjie.builtins.getReceiverTypeFromFunctionType
import com.linqingying.cangjie.builtins.isBuiltinFunctionalType
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.psi.CjLambdaExpression
import com.linqingying.cangjie.psi.ValueArgument
import com.linqingying.cangjie.resolve.calls.ArgumentTypeResolver
import com.linqingying.cangjie.types.expressions.ExpressionTypingServices

class BuilderInferenceSupport(
    val argumentTypeResolver: ArgumentTypeResolver,
    val expressionTypingServices: ExpressionTypingServices
)
fun isBuilderInferenceCall(
    parameterDescriptor: ValueParameterDescriptor,
    argument: ValueArgument,
//    languageVersionSettings: LanguageVersionSettings
): Boolean {
//    val parameterHasOptIn =
//        if (languageVersionSettings.supportsFeature(LanguageFeature.ExperimentalBuilderInference))
//        parameterDescriptor.hasBuilderInferenceAnnotation() && parameterDescriptor.hasFunctionOrSuspendFunctionType
//    else
//        parameterDescriptor.hasSuspendFunctionType

    val pureExpression = argument.getArgumentExpression()
//    val baseExpression = if (pureExpression is CjLabeledExpression) pureExpression.baseExpression else pureExpression

//    return parameterHasOptIn &&
//            baseExpression is CjLambdaExpression &&
//            parameterDescriptor.type.let { it.isBuiltinFunctionalType && it.getReceiverTypeFromFunctionType() != null }


    return    pureExpression is CjLambdaExpression &&
            parameterDescriptor.type.let { it.isBuiltinFunctionalType && it.getReceiverTypeFromFunctionType() != null }
}
