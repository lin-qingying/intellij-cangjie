package com.huawei.cangjie.resolve.calls

import com.huawei.cangjie.builtins.CangJieBuiltIns
import com.huawei.cangjie.config.LanguageVersionSettings
import com.huawei.cangjie.descriptors.ConstructorDescriptor
import com.huawei.cangjie.descriptors.Errors.FUNCTION_CALL_EXPECTED
import com.huawei.cangjie.descriptors.Errors.FUNCTION_EXPECTED
import com.huawei.cangjie.descriptors.FunctionDescriptor
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.BindingContext
import com.huawei.cangjie.resolve.DescriptorUtils
import com.huawei.cangjie.resolve.QualifiedExpressionResolver
import com.huawei.cangjie.resolve.calls.context.*
import com.huawei.cangjie.resolve.calls.model.DataFlowInfoForArgumentsImpl
import com.huawei.cangjie.resolve.calls.model.ResolvedCall
import com.huawei.cangjie.resolve.calls.model.ResolvedCallImpl
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResultsUtil
import com.huawei.cangjie.resolve.calls.results.ResolutionStatus
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.util.CallMaker
import com.huawei.cangjie.resolve.calls.util.ResolveArgumentsMode
import com.huawei.cangjie.resolve.calls.util.getCalleeExpressionIfAny
import com.huawei.cangjie.resolve.calls.util.getResolvedCall
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.huawei.cangjie.resolve.resolveQualifierAsStandaloneExpression
import com.huawei.cangjie.resolve.scopes.receivers.PackageQualifier
import com.huawei.cangjie.resolve.scopes.receivers.Qualifier
import com.huawei.cangjie.resolve.scopes.receivers.Receiver
import com.huawei.cangjie.types.CangJieType
import com.huawei.cangjie.types.ErrorUtils
import com.huawei.cangjie.types.checker.CangJieTypeRefiner
import com.huawei.cangjie.types.error.ErrorTypeKind
import com.huawei.cangjie.types.expressions.DataFlowAnalyzer
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.huawei.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.huawei.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo
import com.intellij.lang.ASTNode

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
            context, "trace to resolve as function call", callExpression
        )
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
                val hasValueParameters = functionDescriptor == null || functionDescriptor.valueParameters.size > 0
                context.trace.report(FUNCTION_CALL_EXPECTED.on(callExpression, callExpression, hasValueParameters))
            }
            if (functionDescriptor == null) {
                return noTypeInfo(context)
            }
//            if (functionDescriptor is ConstructorDescriptor) {
//                val constructedClass = functionDescriptor.constructedClass
//                if (DescriptorUtils.isAnnotationClass(constructedClass) && !canInstantiateAnnotationClass(
//                        callExpression,
//                        context.trace
//                    )
//                ) {
//                    val supported =
//                        context.languageVersionSettings.supportsFeature(LanguageFeature.InstantiationOfAnnotationClasses)
//                    if (!supported) context.trace.report(ANNOTATION_CLASS_CONSTRUCTOR_CALL.on(callExpression))
//                }
//                if (DescriptorUtils.isEnumClass(constructedClass)) {
//                    context.trace.report(ENUM_CLASS_CONSTRUCTOR_CALL.on(callExpression))
//                }
//                if (DescriptorUtils.isSealedClass(constructedClass)) {
//                    context.trace.report(SEALED_CLASS_CONSTRUCTOR_CALL.on(callExpression))
//                }
//            }

            val type = functionDescriptor.returnType
            // Extracting jump out possible and jump point flow info from arguments, if any
            val arguments = callExpression.valueArguments
            val resultFlowInfo = resolvedCall.dataFlowInfoForArguments.resultInfo
            var jumpFlowInfo = resultFlowInfo
            var jumpOutPossible = false
            for (argument in arguments) {
                val argTypeInfo =
                    context.trace.get(BindingContext.EXPRESSION_TYPE_INFO, argument.getArgumentExpression())
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
                context, "trace to resolve as variable with 'invoke' call", callExpression
            )
            val (notNothing, type) = getVariableType(
                calleeExpression, receiver, callOperationNode,
                context.replaceTraceAndCache(temporaryForVariable)
            )
            val qualifier = temporaryForVariable.trace.get(BindingContext.QUALIFIER, calleeExpression)
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
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext
    ): Pair<Boolean, CangJieType?> {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve as local variable or property", nameExpression
        )
        val call = CallMaker.makePropertyCall(receiver, callOperationNode, nameExpression)
        val contextForVariable = BasicCallResolutionContext.create(
            context.replaceTraceAndCache(temporaryForVariable),
            call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS
        )
        val resolutionResult = callResolver.resolveSimpleVariable(contextForVariable)

        // if the expression is a receiver in a qualified expression, it should be resolved after the selector is resolved
//        val isLHSOfDot = CjPsiUtil.isLHSOfDot(nameExpression)
//        if (!resolutionResult.isNothing && resolutionResult.resultCode != CANDIDATES_WITH_WRONG_RECEIVER) {
//            val isQualifier = isLHSOfDot &&
//                    resolutionResult.isSingleResult &&
//                    resolutionResult.resultingDescriptor is FakeCallableDescriptorForObject
//            if (!isQualifier) {
//                temporaryForVariable.commit()
//                return Pair(true, if (resolutionResult.isSingleResult) resolutionResult.resultingDescriptor.returnType else null)
//            }
//        }

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
            context.trace.get(BindingContext.REFERENCE_TARGET, it)
        }

//        resolveQualifierAsReceiverInExpression(qualifier, selectorDescriptor, context)
    }

    private fun getResolvedCallForFunction(
        call: Call,
        context: ResolutionContext<*>,
        checkArguments: CheckArgumentTypesMode,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): Pair<Boolean, ResolvedCall<FunctionDescriptor>?> {
        val results = callResolver.resolveFunctionCall(
            BasicCallResolutionContext.create(
                context, call, checkArguments, DataFlowInfoForArgumentsImpl(initialDataFlowInfoForArguments, call)
            )
        )
        return if (!results.isNothing)
            Pair(true, OverloadResolutionResultsUtil.getResultingCall(results, context))
        else
            Pair(false, null)
    }

    private fun getSimpleNameExpressionTypeInfo(
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): CangJieTypeInfo {

        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve as variable", nameExpression
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
            context, "trace to resolve as function", nameExpression
        )
        val newContext = context.replaceTraceAndCache(temporaryForFunction)
        val (resolveResult, resolvedCall) = getResolvedCallForFunction(
            call, newContext, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS, initialDataFlowInfoForArguments
        )
        if (resolveResult) {
            val functionDescriptor = resolvedCall?.resultingDescriptor
            if (functionDescriptor !is ConstructorDescriptor) {
                temporaryForFunction.commit()
                val hasValueParameters = functionDescriptor == null || functionDescriptor.valueParameters.size > 0
                context.trace.report(FUNCTION_CALL_EXPECTED.on(nameExpression, nameExpression, hasValueParameters))
                return createTypeInfo(functionDescriptor?.returnType, context)
            }
        }

        val temporaryForQualifier =
            TemporaryTraceAndCache.create(context, "trace to resolve as qualifier", nameExpression)
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
}


