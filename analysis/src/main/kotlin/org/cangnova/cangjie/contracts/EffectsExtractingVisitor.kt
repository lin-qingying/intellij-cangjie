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

//package org.cangnova.cangjie.contracts
//
//import org.cangnova.cangjie.builtins.CangJieBuiltIns
//import org.cangnova.cangjie.config.LanguageVersionSettings
//import org.cangnova.cangjie.contracts.model.Computation
//import org.cangnova.cangjie.descriptors.BindingTrace
//import org.cangnova.cangjie.descriptors.ModuleDescriptor
//import org.cangnova.cangjie.psi.CjVisitor
//import org.cangnova.cangjie.resolveName.calls.smartcasts.DataFlowValueFactory
//
//
///**
// * Visits a given PSI-tree of call (and nested calls, if any) and extracts information
// * about effects of that call.
// */
//class EffectsExtractingVisitor(
//    private val trace: BindingTrace,
//    private val moduleDescriptor: ModuleDescriptor,
//    private val dataFlowValueFactory: DataFlowValueFactory,
//    private val languageVersionSettings: LanguageVersionSettings
//) : CjVisitor<Computation, Unit>() {
//    private val builtIns: CangJieBuiltIns get() = moduleDescriptor.builtIns
//    private val reducer: Reducer = Reducer(builtIns)
//
//    fun extractOrGetCached(element: CjElement): Computation {
//        trace[BindingContext.EXPRESSION_EFFECTS, element]?.let { return it }
//        return element.accept(this, Unit).also { trace.record(BindingContext.EXPRESSION_EFFECTS, element, it) }
//    }
//
//    override fun visitCjElement(element: CjElement, data: Unit): Computation {
//        val resolvedCall = element.getResolvedCall(trace.bindingContext) ?: return UNKNOWN_COMPUTATION
//        if (resolvedCall.isCallWithUnsupportedReceiver()) return UNKNOWN_COMPUTATION
//
//        val arguments = resolvedCall.getCallArgumentsAsComputations() ?: return UNKNOWN_COMPUTATION
//        val typeSubstitution = resolvedCall.getTypeSubstitution()
//
//        val descriptor = resolvedCall.resultingDescriptor
//        return when {
//            descriptor.isEqualsDescriptor() -> CallComputation(
//                ESBooleanType,
//                EqualsFunctor(false).invokeWithArguments(arguments, typeSubstitution, reducer)
//            )
//            descriptor is ValueDescriptor -> ESVariableWithDataFlowValue(
//                descriptor,
//                (element as CjExpression).createDataFlowValue() ?: return UNKNOWN_COMPUTATION
//            )
//            descriptor is FunctionDescriptor -> {
//                val esType = descriptor.returnType?.toESType()
//                CallComputation(
//                    esType,
//                    descriptor.getFunctor()?.invokeWithArguments(arguments, typeSubstitution, reducer) ?: emptyList()
//                )
//            }
//            else -> UNKNOWN_COMPUTATION
//        }
//    }
//
//    // We need lambdas only as arguments currently, and it is processed in 'visitElement' while parsing arguments of call.
//    // For all other cases we don't need lambdas.
//    override fun visitLambdaExpression(expression: CjLambdaExpression, data: Unit?): Computation = UNKNOWN_COMPUTATION
//
//    override fun visitParenthesizedExpression(expression: CjParenthesizedExpression, data: Unit): Computation =
//        CjPsiUtil.deparenthesize(expression)?.accept(this, data) ?: UNKNOWN_COMPUTATION
//
//    override fun visitConstantExpression(expression: CjConstantExpression, data: Unit): Computation {
//        val bindingContext = trace.bindingContext
//
//        val type: CangJieType = bindingContext.getType(expression) ?: return UNKNOWN_COMPUTATION
//
//        val compileTimeConstant: CompileTimeConstant<*> =
//            bindingContext.get(BindingContext.COMPILE_TIME_VALUE, expression) ?: return UNKNOWN_COMPUTATION
//        if (compileTimeConstant.isError || compileTimeConstant is UnsignedErrorValueTypeConstant) return UNKNOWN_COMPUTATION
//
//        val value: Any? = compileTimeConstant.getValue(type)
//
//        return when (value) {
//            is Boolean -> ESConstants.booleanValue(value)
//            null -> ESConstants.nullValue
//            else -> UNKNOWN_COMPUTATION
//        }
//    }
//
//    override fun visitIsExpression(expression: CjIsExpression, data: Unit): Computation {
//        val rightType = trace[BindingContext.TYPE, expression.typeReference]?.toESType() ?: return UNKNOWN_COMPUTATION
//        val arg = extractOrGetCached(expression.leftHandSide)
//        return CallComputation(
//            ESBooleanType,
//            IsFunctor(rightType, expression.isNegated).invokeWithArguments(listOf(arg), ESTypeSubstitution.empty(builtIns), reducer)
//        )
//    }
//
//    override fun visitSafeQualifiedExpression(expression: CjSafeQualifiedExpression, data: Unit?): Computation {
//        val computation = super.visitSafeQualifiedExpression(expression, data)
//        if (computation === UNKNOWN_COMPUTATION) return computation
//
//        // For safecall any clauses of form 'returns(null) -> ...' are incorrect, because safecall can return
//        // null bypassing function's contract, so we have to filter them out
//
//        fun ESEffect.containsReturnsNull(): Boolean =
//            isReturns { value == ESConstants.nullValue } || this is ConditionalEffect && this.simpleEffect.containsReturnsNull()
//
//        val effectsWithoutReturnsNull = computation.effects.filter { !it.containsReturnsNull() }
//        return CallComputation(computation.type, effectsWithoutReturnsNull)
//    }
//
//    override fun visitBinaryExpression(expression: CjBinaryExpression, data: Unit): Computation {
//        val left = extractOrGetCached(expression.left ?: return UNKNOWN_COMPUTATION)
//        val right = extractOrGetCached(expression.right ?: return UNKNOWN_COMPUTATION)
//
//        val args = listOf(left, right)
//
//        return when (expression.operationToken) {
//            CjTokens.EXCLEQ -> CallComputation(
//                ESBooleanType,
//                EqualsFunctor(true).invokeWithArguments(args, ESTypeSubstitution.empty(builtIns), reducer)
//            )
//            CjTokens.EQEQ -> CallComputation(
//                ESBooleanType,
//                EqualsFunctor(false).invokeWithArguments(args, ESTypeSubstitution.empty(builtIns), reducer)
//            )
//            CjTokens.ANDAND -> CallComputation(
//                ESBooleanType,
//                AndFunctor().invokeWithArguments(args, ESTypeSubstitution.empty(builtIns), reducer)
//            )
//            CjTokens.OROR -> CallComputation(
//                ESBooleanType,
//                OrFunctor().invokeWithArguments(args, ESTypeSubstitution.empty(builtIns), reducer)
//            )
//            else -> UNKNOWN_COMPUTATION
//        }
//    }
//
//    override fun visitUnaryExpression(expression: CjUnaryExpression, data: Unit): Computation {
//        val arg = extractOrGetCached(expression.baseExpression ?: return UNKNOWN_COMPUTATION)
//        return when (expression.operationToken) {
//            CjTokens.EXCL -> CallComputation(ESBooleanType, NotFunctor().invokeWithArguments(arg))
//            else -> UNKNOWN_COMPUTATION
//        }
//    }
//
//    private fun ReceiverValue.toComputation(): Computation = when (this) {
//        is ExpressionReceiver -> extractOrGetCached(expression)
//        is ExtensionReceiver -> {
//            if (languageVersionSettings.supportsFeature(LanguageFeature.ContractsOnCallsWithImplicitReceiver)) {
//                ESReceiverWithDataFlowValue(this, createDataFlowValue())
//            } else {
//                UNKNOWN_COMPUTATION
//            }
//        }
//        else -> UNKNOWN_COMPUTATION
//    }
//
//    private fun ExtensionReceiver.createDataFlowValue(): DataFlowValue {
//        return dataFlowValueFactory.createDataFlowValue(
//            receiverValue = this,
//            bindingContext = trace.bindingContext,
//            containingDeclarationOrModule = this.declarationDescriptor
//        )
//    }
//
//    private fun CjExpression.createDataFlowValue(): DataFlowValue? {
//        return dataFlowValueFactory.createDataFlowValue(
//            expression = this,
//            type = trace.getType(this) ?: return null,
//            bindingContext = trace.bindingContext,
//            containingDeclarationOrModule = moduleDescriptor
//        )
//    }
//
//    private fun FunctionDescriptor.getFunctor(): Functor? {
//        val contractDescription = getUserData(ContractProviderKey)?.getContractDescription() ?: return null
//        return contractDescription.getFunctor(moduleDescriptor)
//    }
//
//    private fun ResolvedCall<*>.isCallWithUnsupportedReceiver(): Boolean =
//        (extensionReceiver as? ExpressionReceiver)?.expression?.getResolvedCall(trace.bindingContext) == this ||
//                (dispatchReceiver as? ExpressionReceiver)?.expression?.getResolvedCall(trace.bindingContext) == this ||
//                (explicitReceiverKind == ExplicitReceiverKind.BOTH_RECEIVERS)
//
//    private fun ResolvedCall<*>.getCallArgumentsAsComputations(): List<Computation>? {
//        val arguments = mutableListOf<Computation>()
//        arguments.addIfNotNull(extensionReceiver?.toComputation())
//        arguments.addIfNotNull(dispatchReceiver?.toComputation())
//
//        val passedValueArguments = valueArgumentsByIndex ?: return null
//
//        passedValueArguments.mapTo(arguments) { it.toComputation() ?: return null }
//
//        return arguments
//    }
//
//    private fun ResolvedCall<*>.getTypeSubstitution(): ESTypeSubstitution {
//        val substitution = mutableMapOf<TypeConstructor, UnwrappedType>()
//        for ((typeParameter, typeArgument) in typeArguments) {
//            substitution[typeParameter.typeConstructor] = typeArgument.unwrap()
//        }
//        val substitutor = if (substitution.isNotEmpty()) {
//            NewTypeSubstitutorByConstructorMap(substitution)
//        } else {
//            EmptySubstitutor
//        }
//        return ESTypeSubstitution(substitutor, builtIns)
//    }
//
//    private fun ResolvedValueArgument.toComputation(): Computation? {
//        return when (this) {
//            // Assume that we don't know anything about default arguments
//            // Note that we don't want to return 'null' here, because 'null' indicates that we can't
//            // analyze whole call, which is undesired for cases like `kotlin.test.assertNotNull`
//            is DefaultValueArgument -> UNKNOWN_COMPUTATION
//
//            // We prefer to throw away calls with varags completely, just to be safe
//            // Potentially, we could return UNKNOWN_COMPUTATION here too
//            is VarargValueArgument -> null
//
//            is ExpressionValueArgument -> valueArgument?.toComputation()
//
//            // Should be exhaustive
//            else -> throw IllegalStateException("Unexpected ResolvedValueArgument $this")
//        }
//    }
//
//    private fun ValueArgument.toComputation(): Computation? {
//        return when (this) {
//            is CjLambdaArgument -> getLambdaExpression()?.let { ESLambda(it) }
//            is CjValueArgument -> getArgumentExpression()?.let {
//                if (it is CjLambdaExpression) ESLambda(it)
//                else extractOrGetCached(it)
//            }
//            else -> null
//        }
//    }
//}
