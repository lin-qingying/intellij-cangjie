package com.huawei.cangjie.resolve.calls.tower

import com.huawei.cangjie.descriptors.BindingTrace
import com.huawei.cangjie.descriptors.CallableDescriptor
import com.huawei.cangjie.descriptors.Errors
import com.huawei.cangjie.descriptors.ModuleDescriptor
import com.huawei.cangjie.incremental.components.LookupLocation
import com.huawei.cangjie.name.Name
import com.huawei.cangjie.psi.*
import com.huawei.cangjie.resolve.*
import com.huawei.cangjie.resolve.BindingContext.NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER
import com.huawei.cangjie.resolve.calls.ArgumentTypeResolver
import com.huawei.cangjie.resolve.calls.CallTransformer
import com.huawei.cangjie.resolve.calls.CangJieCallResolver
import com.huawei.cangjie.resolve.calls.checkers.CallCheckerWithAdditionalResolve
import com.huawei.cangjie.resolve.calls.checkers.PassingProgressionAsCollectionCallChecker
import com.huawei.cangjie.resolve.calls.checkers.ResolutionWithStubTypesChecker
import com.huawei.cangjie.resolve.calls.components.CangJieResolutionCallbacks
import com.huawei.cangjie.resolve.calls.components.InferenceSession
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext
import com.huawei.cangjie.resolve.calls.context.ContextDependency
import com.huawei.cangjie.resolve.calls.model.*
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResults
import com.huawei.cangjie.resolve.calls.results.SingleOverloadResolutionResult
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy
import com.huawei.cangjie.resolve.calls.util.*
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator
import com.huawei.cangjie.resolve.deprecation.DeprecationResolver
import com.huawei.cangjie.resolve.descriptorUtil.isUnderscoreNamed
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil
import com.huawei.cangjie.resolve.scopes.LexicalScope
import com.huawei.cangjie.resolve.scopes.LexicalScopeKind
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope
import com.huawei.cangjie.resolve.scopes.SyntheticScopes
import com.huawei.cangjie.resolve.scopes.receivers.*
import com.huawei.cangjie.resolve.source.getPsi
import com.huawei.cangjie.types.TypeUtils
import com.huawei.cangjie.types.UnwrappedType
import com.huawei.cangjie.types.expressions.DoubleColonExpressionResolver
import com.huawei.cangjie.types.expressions.ExpressionTypingContext
import com.huawei.cangjie.types.expressions.ExpressionTypingServices
import com.huawei.cangjie.types.isError
import com.huawei.cangjie.utils.CangJieExceptionWithAttachments

