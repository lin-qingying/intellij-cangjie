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

package cn.cangnova.cangjie.resolve.calls.smartcasts

import cn.cangnova.cangjie.builtins.CangJieBuiltIns
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.PropertyDescriptor
import cn.cangnova.cangjie.descriptors.VariableDescriptor
import cn.cangnova.cangjie.lexer.CjTokens
import cn.cangnova.cangjie.psi.*
import cn.cangnova.cangjie.resolve.BindingContext
import cn.cangnova.cangjie.resolve.descriptorUtil.builtIns
import cn.cangnova.cangjie.resolve.calls.context.ResolutionContext
import cn.cangnova.cangjie.resolve.scopes.receivers.ExpressionReceiver
import cn.cangnova.cangjie.resolve.scopes.receivers.ImplicitReceiver
import cn.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import cn.cangnova.cangjie.resolve.scopes.receivers.TransientReceiver
import cn.cangnova.cangjie.types.CangJieType
import cn.cangnova.cangjie.types.expressions.ExpressionTypingUtils
import cn.cangnova.cangjie.types.isError


// Please, avoid using this implementation explicitly. If you need DataFlowValueFactory, use injection.
class DataFlowValueFactoryImpl : DataFlowValueFactory {
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

        is CjBinaryExpression -> expression.operationToken === CjTokens.COALESCING

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
