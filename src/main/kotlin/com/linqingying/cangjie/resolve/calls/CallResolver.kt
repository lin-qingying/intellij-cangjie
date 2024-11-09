package com.linqingying.cangjie.resolve.calls

import com.linqingying.cangjie.builtins.CangJieBuiltIns
import com.linqingying.cangjie.builtins.createFunctionType
import com.linqingying.cangjie.config.LanguageFeature
import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.descriptors.*
import com.linqingying.cangjie.descriptors.annotations.Annotations
import com.linqingying.cangjie.diagnostics.Errors
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus.checkCanceled
import com.linqingying.cangjie.psi.*
import com.linqingying.cangjie.psi.psiUtil.getStrictParentOfType
import com.linqingying.cangjie.resolve.BindingContext
import com.linqingying.cangjie.resolve.DescriptorUtils.getSuperClassType
import com.linqingying.cangjie.resolve.ModifierCheckerCore.check
import com.linqingying.cangjie.resolve.TemporaryBindingTrace
import com.linqingying.cangjie.resolve.TypeResolver
import com.linqingying.cangjie.resolve.calls.components.InferenceSession
import com.linqingying.cangjie.resolve.calls.components.InferenceSession.Companion.default
import com.linqingying.cangjie.resolve.calls.context.*
import com.linqingying.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments
import com.linqingying.cangjie.resolve.calls.results.ManyCandidates
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResults
import com.linqingying.cangjie.resolve.calls.results.OverloadResolutionResultsImpl
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory
import com.linqingying.cangjie.resolve.calls.tasks.*
import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategyImpl.Companion.create
import com.linqingying.cangjie.resolve.calls.tower.EnumClassCallableDescriptor
import com.linqingying.cangjie.resolve.calls.tower.NewResolutionOldInference

import com.linqingying.cangjie.resolve.calls.tower.PSICallResolver

import com.linqingying.cangjie.resolve.calls.util.*
import com.linqingying.cangjie.resolve.calls.util.CallMaker.makeCall
import com.linqingying.cangjie.resolve.descriptorUtil.getSuperClassOrAny
import com.linqingying.cangjie.resolve.lazy.ForceResolveUtil.forceResolveAllContents
import com.linqingying.cangjie.resolve.recordScope
import com.linqingying.cangjie.resolve.scopes.LexicalScope
import com.linqingying.cangjie.resolve.scopes.SyntheticScopes
import com.linqingying.cangjie.resolve.scopes.receivers.ExpressionReceiver
import com.linqingying.cangjie.resolve.scopes.receivers.ExpressionReceiver.Companion.create
import com.linqingying.cangjie.types.CangJieType
import com.linqingying.cangjie.types.expressions.ExpressionTypingContext
import com.linqingying.cangjie.types.expressions.ExpressionTypingServices
import com.linqingying.cangjie.types.expressions.ExpressionTypingVisitorDispatcher
import com.linqingying.cangjie.types.isError
import com.linqingying.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE
import com.linqingying.cangjie.utils.OperatorNameConventions
import com.linqingying.cangjie.utils.PerformanceCounter.Companion.create
import com.intellij.psi.PsiElement
import jakarta.inject.Inject

