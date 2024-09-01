package com.huawei.cangjie.resolve.calls.smartcasts

import com.huawei.cangjie.CjNodeTypes
import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.descriptors.PropertyDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptor
import com.huawei.cangjie.descriptors.VariableDescriptorBase
import com.huawei.cangjie.lexer.CjTokens
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.descriptorUtil.builtIns
import com.huawei.cangjie.resolve.calls.context.ResolutionContext
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.huawei.cangjie.resolve.scopes.receivers.ImplicitReceiver
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue
import com.huawei.cangjie.resolve.scopes.receivers.TransientReceiver
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.expressions.ExpressionTypingUtils
import com.huawei.cangjie.types.isError


// Please, avoid using this implementation explicitly. If you need DataFlowValueFactory, use injection.
class DataFlowValueFactoryImpl (/*private val languageVersionSettings: LanguageVersionSettings*/): DataFlowValueFactory {
    override fun createDataFlowValue(
        expression: CjExpression,
        type: CangJieType,
        resolutionContext: ResolutionContext<*>
    ): DataFlowValue = createDataFlowValue(expression, type, resolutionContext.trace.bindingContext, resolutionContext.scope.ownerDescriptor)


    override fun createDataFlowValue(
        expression: CjExpression,
        type: CangJieType,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor
    ): DataFlowValue {
        return when {

            type.isError -> DataFlowValue.ERROR

            CangJieBuiltIns.isNothing(type) ->
                DataFlowValue.nullValue(containingDeclarationOrModule.builtIns) // 'null' is the only inhabitant of 'Nothing?'

            // In most cases type of `E!!`-expression is strictly not nullable and we could get proper Nullability
            // by calling `getImmanentNullability` (as it happens below).
            //
            // But there are some problem with types built on type parameters, e.g.
            // fun <T : Any?> foo(x: T) = x!!.hashCode() // there no way in type system to denote that `x!!` is not nullable
            ExpressionTypingUtils.isExclExclExpression(CjPsiUtil.deparenthesize(expression)) ->
                DataFlowValue(IdentifierInfo.Expression(expression), type, Nullability.NOT_NULL)

            isComplexExpression(expression) ->
                DataFlowValue(IdentifierInfo.Expression(expression, stableComplex = true), type)

            else -> {
                val result = getIdForStableIdentifier(expression, bindingContext, containingDeclarationOrModule/*, languageVersionSettings*/)
                DataFlowValue(if (result === IdentifierInfo.NO) IdentifierInfo.Expression(expression) else result, type)
            }
        }
    }
    private fun isComplexExpression(expression: CjExpression): Boolean = when (expression) {
        is CjBlockExpression, is CjIfExpression, is CjMatchExpression -> true

        is CjBinaryExpression -> expression.operationToken === CjTokens.ELVIS

        is CjParenthesizedExpression -> {
            val deparenthesized = CjPsiUtil.deparenthesize(expression)
            deparenthesized != null && isComplexExpression(deparenthesized)
        }

        else -> false
    }
    override fun createDataFlowValue(
        receiverValue: ReceiverValue,
        resolutionContext: ResolutionContext<*>
    ): DataFlowValue = createDataFlowValue(receiverValue, resolutionContext.trace.bindingContext, resolutionContext.scope.ownerDescriptor)

    override fun createDataFlowValue(
        receiverValue: ReceiverValue,
        bindingContext: BindingContext,
        containingDeclarationOrModule: DeclarationDescriptor
    ): DataFlowValue = when (receiverValue) {
        is TransientReceiver, is ImplicitReceiver -> createDataFlowValueForStableReceiver(receiverValue)
        is ExpressionReceiver -> createDataFlowValue(
            receiverValue.expression,
            receiverValue.getType(),
            bindingContext,
            containingDeclarationOrModule
        )
        else -> throw UnsupportedOperationException("Unsupported receiver value: " + receiverValue::class.java.name)
    }

    override fun createDataFlowValueForStableReceiver(receiver: ReceiverValue) =
        DataFlowValue(IdentifierInfo.Receiver(receiver), receiver.type)
    override fun createDataFlowValueForVariable(
        variable: CjVariable,
        variableDescriptor: VariableDescriptor,
        bindingContext: BindingContext,
        usageContainingModule: ModuleDescriptor?
    ): DataFlowValue {
        val identifierInfo = IdentifierInfo.Variable(
            variableDescriptor,
            variableDescriptor.variableKind(usageContainingModule, bindingContext, variable/*, languageVersionSettings*/),
            bindingContext[BindingContext.BOUND_INITIALIZER_VALUE, variableDescriptor]
        )
        return DataFlowValue(identifierInfo, variableDescriptor.type)
    }

    override fun createDataFlowValueForProperty(
        property: CjProperty,
        variableDescriptor: PropertyDescriptor,
        bindingContext: BindingContext,
        usageContainingModule: ModuleDescriptor?
    ): DataFlowValue {
        val identifierInfo = IdentifierInfo.Variable(
            variableDescriptor,
            variableDescriptor.variableKind(usageContainingModule, bindingContext, property/*, languageVersionSettings*/),
            bindingContext[BindingContext.BOUND_INITIALIZER_VALUE, variableDescriptor]
        )
        return DataFlowValue(identifierInfo, variableDescriptor.type)
    }
}
