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

package org.cangnova.cangjie.resolve.calls

import com.intellij.lang.ASTNode
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.util.AstLoadingFilter
import jakarta.inject.Inject
import org.cangnova.cangjie.builtins.CangJieBuiltIns
import org.cangnova.cangjie.config.LanguageVersionSettings
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.diagnostics.infos.errors.*
import org.cangnova.cangjie.diagnostics.infos.warnings.UNNECESSARY_SAFE_CALL
import org.cangnova.cangjie.psi.*
import org.cangnova.cangjie.resolve.*
import org.cangnova.cangjie.resolve.binding.BindingContext
import org.cangnova.cangjie.resolve.binding.BindingContext.Companion.IS_FUNC
import org.cangnova.cangjie.resolve.binding.BindingTrace
import org.cangnova.cangjie.resolve.binding.recordDataFlowInfo
import org.cangnova.cangjie.resolve.binding.recordScope
import org.cangnova.cangjie.resolve.calls.context.*
import org.cangnova.cangjie.resolve.calls.model.DataFlowInfoForArgumentsImpl
import org.cangnova.cangjie.resolve.calls.model.ResolvedCall
import org.cangnova.cangjie.resolve.calls.model.ResolvedCallImpl
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResults
import org.cangnova.cangjie.resolve.calls.results.OverloadResolutionResultsUtil
import org.cangnova.cangjie.resolve.calls.results.ResolutionStatus
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValue
import org.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import org.cangnova.cangjie.resolve.calls.util.*
import org.cangnova.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.cangnova.cangjie.resolve.qualified.QualifiedExpressionResolver
import org.cangnova.cangjie.resolve.qualified.resolveQualifierAsReceiverInExpression
import org.cangnova.cangjie.resolve.qualified.resolveQualifierAsStandaloneExpression
import org.cangnova.cangjie.resolve.scopes.receivers.*
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ErrorUtils
import org.cangnova.cangjie.types.TypeUtils
import org.cangnova.cangjie.types.TypeUtils.NO_EXPECTED_TYPE
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner
import org.cangnova.cangjie.types.error.ErrorTypeKind
import org.cangnova.cangjie.types.expressions.CangJieTypeInfo
import org.cangnova.cangjie.types.expressions.DataFlowAnalyzer
import org.cangnova.cangjie.types.expressions.ExpressionTypingContext
import org.cangnova.cangjie.types.expressions.ExpressionTypingServices
import org.cangnova.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import org.cangnova.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import org.cangnova.cangjie.types.isError
import org.cangnova.cangjie.types.toFunctionType