class CallResolver(
    private val builtIns: CangJieBuiltIns,
    private val languageVersionSettings: LanguageVersionSettings,
    private val dataFlowValueFactory: DataFlowValueFactory
) {
    private lateinit var expressionTypingServices: ExpressionTypingServices
    private lateinit var syntheticScopes: SyntheticScopes
    private lateinit var argumentTypeResolver: ArgumentTypeResolver
    private lateinit var newResolutionOldInference: NewResolutionOldInference
    private lateinit var psiCallResolver1: PSICallResolver
    private lateinit var typeResolver: TypeResolver

    @Inject
    fun setSyntheticScopes(syntheticScopes: SyntheticScopes) {
        this.syntheticScopes = syntheticScopes
    }

    @Inject
    fun setPSICallResolver(psiCallResolver: PSICallResolver) {
        this.psiCallResolver1 = psiCallResolver
    }

    // component dependency cycle
    @Inject
    fun setTypeResolver(typeResolver: TypeResolver) {
        this.typeResolver = typeResolver
    }

    /**
     * 计算任务并解析调用
     *
     *
     * 此方法用于在给定的上下文中计算任务并解析特定名称的调用它接受一个基本的调用解析上下文、一个名称、一个引用表达式和一个解析类型，
     * 并返回一个OverloadResolutionResults的实例，该实例包含解析的结果和任务
     *
     * @param context             调用解析的上下文，包含有关调用的信息和设置
     * @param name                要解析的调用名称
     * @param referenceExpression 一个CjReferenceExpression对象，表示对某个元素的引用
     * @param kind                解析的类型，指示解析应该如何执行
     * @param <D>                 一个扩展了CallableDescriptor的类型，表示可以被解析的调用描述符
     * @return 返回一个OverloadResolutionResults的实例，包含解析的结果和任务
    </D> */
    fun <D : CallableDescriptor> computeTasksAndResolveCall(
        context: BasicCallResolutionContext,
        name: Name,
        referenceExpression: CjReferenceExpression,
        kind: NewResolutionOldInference.ResolutionKind
    ): OverloadResolutionResults<D> {
        // 创建一个追踪策略，用于在解析过程中记录和追踪信息
        val tracing = create(referenceExpression, context.call)
        // 调用重载的方法，计算任务并解析调用
        return computeTasksAndResolveCall(context, name, tracing, kind)
    }

    private fun <D : CallableDescriptor?> checkArgumentTypesAndFail(context: BasicCallResolutionContext): OverloadResolutionResultsImpl<D> {
        argumentTypeResolver.checkTypesWithNoCallee(context)
        return OverloadResolutionResultsImpl.nameNotFound()
    }

    fun resolveCallWithGivenName(
        context: ResolutionContext<*>,
        call: Call,
        functionReference: CjReferenceExpression,
        name: Name
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        return computeTasksAndResolveCall(
            callResolutionContext, name, functionReference,
            NewResolutionOldInference.ResolutionKind.Function
        )
    }

    fun resolveCallWithGivenName(
        context: ResolutionContext<*>,
        call: Call,
        name: Name,
        tracing: TracingStrategy
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        return computeTasksAndResolveCall(
            callResolutionContext,
            name,
            tracing,
            NewResolutionOldInference.ResolutionKind.Function
        )
    }

    fun resolveCallWithKnownCandidate(
        call: Call,
        tracing: TracingStrategy,
        context: ResolutionContext<*>,
        candidate: OldResolutionCandidate<FunctionDescriptor>,
        dataFlowInfoForArguments: MutableDataFlowInfoForArguments?
    ): OverloadResolutionResults<FunctionDescriptor> {
        return callResolvePerfCounter.time {
            val basicCallResolutionContext =
                BasicCallResolutionContext.create(
                    context,
                    call,
                    CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                    dataFlowInfoForArguments
                )
            val candidates = setOf(candidate)

            val resolutionTask = ResolutionTask(
                NewResolutionOldInference.ResolutionKind.GivenCandidates(), null, candidates
            )
            doResolveCallOrGetCachedResults(basicCallResolutionContext, resolutionTask, tracing)
        }
    }

    fun resolveBinaryCall(
        context: ExpressionTypingContext,
        receiver: ExpressionReceiver,
        binaryExpression: CjBinaryExpression,
        name: Name
    ): OverloadResolutionResults<FunctionDescriptor> {
        return resolveCallWithGivenName(
            context,
            makeCall(receiver, binaryExpression),
            binaryExpression.operationReference,
            name
        )
    }

    fun resolveBinaryCall(
        context: ExpressionTypingContext,
        receiver: ExpressionReceiver,
        binaryExpression: CjBinaryExpression,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val call = makeCall(receiver, binaryExpression)
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }

        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, create(binaryExpression.operationReference, call)
        )
    }

    fun resolveRangeLiteralCallWithGivenDescriptor(
        context: ExpressionTypingContext,
        expression: CjRangeExpression,
        call: Call,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }

        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, create(expression, call)
        )
    }

    fun resolveBloackReturnCallWithGivenDescriptor(
        context: ExpressionTypingContext,
        expression: CjBlockExpression,
        call: Call,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }

        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, TracingStrategyBlockExpression(expression, call)
        )
    }

    fun resolveCallExpressionWithGivenDescriptor(
        context: ExpressionTypingContext,
        expression: CjCallExpression,
        call: Call,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }

        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, create(expression, call)
        )
    }

    fun resolveCollectionLiteralCallWithGivenDescriptor(
        context: ExpressionTypingContext,
        expression: CjCollectionLiteralExpression,
        call: Call,
        functionDescriptors: Collection<FunctionDescriptor>
    ): OverloadResolutionResults<FunctionDescriptor> {
        val callResolutionContext =
            BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS)
        val candidates = functionDescriptors.map { descriptor: FunctionDescriptor ->
            OldResolutionCandidate.create(
                call,
                descriptor,
                null,
                ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                null
            )
        }

        return computeTasksFromCandidatesAndResolvedCall(
            callResolutionContext, candidates, create(expression, call)
        )
    }

    // component dependency cycle
    @Inject
    fun setExpressionTypingServices(expressionTypingServices: ExpressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices
    }

    private fun resolveConstructorDelegationCall(
        context: BasicCallResolutionContext,
        call: CjConstructorDelegationCall,
        calleeExpression: CjConstructorDelegationReferenceExpression,
        currentClassDescriptor: ClassDescriptor
    ): OverloadResolutionResults<ConstructorDescriptor> {
        var context = context
        context.trace.record(BindingContext.LEXICAL_SCOPE, call, context.scope)

        val isThisCall = calleeExpression.isThis /*|| currentClassDescriptor.getKind() == ClassKind.STRUCT*/
        if (currentClassDescriptor.kind == ClassKind.ENUM && !isThisCall) {
            context.trace.report(Errors.DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR.on(calleeExpression))
            return checkArgumentTypesAndFail(context)
        }

        val delegateClassDescriptor =
            if (isThisCall) currentClassDescriptor else currentClassDescriptor.getSuperClassOrAny()
        val constructors = delegateClassDescriptor.constructors

        //        if (!isThisCall && currentClassDescriptor.getUnsubstitutedPrimaryConstructor() != null) {
//            if (DescriptorUtils.canHaveDeclaredConstructors(currentClassDescriptor)) {
//                // Diagnostic is meaningless when reporting on interfaces and object
//                PsiElement reportOn = calcReportOn(calleeExpression);
//                context.trace.report(PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED.on(reportOn));
//            }
//            if (call.isImplicit()) return OverloadResolutionResultsImpl.nameNotFound();
//        }
        if (constructors.isEmpty()) {
            context.trace.report(Errors.NO_CONSTRUCTOR.on(context.call.getValueArgumentListOrElement()))
            return checkArgumentTypesAndFail(context)
        }


        val superType =
            if (isThisCall) currentClassDescriptor.defaultType else getSuperClassType(currentClassDescriptor)

        val candidatesAndContext =
            prepareCandidatesAndContextForConstructorCall(
                superType, context,
                syntheticScopes
            )
        val candidates = candidatesAndContext.first
        context = candidatesAndContext.second

        val tracing =
            if (call.isImplicit) TracingStrategyForImplicitConstructorDelegationCall(call, context.call) else create(
                calleeExpression,
                context.call
            )

        val reportOn: PsiElement = if (call.isImplicit) call else calleeExpression

        //
//        if (delegateClassDescriptor.isInner()
//                && !DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, reportOn,
//                (ClassDescriptor) delegateClassDescriptor.getContainingDeclaration())) {
//            return checkArgumentTypesAndFail(context);
//        }
        return computeTasksFromCandidatesAndResolvedCall(context, candidates, tracing)
    }

    private fun <D : FunctionDescriptor> computeTasksFromCandidatesAndResolvedCall(
        context: BasicCallResolutionContext,
        candidates: Collection<OldResolutionCandidate<D>>,
        tracing: TracingStrategy
    ): OverloadResolutionResults<D> {
        return callResolvePerfCounter.time {
            val resolutionTask = ResolutionTask(
                NewResolutionOldInference.ResolutionKind.GivenCandidates(), null, candidates
            )
            doResolveCallOrGetCachedResults(context, resolutionTask, tracing)
        }
    }

    private fun <D : FunctionDescriptor> computeTasksFromCandidatesAndResolvedCall(
        context: BasicCallResolutionContext,
        referenceExpression: CjReferenceExpression,
        candidates: Collection<OldResolutionCandidate<D>>
    ): OverloadResolutionResults<D> {
        return computeTasksFromCandidatesAndResolvedCall(
            context, candidates,
            create(referenceExpression, context.call)
        )
    }

    fun resolveEnumCall(

        tcache: TemporaryTraceAndCache,

        context: BasicCallResolutionContext,
        kind: NewResolutionOldInference.ResolutionKind  =  NewResolutionOldInference.ResolutionKind.Enum
    ): OverloadResolutionResults<out CallableDescriptor> {
        checkCanceled()


        when (val calleeExpression = context.call.calleeExpression){
            is CjSimpleNameExpression -> {
                computeTasksAndResolveCall<CallableDescriptor>(
                    context, calleeExpression.getReferencedNameAsName(), calleeExpression,
                    kind
                )
            }
            null -> {
                return checkArgumentTypesAndFail(context)
            }
            else->{
                var expectedType: CangJieType = NO_EXPECTED_TYPE

                val calleeType = expressionTypingServices.safeGetType(
                    context.scope,
                    calleeExpression,
                    expectedType,
                    context.dataFlowInfo,
                    context.inferenceSession,
                    context.trace
                )
                val expressionReceiver = create(calleeExpression, calleeType, context.trace.bindingContext)

                val call: Call = CallTransformer.CallForImplicitInvoke(
                    context.call.explicitReceiver, expressionReceiver, context.call,
                    false
                )
                val tracingForInvoke = TracingStrategyForInvoke(calleeExpression, call, calleeType)
                return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke)
            }
        }

        val calleeExpression = context.call.calleeExpression

        val callExpression = context.call.callElement
        val dotParent = callExpression.getStrictParentOfType<CjDotQualifiedExpression>()
        var isCall = callExpression is CjCallExpression && callExpression.valueArgumentList != null
        var result: OverloadResolutionResults<*>? = null

        fun getResult() {
            //        TODO    a.b<Int> 这种情况不做处理了，只报错无法推断
            if (calleeExpression is CjSimpleNameExpression) {

                result = computeTasksAndResolveCall<CallableDescriptor>(
                    context, calleeExpression.getReferencedNameAsName(), calleeExpression,
                    kind
                )

            }
        }

        getResult()

        if (result is ManyCandidates) {
            return result!!
        }

        if (result == null || result!!.isNothing) {
            return OverloadResolutionResultsImpl.nameNotFound()
        }


        val resultDescriptor: EnumClassCallableDescriptor

        if (result!!.resultingDescriptor == null) {
            return result!!
        } else {
            resultDescriptor = result!!.resultingDescriptor as EnumClassCallableDescriptor
        }

        if(kind == NewResolutionOldInference.ResolutionKind.CaseEnum){
            return result!!
        }

        //        判断结果是否有无参构造，如果没有则调用 resolveCallForInvoke
        if (resultDescriptor.hashUnsubstitutedPrimaryConstructor() && !isCall) {
//            有无参构造，并且不是call
            return result!!
        } else if (!resultDescriptor.hashUnsubstitutedPrimaryConstructor() && isCall) {

            return result!!

        }
        val calleeType = resultDescriptor.returnType

        val expressionReceiver = create(
            calleeExpression!!, calleeType!!, context.trace.bindingContext
        )

        val call: Call = CallTransformer.CallForImplicitInvoke(
            context.call.explicitReceiver, expressionReceiver, context.call,
            false
        )
        val tracingForInvoke = TracingStrategyForInvoke(
            calleeExpression, call, calleeType
        )
        if (isCall) {
            if (resultDescriptor.typeParameters.isNotEmpty()) {
//                如果有类型参数，去掉表达式中的类型参数
//                然而实际上，操作符函数不能有类型参数，可以直接设置为null
                call.noTypeParameter = true
            }
            val temp =
                resolveCallForInvoke(context.replaceCall(call), tracingForInvoke)
            call.noTypeParameter = false


            return temp.replaceCode(OverloadResolutionResults.Code.SUCCESS_NAME_NOT_FOUND)
        }

        return OverloadResolutionResultsImpl.nameNotFound()
    }

    fun resolveFunctionCall(context: BasicCallResolutionContext): OverloadResolutionResults<out FunctionDescriptor> {
        checkCanceled()

        //        Call.CallType callType = context.call.getCallType();
//        if (callType == Call.CallType.ARRAY_GET_METHOD || callType == Call.CallType.ARRAY_SET_METHOD) {
//            Name name = callType == Call.CallType.ARRAY_GET_METHOD ? OperatorNameConventions.GET : OperatorNameConventions.SET;
//            CjArrayAccessExpression arrayAccessExpression = (CjArrayAccessExpression) context.call.getCallElement();
//            return computeTasksAndResolveCall(
//                    context, name, arrayAccessExpression,
//                    NewResolutionOldInference.ResolutionKind.Function.INSTANCE);
//        }
        when (val calleeExpression = context.call.calleeExpression) {
            is CjSimpleNameExpression -> {
                return computeTasksAndResolveCall(
                    context, calleeExpression.getReferencedNameAsName(), calleeExpression,
                    NewResolutionOldInference.ResolutionKind.Function
                )
            }

            is CjConstructorCalleeExpression -> {
                return resolveCallForConstructor(context, calleeExpression)
            }

            is CjConstructorDelegationReferenceExpression -> {
                val delegationCall = context.call.callElement as CjConstructorDelegationCall
                val container = context.scope.ownerDescriptor
                assert(container is ConstructorDescriptor) { "Trying to resolve KtConstructorDelegationCall not in constructor. scope.ownerDescriptor = $container" }
                return resolveConstructorDelegationCall(
                    context,
                    delegationCall,
                    calleeExpression,
                    container.containingDeclaration as ClassDescriptor
                )
            }

            null -> {
                return checkArgumentTypesAndFail(context)
            }

            // Here we handle the case where the callee expression must be something of type function, e.g. (foo.bar())(1, 2)
            else -> {
                var expectedType: CangJieType = NO_EXPECTED_TYPE
                if (calleeExpression is CjLambdaExpression) {
                    val parameterNumber = calleeExpression.valueParameters.size
                    val parameterTypes: MutableList<CangJieType> = ArrayList(parameterNumber)
                    for (i in 0 until parameterNumber) {
                        parameterTypes.add(NO_EXPECTED_TYPE)
                    }
                    expectedType = createFunctionType(
                        builtIns, Annotations.EMPTY, null, emptyList(), parameterTypes, null, context.expectedType
                    )
                }
                val calleeType = expressionTypingServices.safeGetType(
                    context.scope,
                    calleeExpression,
                    expectedType,
                    context.dataFlowInfo,
                    context.inferenceSession,
                    context.trace
                )
                val expressionReceiver = create(calleeExpression, calleeType, context.trace.bindingContext)

                val call: Call = CallTransformer.CallForImplicitInvoke(
                    context.call.explicitReceiver, expressionReceiver, context.call,
                    false
                )
                val tracingForInvoke = TracingStrategyForInvoke(calleeExpression, call, calleeType)
                return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke)
            }
        }
    }

    private fun resolveCallForInvoke(
        context: BasicCallResolutionContext,
        tracing: TracingStrategy
    ): OverloadResolutionResults<FunctionDescriptor> {
        return computeTasksAndResolveCall(
            context, OperatorNameConventions.INVOKE, tracing,
            NewResolutionOldInference.ResolutionKind.Invoke
        )
    }

    fun resolveConstructorDelegationCall(
        trace: BindingTrace, scope: LexicalScope, dataFlowInfo: DataFlowInfo,
        constructorDescriptor: ClassConstructorDescriptor,
        call: CjConstructorDelegationCall?,
        inferenceSession: InferenceSession?
    ): OverloadResolutionResults<ConstructorDescriptor>? {
        if (call == null) {
            return null
        }

        // Method returns `null` when there is nothing to resolve in trivial cases like `null` call expression or
        // when super call should be conventional enum constructor and super call should be empty
        val context = BasicCallResolutionContext.create(
            trace, scope,
            makeCall(null, null, call),
            NO_EXPECTED_TYPE,
            dataFlowInfo, ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
            false,
            languageVersionSettings,
            dataFlowValueFactory,
            inferenceSession ?: default
        )

        val calleeExpression = call.calleeExpression
            ?: return checkArgumentTypesAndFail(context)

        val currentClassDescriptor = constructorDescriptor.containingDeclaration

        if (constructorDescriptor.constructedClass.kind == ClassKind.ENUM && call.isImplicit) {
            if (currentClassDescriptor.unsubstitutedPrimaryConstructor != null) {
                val warningOrError =
                    if (languageVersionSettings.supportsFeature(LanguageFeature.RequiredPrimaryConstructorDelegationCallInEnums)) {
                        Errors.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED // error
                    } else {
                        Errors.PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED_IN_ENUM // warning
                    }
                val reportOn = calcReportOn(calleeExpression)
                context.trace.report(warningOrError.on(reportOn))
            }
            return null
        }

        return resolveConstructorDelegationCall(context, call, call.calleeExpression!!, currentClassDescriptor)
    }

    private fun calcReportOn(calleeExpression: CjConstructorDelegationReferenceExpression): PsiElement {
        val delegationCall = calleeExpression.parent
        return delegationCall!!.reportOnElement()
    }

    private fun resolveCallForConstructor(
        context: BasicCallResolutionContext,
        expression: CjConstructorCalleeExpression
    ): OverloadResolutionResults<ConstructorDescriptor> {
        assert(context.call.explicitReceiver == null) { "Constructor can't be invoked with explicit receiver: " + context.call.callElement.text }

        context.trace.record(BindingContext.LEXICAL_SCOPE, context.call.callElement, context.scope)

        val functionReference: CjReferenceExpression? = expression.constructorReferenceExpression
        val typeReference = expression.typeReference
        if (functionReference == null || typeReference == null) {
            checkForConstructorCallOnFunctionalType(typeReference, context)
            return checkArgumentTypesAndFail(context) // No type there
        }
        val constructedType = typeResolver.resolveType(context.scope, typeReference, context.trace, true)
        if (constructedType.isError) {
            return checkArgumentTypesAndFail(context)
        }

        val declarationDescriptor: DeclarationDescriptor? = constructedType.constructor.declarationDescriptor
        if (declarationDescriptor !is ClassDescriptor) {
            context.trace.report(Errors.NOT_A_CLASS.on(expression))
            return checkArgumentTypesAndFail(context)
        }

        val constructors = declarationDescriptor.constructors
        if (constructors.isEmpty()) {
            context.trace.report(Errors.NO_CONSTRUCTOR.on(context.call.getValueArgumentListOrElement()))
            return checkArgumentTypesAndFail(context)
        }

        return resolveConstructorCall(context, functionReference, constructedType)
    }

    fun resolveConstructorCall(
        context: BasicCallResolutionContext,
        functionReference: CjReferenceExpression,
        constructedType: CangJieType
    ): OverloadResolutionResults<ConstructorDescriptor> {
        var context = context
        val candidatesAndContext =
            prepareCandidatesAndContextForConstructorCall(
                constructedType, context,
                syntheticScopes
            )

        val candidates = candidatesAndContext.first
        context = candidatesAndContext.second

        return computeTasksFromCandidatesAndResolvedCall(context, functionReference, candidates)
    }

    /**
     * 计算任务并解析调用
     *
     *
     * 该方法主要用于在给定的调用上下文中，计算所有可能的解析任务，并根据这些任务解析调用
     * 它结合了调用的基本上下文、调用的名字、追踪策略以及解析的种类，来执行具体的解析逻辑
     *
     * @param context 调用解析的基本上下文，包含了进行解析所需的所有信息
     * @param name    调用的名字，用于识别和区分不同的调用
     * @param tracing 追踪策略，用于在解析过程中追踪和记录解析的路径
     * @param kind    解析的种类，表示解析的具体类型，可以是函数、构造器等
     * @param <D>     CallableDescriptor的子类，表示可以被调用的描述符类型
     * @return 返回一个OverloadResolutionResults对象，包含了解析的结果和相关信息
    </D> */
    fun <D : CallableDescriptor> computeTasksAndResolveCall(
        context: BasicCallResolutionContext,
        name: Name,
        tracing: TracingStrategy,
        kind: NewResolutionOldInference.ResolutionKind
    ): OverloadResolutionResults<D> {
        // 通过性能计数器来记录解析调用的时间
        return callResolvePerfCounter.time {
            // 创建一个解析任务，该任务根据解析的种类、调用的名字等信息初始化
            val resolutionTask = ResolutionTask<D>(kind, name, null)
            doResolveCallOrGetCachedResults(context, resolutionTask, tracing)
        }
    }


    /**
     * 执行函数或方法的重载解析，或者获取缓存的解析结果
     * 该方法是实际进行重载解析的核心逻辑，它会根据不同的情况调用不同的解析策略
     *
     * @param context        调用解析的上下文信息，包括调用表达式、当前作用域等
     * @param resolutionTask 解析任务对象，包含了需要解析的描述符类型以及解析的种类
     * @param tracing        追踪策略对象，用于绑定调用并记录解析过程中的信息
     * @param <D>            可调用描述符的类型，继承自CallableDescriptor
     * @return 返回解析结果对象，包含成功或失败的解析信息
    </D> */
    private fun <D : CallableDescriptor> doResolveCallOrGetCachedResults(
        context: BasicCallResolutionContext,
        resolutionTask: ResolutionTask<D>,
        tracing: TracingStrategy
    ): OverloadResolutionResults<D> {
        // 获取调用表达式和追踪对象
        val call = context.call
        tracing.bindCall(context.trace, call)

        // 判断是否启用新的推断功能，并根据解析任务的种类选择解析策略
        val newInferenceEnabled = languageVersionSettings.supportsFeature(LanguageFeature.NewInference)
        val resolutionKind = resolutionTask.resolutionKind

        // 如果启用新推断功能且解析种类在默认解析种类列表中，则执行新的解析和推断过程
        if (newInferenceEnabled &&
            psiCallResolver1.defaultResolutionKinds.contains(resolutionKind)
        ) {
            checkNotNull(resolutionTask.name)
            context.trace.recordScope(context.scope, context.call.calleeExpression)
            return psiCallResolver1.runResolutionAndInference(context, resolutionTask.name, resolutionKind, tracing)
        }

        // 如果启用新推断功能且解析种类为给定候选，则执行针对给定候选的解析和推断过程
        if (newInferenceEnabled && resolutionKind is NewResolutionOldInference.ResolutionKind.GivenCandidates) {
            checkNotNull(resolutionTask.givenCandidates)
            context.trace.recordScope(context.scope, context.call.calleeExpression)
            return psiCallResolver1.runResolutionAndInferenceForGivenCandidates(
                context,
                resolutionTask.givenCandidates,
                tracing
            )
        }

        // 创建一个临时的追踪对象，用于记录解析调用过程中的信息
        val traceToResolveCall = TemporaryBindingTrace.create(context.trace, "trace to resolve call", call)

        // 使用临时追踪对象替换原有的上下文对象
        val newContext = context.replaceBindingTrace(traceToResolveCall)

        // 执行重载解析，并返回解析结果
        return doResolveCall(newContext, resolutionTask, tracing)
    }

    // component dependency cycle
    @Inject
    fun setResolutionOldInference(newResolutionOldInference: NewResolutionOldInference) {
        this.newResolutionOldInference = newResolutionOldInference
    }

    private fun <D : CallableDescriptor> doResolveCall(
        context: BasicCallResolutionContext,
        resolutionTask: ResolutionTask<D>,
        tracing: TracingStrategy
    ): OverloadResolutionResultsImpl<D> {
        val initialInfo = context.dataFlowInfoForArguments.resultInfo
        if (context.checkArguments == CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) {
            argumentTypeResolver.analyzeArgumentsAndRecordTypes(
                context,
                ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS
            )
        }

        val typeArguments = context.call.typeArguments
        for (projection in typeArguments) {
            if (projection.projectionKind != CjProjectionKind.NONE) {
                context.trace.report(Errors.PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection))
                check(
                    projection, context.trace, null,
                    languageVersionSettings
                )
            }
            val type = argumentTypeResolver.resolveTypeRefWithDefault(
                projection.typeReference, context.scope, context.trace,
                null
            )
            if (type != null) {
                forceResolveAllContents(type)
            }
        }

        val result: OverloadResolutionResultsImpl<D> =
            if (resolutionTask.resolutionKind !is NewResolutionOldInference.ResolutionKind.GivenCandidates) {

                newResolutionOldInference.runResolution(
                    context,
                    resolutionTask.name!!,
                    resolutionTask.resolutionKind,
                    tracing
                )
            } else {

                newResolutionOldInference.runResolutionForGivenCandidates(
                    context,
                    tracing,
                    resolutionTask.givenCandidates!!
                )
            }

        // in code like
        //   assert(a!!.isEmpty())
        //   a.length
        // we should ignore data flow info from assert argument, since assertions can be disabled and
        // thus it will lead to NPE in runtime otherwise
