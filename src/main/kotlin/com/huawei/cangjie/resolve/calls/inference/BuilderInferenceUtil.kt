package com.huawei.cangjie.resolve.calls.inference

import com.huawei.cangjie.builtins.getReceiverTypeFromFunctionType
import com.huawei.cangjie.builtins.isBuiltinFunctionalType
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.psi.CjLambdaExpression
import com.huawei.cangjie.psi.ValueArgument
import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver
import com.huawei.cangjie.types.expressions.ExpressionTypingServices

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
