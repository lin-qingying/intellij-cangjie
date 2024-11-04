package com.linqingying.cangjie.types.expressions

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.BindingTrace
import com.linqingying.cangjie.descriptors.ValueParameterDescriptor
import com.linqingying.cangjie.psi.CjParameter
import com.linqingying.cangjie.resolve.DescriptorResolver
import com.linqingying.cangjie.resolve.DescriptorUtils
import com.linqingying.cangjie.resolve.calls.checkers.NewSchemeOfIntegerOperatorResolutionChecker
import com.linqingying.cangjie.resolve.calls.components.InferenceSession
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.linqingying.cangjie.resolve.lazy.ForceResolveUtil
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.LexicalScopeImpl
import com.linqingying.cangjie.resolve.scopes.LexicalScopeKind
import com.linqingying.cangjie.types.util.TypeUtils

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