class PSICallResolver(
    private val typeResolver: TypeResolver,
    private val expressionTypingServices: ExpressionTypingServices,
    private val doubleColonExpressionResolver: DoubleColonExpressionResolver,
//    private val languageVersionSettings: LanguageVersionSettings,
//    private val dynamicCallableDescriptors: DynamicCallableDescriptors,
    private val syntheticScopes: SyntheticScopes,
    private val callComponents: CangJieCallComponents,
    private val cangjieToResolvedCallTransformer: CangJieToResolvedCallTransformer,
    private val cangjieCallResolver: CangJieCallResolver,
//    private val typeApproximator: TypeApproximator,
//    private val implicitsResolutionFilter: ImplicitsExtensionsResolutionFilter,
    private val argumentTypeResolver: ArgumentTypeResolver,
//    private val effectSystem: EffectSystem,
    private val constantExpressionEvaluator: ConstantExpressionEvaluator,
    private val dataFlowValueFactory: DataFlowValueFactory,
//    private val postponedArgumentsAnalyzer: PostponedArgumentsAnalyzer,
//    private val cangjieConstraintSystemCompleter: CangJieConstraintSystemCompleter,
    private val deprecationResolver: DeprecationResolver,
    private val moduleDescriptor: ModuleDescriptor,
//    private val candidateInterceptor: CandidateInterceptor,
    private val missingSupertypesResolver: MissingSupertypesResolver,
//    private val resultTypeResolver: ResultTypeResolver,
) {
    private val callCheckersWithAdditionalResolve = listOf<CallCheckerWithAdditionalResolve>(
        PassingProgressionAsCollectionCallChecker(cangjieCallResolver),
        ResolutionWithStubTypesChecker(cangjieCallResolver)
    )
    val defaultResolutionKinds = setOf(
        NewResolutionOldInference.ResolutionKind.Function,
//        NewResolutionOldInference.ResolutionKind.Variable,
//        NewResolutionOldInference.ResolutionKind.Invoke,
//        NewResolutionOldInference.ResolutionKind.CallableReference
    )

    fun <D : CallableDescriptor> convertToOverloadResolutionResults(
        context: BasicCallResolutionContext,
        result: CallResolutionResult,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
//        if (result is AllCandidatesResolutionResult) {
//            val resolvedCalls = result.allCandidates.map { (candidate, diagnostics) ->
//                val system = candidate.getSystem()
//                val resultingSubstitutor =
//                    system.asReadOnlyStorage().buildResultingSubstitutor(system as TypeSystemInferenceExtensionContext)
//
//                cangjieToResolvedCallTransformer.transformToResolvedCall<D>(
//                    candidate.resolvedCall, null, resultingSubstitutor, diagnostics
//                )
//            }
//
//            return AllCandidates(resolvedCalls)
//        }

        val trace = context.trace

//        handleErrorResolutionResult<D>(context, trace, result, tracingStrategy)?.let { errorResult ->
//            context.inferenceSession.addErrorCallInfo(PSIErrorCallInfo(result, errorResult))
//            return errorResult
//        }

        val resolvedCall = cangjieToResolvedCallTransformer.transformAndReport<D>(result, context, tracingStrategy)

        // NB. Be careful with moving this invocation, as effect system expects resolution results to be written in trace
        // (see EffectSystem for details)
//        resolvedCall.recordEffects(trace)

        return SingleOverloadResolutionResult(resolvedCall)
//        TODO()
    }


    private inner class ASTScopeTower(
        val context: BasicCallResolutionContext,
        cjExpression: CjExpression? = null
    ) : ImplicitScopeTower {
        override val lexicalScope: LexicalScope get() = context.scope

        override val location: LookupLocation =
            cjExpression?.createLookupLocation() ?: context.call.createLookupLocation()

    }

    private fun createResolutionCallbacks(context: BasicCallResolutionContext) =
        createResolutionCallbacks(context.trace, context.inferenceSession, context)

    fun createResolutionCallbacks(
        trace: BindingTrace,
        inferenceSession: InferenceSession,
        context: BasicCallResolutionContext
    ) =
        CangJieResolutionCallbacksImpl(
            trace,
            expressionTypingServices, /*typeApproximator,*/
            argumentTypeResolver,/* languageVersionSettings,*/
            cangjieToResolvedCallTransformer,
            dataFlowValueFactory,
            inferenceSession,
            constantExpressionEvaluator,
            typeResolver,
            this,/* postponedArgumentsAnalyzer, cangjieConstraintSystemCompleter,*/
            callComponents,
            doubleColonExpressionResolver,
            deprecationResolver,
            moduleDescriptor,
            context,
            missingSupertypesResolver,
            cangjieCallResolver,
//            resultTypeResolver
        )

    private fun resolveReceiver(
        context: BasicCallResolutionContext,
        oldReceiver: Receiver?,
        isSafeCall: Boolean,
        isForImplicitInvoke: Boolean
    ): ReceiverCangJieCallArgument? {
        return when (oldReceiver) {
            null -> null

            is QualifierReceiver -> QualifierReceiverCangJieCallArgument(oldReceiver) // todo report warning if isSafeCall

            is ReceiverValue -> {
                if (oldReceiver is ExpressionReceiver) {
                    val cjExpression =
                        CjPsiUtil.getLastElementDeparenthesized(oldReceiver.expression, context.statementFilter)

                    val bindingContext = context.trace.bindingContext
                    val call =
                        bindingContext[BindingContext.DELEGATE_EXPRESSION_TO_PROVIDE_DELEGATE_CALL, cjExpression]
                            ?: cjExpression?.getCall(bindingContext)

                    val partiallyResolvedCall =
                        call?.let { bindingContext.get(BindingContext.ONLY_RESOLVED_CALL, it)?.result }

                    if (partiallyResolvedCall != null) {
                        val receiver = ReceiverValueWithSmartCastInfo(oldReceiver, emptySet(), isStable = true)
                        return SubCangJieCallArgumentImpl(
                            CallMaker.makeExternalValueArgument(oldReceiver.expression),
                            context.dataFlowInfo, context.dataFlowInfo, receiver, partiallyResolvedCall
                        )
                    }
                }

                ReceiverExpressionCangJieCallArgument(
                    context.transformToReceiverWithSmartCastInfo(oldReceiver),
                    isSafeCall,
                    isForImplicitInvoke
                )
            }

            else -> error("Incorrect receiver: $oldReceiver")
        }
    }

    private fun resolveArgumentsInParenthesis(
        context: BasicCallResolutionContext,
        arguments: List<ValueArgument>,
        isSpecialFunction: Boolean,
        tracingStrategy: TracingStrategy,
    ): List<CangJieCallArgument> {
        val dataFlowInfoForArguments = context.dataFlowInfoForArguments
        return arguments.map { argument ->
            resolveValueArgument(
                context,
                dataFlowInfoForArguments.getInfo(argument),
                argument,
                isSpecialFunction,
                tracingStrategy
            ).also { resolvedArgument ->
                dataFlowInfoForArguments.updateInfo(argument, resolvedArgument.dataFlowInfoAfterThisArgument)
            }
        }
    }

    private fun resolveDispatchReceiverForInvoke(
        context: BasicCallResolutionContext,
        cangjieCallKind: CangJieCallKind,
        oldCall: Call
    ): ReceiverCangJieCallArgument? {
//        if (cangjieCallKind != CangJieCallKind.INVOKE) return null

        require(oldCall is CallTransformer.CallForImplicitInvoke) { "Call should be CallForImplicitInvoke, but it is: $oldCall" }

        return resolveReceiver(context, oldCall.dispatchReceiver, isSafeCall = false, isForImplicitInvoke = true)
    }

    private fun resolveTypeArguments(
        context: BasicCallResolutionContext,
        typeArguments: List<CjTypeProjection>
    ): List<TypeArgument> =
        typeArguments.map { projection ->
            if (projection.projectionKind != CjProjectionKind.NONE) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection))
            }
            ModifierCheckerCore.check(projection, context.trace, null)

            val typeReference = projection.typeReference ?: return@map TypeArgumentPlaceholder

            if (typeReference.isPlaceholder) {
                val resolvedAnnotations =
                    typeResolver.resolveTypeAnnotations(context.trace, context.scope, typeReference)
                        .apply(ForceResolveUtil::forceResolveAllContents)

                for (annotation in resolvedAnnotations) {
                    val annotationElement = annotation.source.getPsi() ?: continue
                    context.trace.report(
                        Errors.UNSUPPORTED.on(
                            annotationElement,
                            "annotations on an underscored type argument"
                        )
                    )
                }

//                if (!arePartiallySpecifiedTypeArgumentsEnabled) {
//                    context.trace.report(Errors.UNSUPPORTED.on(typeReference, "underscored type argument"))
//                }

                return@map TypeArgumentPlaceholder
            }

            SimpleTypeArgumentImpl(projection, resolveType(context, typeReference, typeResolver))
        }


    private fun toCangJieCall(
        context: BasicCallResolutionContext,
        cangjieCallKind: CangJieCallKind,
        oldCall: Call,
        name: Name,
        tracingStrategy: TracingStrategy,
        isSpecialFunction: Boolean,
        forcedExplicitReceiver: Receiver? = null,
    ): PSICangJieCallImpl {
        val resolvedExplicitReceiver = resolveReceiver(
            context,
            forcedExplicitReceiver ?: oldCall.explicitReceiver,
            oldCall.isSafeCall(),
            isForImplicitInvoke = false
        )
        val dispatchReceiverForInvoke = resolveDispatchReceiverForInvoke(context, cangjieCallKind, oldCall)

        val resolvedTypeArguments = resolveTypeArguments(context, oldCall.typeArguments)

        val lambdasOutsideParenthesis = oldCall.functionLiteralArguments.size
        val extraArgumentsNumber =
            if (oldCall.callType == Call.CallType.ARRAY_SET_METHOD) 1 else lambdasOutsideParenthesis

        val allValueArguments = oldCall.valueArguments
        val argumentsInParenthesis =
            if (extraArgumentsNumber == 0) allValueArguments else allValueArguments.dropLast(extraArgumentsNumber)

        val externalLambdaArguments = oldCall.functionLiteralArguments
        val resolvedArgumentsInParenthesis =
            resolveArgumentsInParenthesis(context, argumentsInParenthesis, isSpecialFunction, tracingStrategy)

        val externalArgument = if (oldCall.callType == Call.CallType.ARRAY_SET_METHOD) {
            assert(externalLambdaArguments.isEmpty()) {
                "Unexpected lambda parameters for call $oldCall"
            }
            if (allValueArguments.isEmpty()) {
                throw CangJieExceptionWithAttachments("Can not find an external argument for 'set' method")
                    .withPsiAttachment("callElement.cj", oldCall.callElement)
                    .withPsiAttachment("file.cj", oldCall.callElement.takeIf { it.isValid }?.containingFile)
            }
            allValueArguments.last()
        } else {
            if (externalLambdaArguments.size > 1) {
                for (i in externalLambdaArguments.indices) {
                    if (i == 0) continue
                    val lambdaExpression = externalLambdaArguments[i].getLambdaExpression() ?: continue

                    if (lambdaExpression.isTrailingLambdaOnNewLIne) {
                        context.trace.report(Errors.UNEXPECTED_TRAILING_LAMBDA_ON_A_NEW_LINE.on(lambdaExpression))
                    }
                    context.trace.report(Errors.MANY_LAMBDA_EXPRESSION_ARGUMENTS.on(lambdaExpression))
                }
            }

            externalLambdaArguments.firstOrNull()
        }

        val dataFlowInfoAfterArgumentsInParenthesis =
            if (externalArgument != null && resolvedArgumentsInParenthesis.isNotEmpty())
                resolvedArgumentsInParenthesis.last().psiCallArgument.dataFlowInfoAfterThisArgument
            else
                context.dataFlowInfoForArguments.resultInfo

        val resolvedExternalArgument = externalArgument?.let {
            resolveValueArgument(
                context,
                dataFlowInfoAfterArgumentsInParenthesis,
                it,
                isSpecialFunction,
                tracingStrategy
            )
        }
        val resultDataFlowInfo =
            resolvedExternalArgument?.dataFlowInfoAfterThisArgument ?: dataFlowInfoAfterArgumentsInParenthesis

        resolvedArgumentsInParenthesis.forEach { it.setResultDataFlowInfoIfRelevant(resultDataFlowInfo) }
        resolvedExternalArgument?.setResultDataFlowInfoIfRelevant(resultDataFlowInfo)

        val isForImplicitInvoke = oldCall is CallTransformer.CallForImplicitInvoke

        return PSICangJieCallImpl(
            cangjieCallKind,
            oldCall,
            tracingStrategy,
            resolvedExplicitReceiver,
            dispatchReceiverForInvoke,
            name,
            resolvedTypeArguments,
            resolvedArgumentsInParenthesis,
            resolvedExternalArgument,
            context.dataFlowInfo,
            resultDataFlowInfo,
            context.dataFlowInfoForArguments,
            isForImplicitInvoke
        )
    }
    private fun BasicCallResolutionContext.expandContextForCatchClause(cjExpression: Any): BasicCallResolutionContext {
        if (cjExpression !is CjExpression) return this

        val variableDescriptorHolder = trace.bindingContext[NEW_INFERENCE_CATCH_EXCEPTION_PARAMETER, cjExpression] ?: return this
        val variableDescriptor = variableDescriptorHolder.get() ?: return this
        variableDescriptorHolder.set(null)

        val redeclarationChecker = expressionTypingServices.createLocalRedeclarationChecker(trace)

        val catchScope = with(scope) {
            LexicalWritableScope(this, ownerDescriptor, false, redeclarationChecker, LexicalScopeKind.CATCH)
        }
//        val isReferencingToUnderscoreNamedParameterForbidden =
//            languageVersionSettings.getFeatureSupport(LanguageFeature.ForbidReferencingToUnderscoreNamedParameterOfCatchBlock) == LanguageFeature.State.ENABLED
        if (!variableDescriptor.isUnderscoreNamed /*|| !isReferencingToUnderscoreNamedParameterForbidden*/) {
            catchScope.addVariableDescriptor(variableDescriptor)
        }
        return replaceScope(catchScope)
    }
    private fun resolveValueArgument(
        outerCallContext: BasicCallResolutionContext,
        startDataFlowInfo: DataFlowInfo,
        valueArgument: ValueArgument,
        isSpecialFunction: Boolean,
        tracingStrategy: TracingStrategy,
    ): PSICangJieCallArgument {
        val builtIns = outerCallContext.scope.ownerDescriptor.builtIns

        fun createParseErrorElement() = ParseErrorCangJieCallArgument(valueArgument, startDataFlowInfo)

        val argumentExpression = valueArgument.getArgumentExpression() ?: return createParseErrorElement()
        val cjExpression = CjPsiUtil.deparenthesize(argumentExpression) ?: createParseErrorElement()

        val argumentName = valueArgument.getArgumentName()?.asName

        @Suppress("NAME_SHADOWING")
        val outerCallContext = outerCallContext.expandContextForCatchClause(cjExpression)

        processFunctionalExpression(
            outerCallContext, argumentExpression, startDataFlowInfo,
            valueArgument, argumentName, builtIns, typeResolver
        )?.let {
            return it
        }

        if (cjExpression is CjCollectionLiteralExpression) {
            return CollectionLiteralCangJieCallArgumentImpl(
                valueArgument, argumentName, startDataFlowInfo, startDataFlowInfo, cjExpression, outerCallContext
            )
        }

        val context = outerCallContext.replaceContextDependency(ContextDependency.DEPENDENT)
            .replaceDataFlowInfo(startDataFlowInfo)
            .let {
                if (isSpecialFunction &&
                    argumentExpression is CjBlockExpression &&
                    ArgumentTypeResolver.getCallableReferenceExpressionIfAny(argumentExpression, it) != null
                ) {
                    it
                } else {
                    it.replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE)
                }
            }

