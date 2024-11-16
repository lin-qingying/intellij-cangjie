package com.linqingying.cangjie.resolve.calls

import com.intellij.lang.ASTNode
import com.intellij.openapi.application.runReadAction
import com.intellij.openapi.util.ThrowableComputable
import com.intellij.util.AstLoadingFilter
import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.toFunctionType
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.diagnostics.Errors.*
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.linqingying.cangjie.resolve.*
import com.linqingying.cangjie.resolve.BindingContext.IS_FUNC
import com.linqingying.cangjie.resolve.calls.context.*
import com.linqingying.cangjie.resolve.calls.model.DataFlowInfoForArgumentsImpl
import com.linqingying.cangjie.resolve.calls.model.ResolvedCall
import com.linqingying.cangjie.resolve.calls.model.ResolvedCallImpl
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResults
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResultsUtil
import com.linqingying.cangjie.resolve.calls.results.ResolutionStatus
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValue
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.resolve.calls.tower.EnumClassCallableDescriptor
import com.linqingying.cangjie.resolve.calls.tower.NewResolutionOldInference
import com.linqingying.cangjie.resolve.calls.util.*
import com.linqingying.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.linqingying.cangjie.resolve.scopes.receivers.*
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.ErrorUtils
import com.linqingying.cangjie.types.TypeRefinement
import com.linqingying.cangjie.types.checker.CangJieTypeRefiner
import com.linqingying.cangjie.types.error.ErrorTypeKind
import com.linqingying.cangjie.types.expressions.DataFlowAnalyzer
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.linqingying.cangjie.types.expressions.ExpressionTypingServices
import com.linqingying.cangjie.types.expressions.typeInfoFactory.createTypeInfo
import com.linqingying.cangjie.types.expressions.typeInfoFactory.noTypeInfo
import com.linqingying.cangjie.types.isError
import com.linqingying.cangjie.types.util.TypeUtils
import com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo
import jakarta.inject.Inject

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
            context, "trace to resolve as function call", callExpression
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
                val hasValueParameters = functionDescriptor == null || functionDescriptor.valueParameters.size > 0
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
                    context.trace[BindingContext.EXPRESSION_TYPE_INFO, argument.getArgumentExpression()]
                if (argTypeInfo != null && argTypeInfo.jumpOutPossible) {
                    jumpOutPossible = true
                    jumpFlowInfo = argTypeInfo.jumpFlowInfo
                    break
                }
            }
            return createTypeInfo(type, resultFlowInfo, jumpOutPossible, jumpFlowInfo)
        }


        val temporaryForEnum = TemporaryTraceAndCache.create(
            context, "trace to resolve as enum call", callExpression
        )
        val (resolveByEnumResult, resolvedByEnumCall) = getResolvedCallForEnum(
            call,
            temporaryForEnum,
            context.replaceTraceAndCache(temporaryForEnum),
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            initialDataFlowInfoForArguments
        )


        if (resolveByEnumResult) {
            temporaryForEnum.commit()
            val enumDescriptor = resolvedByEnumCall?.resultingDescriptor ?: return noTypeInfo(context)
//            val isEnumClass = when (enumDescriptor) {
//                is EnumClassCallableDescriptor -> !enumDescriptor.isEnumEntry
//                else -> false
//            }

            val type = enumDescriptor.returnType
            val resultFlowInfo = resolvedByEnumCall.dataFlowInfoForArguments.resultInfo
//
//            if (callExpression.getStrictParentOfType<CjDotQualifiedExpression>() == null && enumDescriptor is EnumClassCallableDescriptor && DescriptorUtils.isEnum(
//                    enumDescriptor.type
//                )
//            ) {
//                context.trace.report(
//                    EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(
//                        callExpression,
//                        enumDescriptor.type as? ClassifierDescriptor
//                    )
//                )
//
//            }
//
//            if (isEnumClass) {
//                val enumClass:ClassDescriptor? = when(enumDescriptor){
//                    is EnumClassCallableDescriptor -> enumDescriptor.type as ClassDescriptor
//
//                    else -> null
//                }
//                context.trace.record(
//                    BindingContext.QUALIFIER, callExpression,
//                    enumClass?.let {
//                        EnumClassQualifier(
//                            callExpression,
//                            it,
//                            (resolvedByEnumCall as? NewAbstractResolvedCall)?.cangjieCall
//                        )
//                    }
//
//                )
//            }
            return createTypeInfo(type, resultFlowInfo)
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

    fun getSimpleNameExpressionTypeInfoByCaseEnum(
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext, argument: List<ValueArgument>,
        isReportError: Boolean = true
    ) = getSimpleNameExpressionTypeInfoByCaseEnum(
        nameExpression,
        receiver,
        callOperationNode,
        context,
        context.dataFlowInfo,
        argument, isReportError
    )

    fun getSimpleNameExpressionTypeInfoByEnum(
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext
    ) = getSimpleNameExpressionTypeInfoByEnum(
        nameExpression,
        receiver,
        callOperationNode,
        context,
        context.dataFlowInfo
    )

    fun getSimpleNameExpressionTypeInfo(
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext
    ) = getSimpleNameExpressionTypeInfo(nameExpression, receiver, callOperationNode, context, context.dataFlowInfo)

    fun getSimpleNameExpressionEnumEntryType(
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext
    ) = getSimpleNameExpressionEnumEntryType(nameExpression, receiver, callOperationNode, context, context.dataFlowInfo)

    private fun getVariableType(
        nameExpression: CjSimpleNameExpression,
        receiver: Receiver?,
        callOperationNode: ASTNode?,
        context: ExpressionTypingContext
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


        temporaryForVariable.commit()
        return Pair(
            !resolutionResult.isNothing,
            if (resolutionResult.isSingleResult) resolutionResult.resultingDescriptor.returnType else null
        )
    }

    private fun getEnumEntryDescriptor(
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext
    ): Pair<Boolean, CallableDescriptor?> {
        val temporaryForVariable = TemporaryTraceAndCache.create(
            context, "trace to resolve as local variable or property", nameExpression
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
            if (resolutionResult.isSingleResult) resolutionResult.resultingDescriptor else null
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

    private fun getResolvedCallForEnum(
        call: Call,
        tcache: TemporaryTraceAndCache,
        context: ResolutionContext<*>,
        checkArguments: CheckArgumentTypesMode,
        initialDataFlowInfoForArguments: DataFlowInfo,
        kind: NewResolutionOldInference.ResolutionKind = NewResolutionOldInference.ResolutionKind.Enum

    ): Pair<Boolean, ResolvedCall<out CallableDescriptor>?> {

        return runReadAction {
            val results = callResolver.resolveEnumCall(
                tcache,
                BasicCallResolutionContext.create(
                    context, call, checkArguments, DataFlowInfoForArgumentsImpl(initialDataFlowInfoForArguments, call)
                ),
                kind
            )
            if (!results.isNothing)
                Pair(true, OverloadResolutionResultsUtil.getResultingCall(results, context))
            else
                Pair(false, null)
        }
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


        val call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, emptyList())
        val temporaryForEnum = TemporaryTraceAndCache.create(
            context, "trace to resolve as enum", nameExpression
        )
        val newContext = context.replaceTraceAndCache(temporaryForEnum)
        val (resolveResult, resolvedCall) = getResolvedCallForEnum(
            call,
            temporaryForEnum,
            newContext,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            initialDataFlowInfoForArguments
        )
        if (resolveResult) {
//            temporaryForEnum.commit()
            return resolvedCall?.resultingDescriptor

        }


        return null

    }

    private fun getSimpleNameExpressionTypeInfoByEnum(
        nameExpression: CjSimpleNameExpression, receiver: Receiver?,
        callOperationNode: ASTNode?, context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo
    ): CangJieTypeInfo {
        val call = CallMaker.makeCall(nameExpression, receiver, callOperationNode, nameExpression, emptyList())

        val temporaryForEnum = TemporaryTraceAndCache.create(
            context, "trace to resolve as enum", nameExpression
        )
        val newEnumContext = context.replaceTraceAndCache(temporaryForEnum)

        val (resolveEnumResult, resolvedEnumCall) = getResolvedCallForEnum(
            call,
            temporaryForEnum,
            newEnumContext,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            initialDataFlowInfoForArguments
        )
        temporaryForEnum.commit()
        if (resolveEnumResult) {

            val enumDescriptor = resolvedEnumCall?.resultingDescriptor
            if (nameExpression.getStrictParentOfType<CjDotQualifiedExpression>() == null && enumDescriptor is EnumClassCallableDescriptor && DescriptorUtils.isEnum(
                    enumDescriptor.type
                )
            ) {
                context.trace.report(
                    EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(
                        nameExpression,
                        enumDescriptor.type as? ClassifierDescriptor
                    )
                )

            }
            return createTypeInfo(enumDescriptor?.returnType, context)

        }
        return noTypeInfo(context)
    }

    private fun getSimpleNameExpressionTypeInfoByCaseEnum(
        nameExpression: CjSimpleNameExpression,
        receiver: Receiver?,
        callOperationNode: ASTNode?,
        context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo,
        argument: List<ValueArgument>,
        isReportError: Boolean = true
    ): CangJieTypeInfo {
        val call = CallMaker.makeCall(
            nameExpression, receiver, callOperationNode, nameExpression,

            argument

        )

        val temporaryForEnum = TemporaryTraceAndCache.create(
            context, "trace to resolve as enum", nameExpression
        )
        val newEnumContext = context.replaceTraceAndCache(temporaryForEnum)

        val (resolveEnumResult, resolvedEnumCall) = getResolvedCallForEnum(
            call,
            temporaryForEnum,
            newEnumContext,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            initialDataFlowInfoForArguments,
            NewResolutionOldInference.ResolutionKind.CaseEnum
        )
        if (isReportError) {
            temporaryForEnum.commit()
        }


        if (resolveEnumResult) {
            temporaryForEnum.commit()

            val enumDescriptor = resolvedEnumCall?.resultingDescriptor
            if (nameExpression.getStrictParentOfType<CjDotQualifiedExpression>() == null && enumDescriptor is EnumClassCallableDescriptor && DescriptorUtils.isEnum(
                    enumDescriptor.type
                )
            ) {
                context.trace.report(
                    EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(
                        nameExpression,
                        enumDescriptor.type as? ClassifierDescriptor
                    )
                )

            }
            return createTypeInfo(enumDescriptor?.returnType, context)

        }
        return noTypeInfo(context)
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
//                context.trace.report(FUNCTION_CALL_EXPECTED.on(nameExpression, nameExpression, hasValueParameters))
                return createTypeInfo(functionDescriptor?.toFunctionType(), context)
            }
        }

        val temporaryForEnum = TemporaryTraceAndCache.create(
            context, "trace to resolve as enum", nameExpression
        )
        val newEnumContext = context.replaceTraceAndCache(temporaryForEnum)

        val (resolveEnumResult, resolvedEnumCall) = getResolvedCallForEnum(
            call,
            temporaryForEnum,
            newEnumContext,
            CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            initialDataFlowInfoForArguments
        )
        if (resolveEnumResult) {
            temporaryForEnum.commit()
            val enumDescriptor = resolvedEnumCall?.resultingDescriptor
            if (nameExpression.getStrictParentOfType<CjDotQualifiedExpression>() == null && enumDescriptor is EnumClassCallableDescriptor && DescriptorUtils.isEnum(
                    enumDescriptor.type
                )
            ) {
                context.trace.report(
                    EXPECTED_MEMBER_OR_CONSTRUCTOR_AFTER_TYPE.on(
                        nameExpression,
                        enumDescriptor.type as? ClassifierDescriptor
                    )
                )

            }
            return createTypeInfo(enumDescriptor?.returnType, context)

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
                TemporaryTraceAndCache.create(context, "trace to resolve as local variable or property", nameExpression)
            val resolutionResult = resolveSimpleName(context, nameExpression, temporaryTraceAndCache)

            if (resolutionResult.isSingleResult && resolutionResult.resultingDescriptor is FakeCallableDescriptorForObject) {
                false
            } else when (resolutionResult.resultCode) {
                OverloadResolutionResults.Code.NAME_NOT_FOUND, OverloadResolutionResults.Code.CANDIDATES_WITH_WRONG_RECEIVER -> false
                else -> {
                    val newInferenceEnabled =
                        context.languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
                    val success = !newInferenceEnabled || resolutionResult.isSuccess
                    if (newInferenceEnabled && success) {
                        temporaryTraceAndCache.commit()
                    }
                    success
                }
            }
        }

    fun getQualifiedExpressionTypeInfoByEnum(
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
                        is CjNameReferenceExpression -> receiver.getReferencedName()
                        else -> receiver.text
                    }
                )

            val receiver = trace[BindingContext.QUALIFIER, element.receiver]
                ?: ExpressionReceiver.create(element.receiver, receiverType, trace.bindingContext)

            val qualifiedExpression = element.qualified
            val lastStage = qualifiedExpression === expression
            // Drop NO_EXPECTED_TYPE / INDEPENDENT at last stage
            val contextForSelector = (if (lastStage) context else currentContext).replaceDataFlowInfo(
                if (receiver is ReceiverValue && TypeUtils.isNullableType(receiver.type) && !element.safe) {
                    // Call with nullable receiver: take data flow info from branch point
                    branchPointDataFlowInfo
                } else {
                    // Take data flow info from the current receiver
                    receiverTypeInfo.dataFlowInfo
                }
            )

            val selectorTypeInfo = getSafeOrUnsafeSelectorTypeInfoByEnum(receiver, element, contextForSelector)
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

    fun getQualifiedExpressionTypeInfoByCaseEnum(
        expression: CjQualifiedExpression,
        argument: List<ValueArgument>,
        context: ExpressionTypingContext
    ): CangJieTypeInfo {
        val currentContext =
            context.replaceExpectedType(NO_EXPECTED_TYPE).replaceContextDependency(ContextDependency.INDEPENDENT)
        val trace = currentContext.trace

        val elementChain = expression.elementChain(currentContext)
        val firstReceiver = elementChain.first().receiver

        var receiverTypeInfo = when (trace[BindingContext.QUALIFIER, firstReceiver]) {

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
                        is CjNameReferenceExpression -> receiver.getReferencedName()
                        else -> receiver.text
                    }
                )

            val receiver = trace[BindingContext.QUALIFIER, element.receiver]
                ?: ExpressionReceiver.create(element.receiver, receiverType, trace.bindingContext)

            val qualifiedExpression = element.qualified
            val lastStage = qualifiedExpression === expression
            // Drop NO_EXPECTED_TYPE / INDEPENDENT at last stage
            val contextForSelector = (if (lastStage) context else currentContext).replaceDataFlowInfo(
                if (receiver is ReceiverValue && TypeUtils.isNullableType(receiver.type) && !element.safe) {
                    // Call with nullable receiver: take data flow info from branch point
                    branchPointDataFlowInfo
                } else {
                    // Take data flow info from the current receiver
                    receiverTypeInfo.dataFlowInfo
                }
            )

            val selectorTypeInfo =
                getSafeOrUnsafeSelectorTypeInfoByCaseEnum(receiver, element, argument, contextForSelector)
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
                        is CjNameReferenceExpression -> receiver.getReferencedName()
                        else -> receiver.text
                    }
                )

            val receiver = trace[BindingContext.QUALIFIER, element.receiver]
                ?: ExpressionReceiver.create(element.receiver, receiverType, trace.bindingContext)

            val qualifiedExpression = element.qualified
            val lastStage = qualifiedExpression === expression
            // Drop NO_EXPECTED_TYPE / INDEPENDENT at last stage
            val contextForSelector = (if (lastStage) context else currentContext).replaceDataFlowInfo(
                if (receiver is ReceiverValue && TypeUtils.isNullableType(receiver.type) && !element.safe) {
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

    private fun getUnsafeSelectorTypeInfoByEnum(
        receiver: Receiver,
        callOperationNode: ASTNode?,
        selectorExpression: CjExpression?,
        context: ExpressionTypingContext,
        initialDataFlowInfoForArguments: DataFlowInfo,
        isCaseEnum: Boolean = false,
        argument: List<ValueArgument> = emptyList(),
    ): CangJieTypeInfo {
        if (isCaseEnum) {
            return when (selectorExpression) {
//        is CjCallExpression -> getCallExpressionTypeInfoWithoutFinalTypeCheck(
//            selectorExpression, receiver, callOperationNode, context, initialDataFlowInfoForArguments
//        )

                is CjSimpleNameExpression -> getSimpleNameExpressionTypeInfoByCaseEnum(
                    selectorExpression,
                    receiver,
                    callOperationNode,
                    context,
                    initialDataFlowInfoForArguments,
                    argument
                )

//        is CjExpression -> {
//            expressionTypingServices.getTypeInfo(selectorExpression, context)
//            context.trace.report(ILLEGAL_SELECTOR.on(selectorExpression))
//            noTypeInfo(context)
//        }

                else /*null*/ -> noTypeInfo(context)
            }
        }


        return when (selectorExpression) {
//        is CjCallExpression -> getCallExpressionTypeInfoWithoutFinalTypeCheck(
//            selectorExpression, receiver, callOperationNode, context, initialDataFlowInfoForArguments
//        )

            is CjSimpleNameExpression -> getSimpleNameExpressionTypeInfoByEnum(
                selectorExpression, receiver, callOperationNode, context, initialDataFlowInfoForArguments
            )

//        is CjExpression -> {
//            expressionTypingServices.getTypeInfo(selectorExpression, context)
//            context.trace.report(ILLEGAL_SELECTOR.on(selectorExpression))
//            noTypeInfo(context)
//        }

            else /*null*/ -> noTypeInfo(context)
        }
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

    private fun getUnsafeSelectorTypeInfoByEnum(
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
        val shouldNullifySafeCallType =
            receiverCanBeNull || context.languageVersionSettings.supportsFeature(LanguageFeature.SafeCallsAreAlwaysNullable)

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

        @OptIn(TypeRefinement::class)
        val selectorTypeInfo =
            getUnsafeSelectorTypeInfoByEnum(
                receiver,
                callOperationNode,
                selector,
                context,
                initialDataFlowInfoForArguments
            )


        return selectorTypeInfo
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
        val shouldNullifySafeCallType =
            receiverCanBeNull || context.languageVersionSettings.supportsFeature(LanguageFeature.SafeCallsAreAlwaysNullable)

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

        @OptIn(TypeRefinement::class)
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
                selectorTypeInfo = selectorTypeInfo.replaceType(TypeUtils.makeOptional(selectorType))
            }
            // TODO : this is suspicious: remove this code?
            if (selector != null) {
                context.trace.recordType(selector, selectorTypeInfo.type)
            }
        }
        return selectorTypeInfo
    }

    private fun getSafeOrUnsafeSelectorTypeInfoByCaseEnum(
        receiver: Receiver,
        element: CallExpressionElement,
        argument: List<ValueArgument>,
        context: ExpressionTypingContext
    ):
            CangJieTypeInfo {
        var initialDataFlowInfoForArguments = context.dataFlowInfo
        val receiverDataFlowValue =
            (receiver as? ReceiverValue)?.let { dataFlowValueFactory.createDataFlowValue(it, context) }

        val receiverCanBeNull =
            receiverDataFlowValue != null && initialDataFlowInfoForArguments.getStableNullability(receiverDataFlowValue)
                .canBeNull()
        val shouldNullifySafeCallType =
            receiverCanBeNull || context.languageVersionSettings.supportsFeature(LanguageFeature.SafeCallsAreAlwaysNullable)

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

        @OptIn(TypeRefinement::class)
        var selectorTypeInfo =
            getUnsafeSelectorTypeInfoByEnum(
                receiver,
                callOperationNode,
                selector,
                context,
                initialDataFlowInfoForArguments,
                isCaseEnum = true,
                argument
            )
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
                selectorTypeInfo = selectorTypeInfo.replaceType(TypeUtils.makeOptional(selectorType))
            }
            // TODO : this is suspicious: remove this code?
            if (selector != null) {
                context.trace.recordType(selector, selectorTypeInfo.type)
            }
        }
        return selectorTypeInfo
    }

    private fun getSafeOrUnsafeSelectorTypeInfoByEnum(
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
        val shouldNullifySafeCallType =
            receiverCanBeNull || context.languageVersionSettings.supportsFeature(LanguageFeature.SafeCallsAreAlwaysNullable)

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

        @OptIn(TypeRefinement::class)
        var selectorTypeInfo =
            getUnsafeSelectorTypeInfoByEnum(
                receiver,
                callOperationNode,
                selector,
                context,
                initialDataFlowInfoForArguments
            )
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
                selectorTypeInfo = selectorTypeInfo.replaceType(TypeUtils.makeOptional(selectorType))
            }
            // TODO : this is suspicious: remove this code?
            if (selector != null) {
                context.trace.recordType(selector, selectorTypeInfo.type)
            }
        }
        return selectorTypeInfo
    }

    private fun checkNestedClassAccess(
        expression: CjQualifiedExpression,
        context: ExpressionTypingContext
    ) {
        val selectorExpression = expression.selectorExpression ?: return

        // A.B - if B is a nested class accessed by outer class, 'A' and 'A.B' were marked as qualifiers
        // a.B - if B is a nested class accessed by instance reference, 'a.B' was marked as a qualifier, but 'a' was not (it's an expression)

        val expressionQualifier = context.trace[BindingContext.QUALIFIER, expression]
        val receiverQualifier = context.trace[BindingContext.QUALIFIER, expression.receiverExpression]

        if (receiverQualifier == null && expressionQualifier != null) {
            assert(expressionQualifier is ClassifierQualifier) { "Only class can (package cannot) be accessed by instance reference: $expressionQualifier" }
            val descriptor = (expressionQualifier as ClassifierQualifier).descriptor
            context.trace.report(NESTED_CLASS_ACCESSED_VIA_INSTANCE_REFERENCE.on(selectorExpression, descriptor))
        }
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
        checkNestedClassAccess(qualified, context)
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
                if (!languageVersionSettings.supportsFeature(LanguageFeature.SafeCallsAreAlwaysNullable)) {
                    trace.report(SAFE_CALL_WILL_CHANGE_NULLABILITY.on(callElement))
                }
            }
        }

    }

}


