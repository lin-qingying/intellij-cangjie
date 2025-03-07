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

package cn.cangnova.cangjie.resolve.calls.inference

import cn.cangnova.cangjie.builtins.getReceiverTypeFromFunctionType
import cn.cangnova.cangjie.builtins.isBuiltinFunctionalType
import cn.cangnova.cangjie.descriptors.ValueParameterDescriptor
import cn.cangnova.cangjie.psi.CjLambdaExpression
import cn.cangnova.cangjie.psi.ValueArgument
import cn.cangnova.cangjie.resolve.calls.ArgumentTypeResolver
import cn.cangnova.cangjie.types.expressions.ExpressionTypingServices

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