//        if (cjExpression is CjCallableReferenceExpression) {
//            return createCallableReferenceCangJieCallArgument(
//                context, cjExpression, startDataFlowInfo, valueArgument, argumentName, outerCallContext, tracingStrategy
//            )
//        }

        // argumentExpression instead of cjExpression is hack -- type info should be stored also for parenthesized expression
        val typeInfo = expressionTypingServices.getTypeInfo(argumentExpression, context)

        return createSimplePSICallArgument(context, valueArgument, typeInfo) ?: createParseErrorElement()
    }
//    fun getLhsResult(context: BasicCallResolutionContext, ktExpression: CjCallableReferenceExpression): Pair<DoubleColonLHS?, LHSResult> {
//        val expressionTypingContext = ExpressionTypingContext.newContext(context)
//
//        if (ktExpression.isEmptyLHS) return null to LHSResult.Empty
//
//        val doubleColonLhs = (context.callPosition as? CallPosition.CallableReferenceRhs)?.lhs
//            ?: doubleColonExpressionResolver.resolveDoubleColonLHS(ktExpression, expressionTypingContext)
//            ?: return null to LHSResult.Empty
//        val lhsResult = when (doubleColonLhs) {
//            is DoubleColonLHS.Expression -> {
//                if (doubleColonLhs.isObjectQualifier) {
//                    val classifier = doubleColonLhs.type.constructor.declarationDescriptor
//                    val calleeExpression = ktExpression.receiverExpression?.getCalleeExpressionIfAny()
//                    if (calleeExpression is CjSimpleNameExpression && classifier is ClassDescriptor) {
//                        LHSResult.Object(ClassQualifier(calleeExpression, classifier))
//                    } else {
//                        LHSResult.Error
//                    }
//                } else {
//                    val fakeArgument = FakeValueArgumentForLeftCallableReference(ktExpression)
//
//                    val cangjieCallArgument = createSimplePSICallArgument(context, fakeArgument, doubleColonLhs.typeInfo)
//                    cangjieCallArgument?.let { LHSResult.Expression(it as SimpleCangJieCallArgument) } ?: LHSResult.Error
//                }
//            }
//            is DoubleColonLHS.Type -> {
//                val qualifiedExpression = ktExpression.receiverExpression!!
//                val qualifier = expressionTypingContext.trace.get(BindingContext.QUALIFIER, qualifiedExpression)
//                val classifier = doubleColonLhs.type.constructor.declarationDescriptor
//                if (classifier !is ClassDescriptor) {
//                    expressionTypingContext.trace.report(Errors.CALLABLE_REFERENCE_LHS_NOT_A_CLASS.on(ktExpression))
//                    LHSResult.Error
//                } else {
//                    LHSResult.Type(qualifier, doubleColonLhs.type.unwrap())
//                }
//            }
//        }
//
//        return doubleColonLhs to lhsResult
//    }
//    fun createCallableReferenceCangJieCallArgument(
//        context: BasicCallResolutionContext,
//        cjExpression: CjCallableReferenceExpression,
//        startDataFlowInfo: DataFlowInfo,
//        valueArgument: ValueArgument,
//        argumentName: Name?,
//        outerCallContext: BasicCallResolutionContext,
//        tracingStrategy: TracingStrategy
//    ): CallableReferenceCangJieCallArgumentImpl {
//        checkNoSpread(outerCallContext, valueArgument)
//
//        val (doubleColonLhs, lhsResult) = getLhsResult(context, cjExpression)
//        val newDataFlowInfo = (doubleColonLhs as? DoubleColonLHS.Expression)?.dataFlowInfo ?: startDataFlowInfo
//        val rhsExpression = cjExpression.callableReference
//        val rhsName = rhsExpression.getReferencedNameAsName()
//        val call = outerCallContext.trace[BindingContext.CALL, rhsExpression]
//            ?: CallMaker.makeCall(rhsExpression, null, null, rhsExpression, emptyList())
//        val cangjieCall = toCangJieCall(context, CangJieCallKind.CALLABLE_REFERENCE, call, rhsName, tracingStrategy, isSpecialFunction = false)
//
//        return CallableReferenceCangJieCallArgumentImpl(
//            ASTScopeTower(context, rhsExpression), valueArgument, startDataFlowInfo,
//            newDataFlowInfo, cjExpression, argumentName, lhsResult, rhsName, cangjieCall
//        )
//    }

    private fun NewResolutionOldInference.ResolutionKind.toCangJieCallKind(): CangJieCallKind =
        when (this) {
            is NewResolutionOldInference.ResolutionKind.Function -> CangJieCallKind.FUNCTION
//            is NewResolutionOldInference.ResolutionKind.Variable -> CangJieCallKind.VARIABLE
//            is NewResolutionOldInference.ResolutionKind.Invoke -> CangJieCallKind.INVOKE
//            is NewResolutionOldInference.ResolutionKind.CallableReference -> CangJieCallKind.CALLABLE_REFERENCE
            is NewResolutionOldInference.ResolutionKind.GivenCandidates -> CangJieCallKind.UNSUPPORTED
            NewResolutionOldInference.ResolutionKind.Variable -> TODO()
        }

    private fun refineNameForRemOperator(isBinaryRemOperator: Boolean, name: Name): Name {
        return name
//        val shouldUseOperatorRem = languageVersionSettings.supportsFeature(LanguageFeature.OperatorRem)
//        return if (isBinaryRemOperator && !shouldUseOperatorRem) OperatorConventions.REM_TO_MOD_OPERATION_NAMES[name]!! else name
    }

    private fun calculateExpectedType(context: BasicCallResolutionContext): UnwrappedType? {
        val expectedType = context.expectedType.unwrap()

        return if (context.contextDependency == ContextDependency.DEPENDENT) {
            if (TypeUtils.noExpectedType(expectedType)) null else expectedType
        } else {
            if (expectedType.isError) TypeUtils.NO_EXPECTED_TYPE else expectedType
        }
    }

    fun <D : CallableDescriptor> runResolutionAndInference(
        context: BasicCallResolutionContext,
        name: Name,
        resolutionKind: NewResolutionOldInference.ResolutionKind,
        tracingStrategy: TracingStrategy
    ): OverloadResolutionResults<D> {
//        return OverloadResolutionResultsImpl.nameNotFound()
        val isBinaryRemOperator = isBinaryRemOperator(context.call)
        val refinedName = refineNameForRemOperator(isBinaryRemOperator, name)
//
        val cangjieCallKind = resolutionKind.toCangJieCallKind()
        val cangjieCall = toCangJieCall(
            context,
            cangjieCallKind,
            context.call,
            refinedName,
            tracingStrategy,
            isSpecialFunction = false
        )
        val scopeTower = ASTScopeTower(context)
        val resolutionCallbacks = createResolutionCallbacks(context)
//
        val expectedType = calculateExpectedType(context)
        var result = cangjieCallResolver.resolveAndCompleteCall(
            scopeTower, resolutionCallbacks, cangjieCall, expectedType, context.collectAllCandidates
        )

//        val shouldUseOperatorRem = languageVersionSettings.supportsFeature(LanguageFeature.OperatorRem)
//        if (isBinaryRemOperator && shouldUseOperatorRem && (result.isEmpty() || result.areAllInapplicable())) {
//            result = resolveToDeprecatedMod(name, context, cangjieCallKind, tracingStrategy, scopeTower, resolutionCallbacks, expectedType)
//        }
//
//        if (result.isEmpty() && reportAdditionalDiagnosticIfNoCandidates(context, scopeTower, cangjieCallKind, cangjieCall)) {
//            return OverloadResolutionResultsImpl.nameNotFound()
//        }
////
        val overloadResolutionResults = convertToOverloadResolutionResults<D>(context, result, tracingStrategy)
////
        return overloadResolutionResults.also {
            clearCacheForApproximationResults()
            checkCallWithAdditionalResolve(it, scopeTower, resolutionCallbacks, expectedType, context)
        }
    }
    private fun <D : CallableDescriptor> checkCallWithAdditionalResolve(
        overloadResolutionResults: OverloadResolutionResults<D>,
        scopeTower: ImplicitScopeTower,
        resolutionCallbacks: CangJieResolutionCallbacks,
        expectedType: UnwrappedType?,
        context: BasicCallResolutionContext,
    ) {
        for (callChecker in callCheckersWithAdditionalResolve) {
            callChecker.check(overloadResolutionResults, scopeTower, resolutionCallbacks, expectedType, context)
        }
    }
    private fun clearCacheForApproximationResults() {
        // Mostly, we approximate captured or some other internal types that don't live longer than resolve for a call,
        // so it's quite useless to preserve cache for longer time
//        typeApproximator.clearCache()
    }
//
//    private fun <D : CallableDescriptor> checkCallWithAdditionalResolve(
//        overloadResolutionResults: OverloadResolutionResults<D>,
//        scopeTower: ImplicitScopeTower,
//        resolutionCallbacks: CangJieResolutionCallbacks,
//        expectedType: UnwrappedType?,
//        context: BasicCallResolutionContext,
//    ) {
//        for (callChecker in callCheckersWithAdditionalResolve) {
//            callChecker.check(overloadResolutionResults, scopeTower, resolutionCallbacks, expectedType, context)
//        }
//    }
//    private fun clearCacheForApproximationResults() {
//        // Mostly, we approximate captured or some other internal types that don't live longer than resolve for a call,
//        // so it's quite useless to preserve cache for longer time
//        typeApproximator.clearCache()
//    }
}