//        if (languageVersionSettings.getFlag(AnalysisFlags.getIgnoreDataFlowInAssert()) && result.isSingleResult()) {
//            D descriptor = result.getResultingDescriptor();
//            if (descriptor.getName().equals(Name.identifier("assert"))) {
//                DeclarationDescriptor declaration = descriptor.getContainingDeclaration();
//                if (declaration instanceof PackageFragmentDescriptor &&
//                        ((PackageFragmentDescriptor) declaration).getFqName().asString().equals("cangjie")) {
//                    context.dataFlowInfoForArguments.updateInfo(context.call.getValueArguments().get(0), initialInfo);
//                }
//            }
//        }
        return result
    }

    // component dependency cycle
    @Inject
    fun setArgumentTypeResolver(argumentTypeResolver: ArgumentTypeResolver) {
        this.argumentTypeResolver = argumentTypeResolver
    }

    fun resolveSimpleVariable(context: BasicCallResolutionContext): OverloadResolutionResults<VariableDescriptor> {
        val calleeExpression = context.call.calleeExpression
        assert(calleeExpression is CjSimpleNameExpression)
        val nameExpression = calleeExpression as CjSimpleNameExpression
        val referencedName = nameExpression.getReferencedNameAsName()
        return computeTasksAndResolveCall(
            context, referencedName, nameExpression,
            NewResolutionOldInference.ResolutionKind.Variable
        )
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private class ResolutionTask<D : CallableDescriptor?>(
        val resolutionKind: NewResolutionOldInference.ResolutionKind,
        val name: Name?,  //                ,
        val givenCandidates: Collection<OldResolutionCandidate<D>>?
    )

    companion object {
        private val callResolvePerfCounter =
            create("Call resolve", ExpressionTypingVisitorDispatcher.typeInfoPerfCounter)

        private fun prepareCandidatesAndContextForConstructorCall(
            superType: CangJieType,
            context: BasicCallResolutionContext,
            syntheticScopes: SyntheticScopes
        ): Pair<Collection<OldResolutionCandidate<ConstructorDescriptor>>, BasicCallResolutionContext> {
            var context = context
            if (superType.constructor.declarationDescriptor !is ClassDescriptor) {
                return Pair<Collection<OldResolutionCandidate<ConstructorDescriptor>>, BasicCallResolutionContext>(
                    emptyList(), context
                )
            }

            // If any constructor has type parameter (currently it only can be true for ones from Java), try to infer arguments for them
            // Otherwise use NO_EXPECTED_TYPE and known type substitutor
            val anyConstructorHasDeclaredTypeParameters =
                anyConstructorHasDeclaredTypeParameters(superType.constructor.declarationDescriptor)

            if (anyConstructorHasDeclaredTypeParameters) {
                context = context.replaceExpectedType(superType)
            }

            val candidates =
                createResolutionCandidatesForConstructors(
                    context.scope, context.call, superType, !anyConstructorHasDeclaredTypeParameters, syntheticScopes
                )

            return Pair<Collection<OldResolutionCandidate<ConstructorDescriptor>>, BasicCallResolutionContext>(
                candidates,
                context
            )
        }

        private fun anyConstructorHasDeclaredTypeParameters(classDescriptor: ClassifierDescriptor?): Boolean {
            if (classDescriptor !is ClassDescriptor) return false
            for (constructor in classDescriptor.constructors) {
                if (constructor.typeParameters.size > constructor.containingDeclaration
                        .getDeclaredTypeParameters().size
                ) return true
            }

            return false
        }
    }
}
