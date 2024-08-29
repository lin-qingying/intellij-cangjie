package com.huawei.cangjie.types.expressions

import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.ValueParameterDescriptor
import com.huawei.cangjie.psi.CjParameter
import com.huawei.cangjie.resolve.DescriptorResolver
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.calls.checkers.NewSchemeOfIntegerOperatorResolutionChecker
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.LexicalScopeImpl
import com.huawei.cangjie.resolve.scopes.LexicalScopeKind
import com.huawei.cangjie.types.util.TypeUtils

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
            LexicalScopeImpl(declaringScope, declaringScope.ownerDescriptor, false, null, listOf(), LexicalScopeKind.DEFAULT_VALUE)

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
        if (!valueParameterDescriptor.declaresDefaultValue()) return
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