class CallExpressionResolver(
    private val callResolver: CallResolver,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val argumentTypeResolver: ArgumentTypeResolver,
    private val dataFlowAnalyzer: DataFlowAnalyzer,
    private val builtIns: CangJieBuiltIns,
    private val qualifiedExpressionResolver: QualifiedExpressionResolver,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory,
    private val cangjieTypeRefiner: CangJieTypeRefiner
) {

    private lateinit var expressionTypingServices: ExpressionTypingServices

    // component dependency cycle
    @Inject
    fun setExpressionTypingServices(expressionTypingServices: ExpressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices
    }

    /**
     * Visits a call expression and its arguments.
     * Determines the result type and data flow information after the call.
     */
    private fun getCallExpressionTypeInfoWithoutFinalTypeCheck(
        callExpression: CjCallExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): CangJieTypeInfo {
        val call = CallMaker.makeCall(receiver, callOperationNode, callExpression)


        val temporaryForFunction = TemporaryTraceAndCache.create(
            context, "trace to resolveName as function call", callExpression
        )
//        函数是一级公民
        val (resolveResult, resolvedCall) = getResolvedCallForFunction(
            call,
            context.replaceTraceAndCache(temporaryForFunction),
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            initialDataFlowInfoForArguments
        )
        if (resolveResult) {
            val functionDescriptor = resolvedCall?.resultingDescriptor
            temporaryForFunction.commit()


            if (callExpression.valueArgumentList == null && callExpression.lambdaArguments.isEmpty()) {
                // there are only type arguments
                val hasValueParameters = functionDescriptor == null || functionDescriptor.valueParameters.isNotEmpty()
                context.trace.report(FUNCTION_CALL_EXPECTED.on(callExpression, callExpression, hasValueParameters))
            }

            if (functionDescriptor == null) {
                return noTypeInfo(context)
            }
            if (functionDescriptor is ConstructorDescriptor) {
                val constructedClass = functionDescriptor.constructedClass

                if (DescriptorUtils.isSealedClass(constructedClass)) {
                    context.trace.report(SEALED_CLASS_CONSTRUCTOR_CALL.on(callExpression))
                }
            }

            val type = functionDescriptor.returnType
            // Extracting jump out possible and jump point flow info from arguments, if any
            val arguments = callExpression.valueArguments
            val resultFlowInfo = resolvedCall.dataFlowInfoForArguments.resultInfo
            var jumpFlowInfo = resultFlowInfo
            var jumpOutPossible = false
            for (argument in arguments) {
                val argTypeInfo =
                    context.trace[BindingContext.EXPRESSION_TYPE_INFO, argument.getArgumentExpression()!!]
                if (argTypeInfo != null && argTypeInfo.jumpOutPossible) {
                    jumpOutPossible = true
                    jumpFlowInfo = argTypeInfo.jumpFlowInfo
                    break
                }
            }
            return createTypeInfo(type, resultFlowInfo, jumpOutPossible, jumpFlowInfo)
        }






        val calleeExpression = callExpression.calleeExpression
        if (calleeExpression is CjSimpleNameExpression && callExpression.typeArgumentList == null) {
            val temporaryForVariable = TemporaryTraceAndCache.create(
                context, "trace to resolveName as variable with 'invoke' call", callExpression
            )
            val (notNothing, type) = getVariableType(
                calleeExpression, receiver, callOperationNode,
                context.replaceTraceAndCache(temporaryForVariable)
            )
            val qualifier = temporaryForVariable.trace[BindingContext.QUALIFIER, calleeExpression]
            if (notNothing && (qualifier == null || qualifier !is PackageQualifier)) {

                // mark property call as unsuccessful to avoid exceptions
                callExpression.getResolvedCall(temporaryForVariable.trace.bindingContext).let {
                    (it as? ResolvedCallImpl)?.addStatus(ResolutionStatus.OTHER_ERROR)
                }

                temporaryForVariable.commit()
                context.trace.report(
                    FUNCTION_EXPECTED.on(
                        calleeExpression, calleeExpression,
                        type ?: ErrorUtils.createErrorType(ErrorTypeKind.ERROR_EXPECTED_TYPE)
                    )
                )
                argumentTypeResolver.analyzeArgumentsAndRecordTypes(
                    BasicCallResolutionContext.create(
                        context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                        DataFlowInfoForArgumentsImpl(initialDataFlowInfoForArguments, call)
                    ),
                    ResolveArgumentsMode.RESOLVE_FUNCTION_ARGUMENTS
                )
                return noTypeInfo(context)
            }
        }
        temporaryForFunction.commit()
        return noTypeInfo(context)
    }

    fun getCallExpressionTypeInfo(
        callExpression: CjCallExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val typeInfo = getCallExpressionTypeInfoWithoutFinalTypeCheck(
            callExpression, null, null, context, context.dataFlowInfo
        )
        if (context.contextDependency == ContextDependency.INDEPENDENT) {
            dataFlowAnalyzer.checkType(typeInfo.type, callExpression, context)
        }
        return typeInfo
    }




    fun getSimpleNameExpressionTypeInfo(
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext
    ) = getSimpleNameExpressionTypeInfo(nameExpression, receiver, callOperationNode, context, context.dataFlowInfo)


    private fun getVariableType(
        nameExpression: CjSimpleNameExpression,
        receiver: Receiver?,
        callOperationNode: ASTNode?,
        context: ExpressionTypingContext
    ): Pair<Boolean, CangJieType?> {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolveName as local variable or property", nameExpression
        )
        val call = CallMaker.makePropertyCall(receiver, callOperationNode, nameExpression)
        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
        )
        val resolutionResult = callResolver.resolveSimpleVariable(contextForVariable)


        temporaryForVariable.commit()
        return Pair(
            !resolutionResult.isNothing,
            if (resolutionResult.isSingleResult) resolutionResult.resultingDescriptor.returnType else null
        )
    }


    private fun resolveDeferredReceiverInQualifiedExpression(
        qualifier: Qualifier,
        selectorExpression: CjExpression?,
        context: ExpressionTypingContext
    ) {
        val calleeExpression = CjPsiUtil.deparenthesize(selectorExpression.getCalleeExpressionIfAny())
        val selectorDescriptor = (calleeExpression as? CjReferenceExpression)?.let {
            context.trace[BindingContext.REFERENCE_TARGET, it]
        }

        resolveQualifierAsReceiverInExpression(qualifier, selectorDescriptor, context)
    }


    private fun getResolvedCallForFunction(
        call: Call,
        context: ResolutionContext<*>,
        checkArguments: CheckArgumentTypesMode,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): Pair<Boolean, ResolvedCall<out FunctionDescriptor>?> {
        val results = callResolver.resolveFunctionCall(
            BasicCallResolutionContext.create(
                context, call, checkArguments, DataFlowInfoForArgumentsImpl(initialDataFlowInfoForArguments, call)
            )
        )

        return if (!results.isNothing) {
            if (call.callElement is CjCallableReference) {
                context.trace.record(IS_FUNC, call.callElement as CjNameReferenceExpression, true)
            }

            Pair(true, OverloadResolutionResultsUtil.getResultingCall(results, context))
        } else
            Pair(false, null)
    }

    private fun getSimpleNameExpressionEnumEntryType(
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): DeclarationDescriptor? {


        CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, emptyList())



        return null

    }


    private fun getSimpleNameExpressionTypeInfo(
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): CangJieTypeInfo {

        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolveName as variable", nameExpression
        )
        val (notNothing, type) = getVariableType(
            nameExpression, receiver, callOperationNode,
            context.replaceTraceAndCache(temporaryForVariable)
        )

        if (notNothing) {
            temporaryForVariable.commit()
            return createTypeInfo(type, initialDataFlowInfoForArguments)
        }

        val call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, emptyList())


        val temporaryForFunction = TemporaryTraceAndCache.create(
            context, "trace to resolveName as function", nameExpression
        )
        val newContext = context.replaceTraceAndCache(temporaryForFunction)
        val (resolveResult, resolvedCall) = getResolvedCallForFunction(
            call, newContext, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS, initialDataFlowInfoForArguments
        )
        if (resolveResult) {
            val functionDescriptor = resolvedCall?.resultingDescriptor
            if (functionDescriptor !is ConstructorDescriptor) {
                temporaryForFunction.commit()
                functionDescriptor == null || functionDescriptor.valueParameters.isNotEmpty()
//                context.trace.report(FUNCTION_CALL_EXPECTED.on(nameExpression, nameExpression, hasValueParameters))
                return createTypeInfo(functionDescriptor?.toFunctionType(), context)
            }
        }


        val temporaryForQualifier =
            TemporaryTraceAndCache.create(context, "trace to resolveName as qualifier", nameExpression)
        val contextForQualifier = context.replaceTraceAndCache(temporaryForQualifier)
        qualifiedExpressionResolver.resolveNameExpressionAsQualifierForDiagnostics(
            nameExpression,
            receiver,
            contextForQualifier
        )?.let {
            resolveQualifierAsStandaloneExpression(it, contextForQualifier)
            temporaryForQualifier.commit()
        } ?: temporaryForVariable.commit()
        return noTypeInfo(context)
    }

    private fun resolveSimpleName(
        context: ExpressionTypingContext, expression: CjSimpleNameExpression, traceAndCache: TemporaryTraceAndCache
    ): OverloadResolutionResults<VariableDescriptor> {
        val call = CallMaker.makePropertyCall(null, null, expression)
        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(traceAndCache), call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
        )
        return callResolver.resolveSimpleVariable(contextForVariable)
    }

    private fun CjQualifiedExpression.elementChain(context: ExpressionTypingContext) =
        qualifiedExpressionResolver.resolveQualifierInExpressionAndUnroll(this, context) { nameExpression ->
            val temporaryTraceAndCache =
                TemporaryTraceAndCache.create(context, "trace to resolveName as local variable or property", nameExpression)
            val resolutionResult = resolveSimpleName(context, nameExpression, temporaryTraceAndCache)

            if (resolutionResult.isSingleResult && resolutionResult.resultingDescriptor is EnumConstructorAccessDescriptor) {
                false
            } else when (resolutionResult.resultCode) {
                OverloadResolutionResults.Code.NAME_NOT_FOUND, OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER -> false
                else -> {
                    // 默认使用改进的类型推断系统
                    val newInferenceEnabled = true
                    val success = !newInferenceEnabled || resolutionResult.isSuccess
                    if (newInferenceEnabled && success) {
                        temporaryTraceAndCache.commit()
                    }
                    success
                }
            }
        }


    /**
     * Visits a qualified expression like x.y or x?.z controlling data flow information changes.

     * @return qualified expression type together with data flow information
     */
    fun getQualifiedExpressionTypeInfo(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val currentContext =
            context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(ContextDependency.INDEPENDENT)
        val trace = currentContext.trace

        val elementChain = expression.elementChain(currentContext)
        val firstReceiver = elementChain.first().receiver

        var receiverTypeInfo = when (val qualifier = trace[BindingContext.QUALIFIER, firstReceiver]) {
            is EnumClassQualifier -> {
                if (qualifier.call == null) {
                    currentContext.config.isDotEnumGetType = true
                    expressionTypingServices.getTypeInfo(firstReceiver, currentContext)
                    currentContext.config.isDotEnumGetType = false

                    CangJieTypeInfo(null, currentContext.dataFlowInfo)

                } else {
                    CangJieTypeInfo(null, currentContext.dataFlowInfo)
                }
            }

            null -> expressionTypingServices.getTypeInfo(firstReceiver, currentContext)
            else -> CangJieTypeInfo(null, currentContext.dataFlowInfo)
        }

        var resultTypeInfo = receiverTypeInfo

        var allUnsafe = true
        // Branch point: right before first safe call
        var branchPointDataFlowInfo = receiverTypeInfo.dataFlowInfo

        for (element in elementChain) {
            val receiverType = receiverTypeInfo.type
                ?: ErrorUtils.createErrorType(
                    ErrorTypeKind.ERROR_RECEIVER_TYPE,
                    when (val receiver = element.receiver) {
                        is CjNameReferenceExpression -> receiver.referencedName
                        else -> receiver.text
                    }
                )

            val receiver = trace[BindingContext.QUALIFIER, element.receiver]
                ?: ExpressionReceiver.create(element.receiver, receiverType, trace.bindingContext)

            val qualifiedExpression = element.qualified
            val lastStage = qualifiedExpression === expression
            // Drop NO_EXPECTED_TYPE / INDEPENDENT at last stage
            val contextForSelector = (if (lastStage) context else currentContext).replaceDataFlowInfo(
                if (receiver is ReceiverValue && TypeUtils.isOptionType(receiver.type) && !element.safe) {
                    // Call with nullable receiver: take data flow info from branch point
                    branchPointDataFlowInfo
                } else {
                    // Take data flow info from the current receiver
                    receiverTypeInfo.dataFlowInfo
                }
            )

            val selectorTypeInfo = getSafeOrUnsafeSelectorTypeInfo(receiver, element, contextForSelector)
            // if we have only dots and not ?. move branch point further
            allUnsafe = allUnsafe && !element.safe
            if (allUnsafe) {
                branchPointDataFlowInfo = selectorTypeInfo.dataFlowInfo
            }

            resultTypeInfo =
                checkSelectorTypeInfo(qualifiedExpression, selectorTypeInfo, contextForSelector).replaceDataFlowInfo(
                    branchPointDataFlowInfo
                )
            if (!lastStage) {
                recordResultTypeInfo(qualifiedExpression, resultTypeInfo, contextForSelector)
            }
            // For the next stage, if any, current stage selector is the receiver!
            receiverTypeInfo = selectorTypeInfo
        }
        return resultTypeInfo
    }


    private fun getUnsafeSelectorTypeInfo(
        receiver: Receiver,
        callOperationNode: ASTNode?,
        selectorExpression: CjExpression?,
        context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): CangJieTypeInfo = when (selectorExpression) {
        is CjCallExpression -> getCallExpressionTypeInfoWithoutFinalTypeCheck(
            selectorExpression, receiver, callOperationNode, context, initialDataFlowInfoForArguments
        )

        is CjSimpleNameExpression -> getSimpleNameExpressionTypeInfo(
            selectorExpression, receiver, callOperationNode, context, initialDataFlowInfoForArguments
        )

        is CjExpression -> {
            expressionTypingServices.getTypeInfo(selectorExpression, context)
            context.trace.report(ILLEGAL_SELECTOR.on(selectorExpression))
            noTypeInfo(context)
        }

        else /*null*/ -> noTypeInfo(context)
    }


    private fun getSafeOrUnsafeSelectorTypeInfo(
        receiver: Receiver,
        element: CallExpressionElement,
        context: ExpressionTypingContext
    ):
            CangJieTypeInfo {
        var initialDataFlowInfoForArguments = context.dataFlowInfo
        val receiverDataFlowValue =
            (receiver as? ReceiverValue)?.let { dataFlowValueFactory.createDataFlowValue(it, context) }

        val receiverCanBeNull =
            receiverDataFlowValue != null && initialDataFlowInfoForArguments.getStableNullability(receiverDataFlowValue)
                .canBeNull()
        // 默认启用：安全调用总是返回可空类型
        val shouldNullifySafeCallType = receiverCanBeNull || true

        val callOperationNode =
            AstLoadingFilter.forceAllowTreeLoading(element.qualified.containingFile, ThrowableComputable {
                element.node
            })

        if (receiverDataFlowValue != null && element.safe) {
            // Additional "receiver != null" information should be applied if we consider a safe call
            if (shouldNullifySafeCallType) {
                initialDataFlowInfoForArguments = initialDataFlowInfoForArguments.disequate(
                    receiverDataFlowValue, DataFlowValue.nullValue(builtIns), languageVersionSettings
                )
            }
            if (!receiverCanBeNull) {
                reportUnnecessarySafeCall(
                    context.trace,
                    receiver.type,
                    element.qualified,
                    callOperationNode,
                    receiver,
                    context.languageVersionSettings
                )
            }
        }

        val selector = element.selector


        var selectorTypeInfo =
            getUnsafeSelectorTypeInfo(receiver, callOperationNode, selector, context, initialDataFlowInfoForArguments)
                .run {
                    val type = type ?: return@run this
                    replaceType(cangjieTypeRefiner.refineType(type))
                }

        if (receiver is Qualifier) {
            resolveDeferredReceiverInQualifiedExpression(receiver, selector, context)
        }

        val selectorType = selectorTypeInfo.type
        if (selectorType != null) {
            if (element.safe && shouldNullifySafeCallType) {
                selectorTypeInfo = selectorTypeInfo.replaceType(TypeUtils.makeOption(selectorType))
            }
            // TODO : this is suspicious: remove this code?
            if (selector != null) {
                context.trace.recordType(selector, selectorTypeInfo.type)
            }
        }
        return selectorTypeInfo
    }



    private fun recordResultTypeInfo(
        qualified: CjQualifiedExpression,
        resultTypeInfo: CangJieTypeInfo,
        context: ExpressionTypingContext
    ) {
        val trace = context.trace
        if (trace[BindingContext.PROCESSED, qualified] != true) {
            // Store type information (to prevent problems in call completer)
            trace.record(BindingContext.PROCESSED, qualified)
            trace.record(BindingContext.EXPRESSION_TYPE_INFO, qualified, resultTypeInfo)
            // save scope before analyze and fix debugger: see CodeFragmentAnalyzer.correctContextForExpression
            trace.recordScope(context.scope, qualified)
            context.replaceDataFlowInfo(resultTypeInfo.dataFlowInfo).recordDataFlowInfo(qualified)
        }
    }

    private fun checkSelectorTypeInfo(
        qualified: CjQualifiedExpression,
        selectorTypeInfo: CangJieTypeInfo,
        context: ExpressionTypingContext
    ):
            CangJieTypeInfo {
        val value = constantExpressionEvaluator.evaluateExpression(qualified, context.trace, context.expectedType)
        return if (value != null && value.isPure) {
            dataFlowAnalyzer.createCompileTimeConstantTypeInfo(value, qualified, context)
        } else {
            if (context.contextDependency == ContextDependency.INDEPENDENT) {
                dataFlowAnalyzer.checkType(selectorTypeInfo.type, qualified, context)
            }
            selectorTypeInfo
        }
    }

    companion object {

        fun reportUnnecessarySafeCall(
            trace: BindingTrace,
            type: CangJieType,
            callElement: CjQualifiedExpression,
            callOperationNode: ASTNode,
            explicitReceiver: Receiver?,
            languageVersionSettings: LanguageVersionSettings
        ) {
            if (explicitReceiver is ExpressionReceiver && explicitReceiver.expression is CjSuperExpression) {
                trace.report(UNEXPECTED_SAFE_CALL.on(callOperationNode.psi))
            } else if (!type.isError) {
                trace.report(UNNECESSARY_SAFE_CALL.on(callOperationNode.psi, type))
                // 不再报告 SAFE_CALL_WILL_CHANGE_NULLABILITY 警告，因为默认启用了 SafeCallsAreAlwaysNullable
            }
        }

    }

}


