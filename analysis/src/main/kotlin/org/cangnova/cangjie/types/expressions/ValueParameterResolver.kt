/*
 * Copyright 2025 LinQingYing. and contributors.
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

package org.cangnova.cangjie.types.expressions

import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.ValueParameterDescriptor
import org.cangnova.cangjie.psi.CjParameter
import org.cangnova.cangjie.resolve.DescriptorResolver
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.calls.checkers.NewSchemeOfIntegerOperatorResolutionChecker
import org.cangnova.cangjie.resolve.calls.components.InferenceSession
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.cangnova.cangjie.resolve.lazy.ForceResolveUtil
import org.cangnova.cangjie.resolve.scopes.LexicalScope
import org.cangnova.cangjie.resolve.scopes.LexicalScopeImpl
import org.cangnova.cangjie.resolve.scopes.LexicalScopeKind
import org.cangnova.cangjie.types.TypeUtils

class ValueParameterResolver(
    private val expressionTypingServices: ExpressionTypingServices,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory
) {
    fun resolveValueParameters(
        valueParameters: List<CjParameter>,
        valueParameterDescriptors: List<ValueParameterDescriptor>,
        declaringScope: LexicalScope,
        dataFlowInfo: DataFlowInfo,
        trace: BindingTrace,
        inferenceSession: InferenceSession?
    ) {
        val scopeForDefaultValue =
            LexicalScopeImpl(
                declaringScope,
                declaringScope.ownerDescriptor,
                false,
                null,
                listOf(),
                LexicalScopeKind.DEFAULT_VALUE
            )

        val contextForDefaultValue = ExpressionTypingContext.newContext(
            trace, scopeForDefaultValue, dataFlowInfo, TypeUtils.NO_EXPECTED_TYPE,
            languageVersionSettings, dataFlowValueFactory, inferenceSession
        )

        for ((descriptor, parameter) in valueParameterDescriptors.zip(valueParameters)) {
            ForceResolveUtil.forceResolveAllContents(descriptor.annotations)
            resolveDefaultValue(descriptor, parameter, contextForDefaultValue)
        }
    }

    private fun resolveDefaultValue(
        valueParameterDescriptor: ValueParameterDescriptor,
        parameter: CjParameter,
        context: ExpressionTypingContext
    ) {
        if (!valueParameterDescriptor.declaresDefaultValue) return
        val defaultValue = parameter.defaultValue ?: return
        val type = valueParameterDescriptor.type
        expressionTypingServices.getTypeInfo(defaultValue, context.replaceExpectedType(type))
        NewSchemeOfIntegerOperatorResolutionChecker.checkArgument(
            type,
            defaultValue,
            context.trace,
            constantExpressionEvaluator.module
        )
//        if (DescriptorUtils.isAnnotationClass(DescriptorResolver.getContainingClass(context.scope))) {
//            val constant = constantExpressionEvaluator.evaluateExpression(defaultValue, context.trace, type)
//            if ((constant == null || constant.usesNonConstValAsConstant) && !type.isError) {
//                context.trace.report(Errors.ANNOTATION_PARAMETER_DEFAULT_VALUE_MUST_BE_CONSTANT.on(defaultValue))
//            }
//        }
    }
}
