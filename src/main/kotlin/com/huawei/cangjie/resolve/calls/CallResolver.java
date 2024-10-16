package com.huawei.cangjie.resolve.calls;

import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.builtins.FunctionTypesKt;
import com.huawei.cangjie.config.LanguageFeature;
import com.huawei.cangjie.config.LanguageVersionSettings;
import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.diagnostics.DiagnosticFactory0;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.progress.ProgressIndicatorAndCompilationCanceledStatus;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.*;
import com.huawei.cangjie.resolve.calls.components.InferenceSession;
import com.huawei.cangjie.resolve.calls.context.BasicCallResolutionContext;
import com.huawei.cangjie.resolve.calls.context.CheckArgumentTypesMode;
import com.huawei.cangjie.resolve.calls.context.ContextDependency;
import com.huawei.cangjie.resolve.calls.context.ResolutionContext;
import com.huawei.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments;
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResults;
import com.huawei.cangjie.resolve.calls.results.OverloadResolutionResultsImpl;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.huawei.cangjie.resolve.calls.tasks.*;
import com.huawei.cangjie.resolve.calls.tower.EnumClassCallableDescriptor;
import com.huawei.cangjie.resolve.calls.tower.NewResolutionOldInference;
import com.huawei.cangjie.resolve.calls.tower.PSICallResolver;
import com.huawei.cangjie.resolve.calls.util.CallMaker;
import com.huawei.cangjie.resolve.calls.util.CallResolverUtilKt;
import com.huawei.cangjie.resolve.calls.util.CallUtilKt;
import com.huawei.cangjie.resolve.calls.util.ResolveArgumentsMode;
import com.huawei.cangjie.resolve.descriptorUtil.DescriptorUtilsKt;
import com.huawei.cangjie.resolve.lazy.ForceResolveUtil;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.resolve.scopes.SyntheticScopes;
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.CangJieTypeKt;
import com.huawei.cangjie.types.expressions.ExpressionTypingContext;
import com.huawei.cangjie.types.expressions.ExpressionTypingServices;
import com.huawei.cangjie.types.expressions.ExpressionTypingVisitorDispatcher;
import com.huawei.cangjie.utils.OperatorNameConventions;
import com.huawei.cangjie.utils.PerformanceCounter;
import com.intellij.psi.PsiElement;
import jakarta.inject.Inject;
import kotlin.Pair;
import kotlin.collections.CollectionsKt;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.huawei.cangjie.diagnostics.Errors.*;
import static com.huawei.cangjie.resolve.calls.results.OverloadResolutionResults.Code.NAME_NOT_FOUND;
import static com.huawei.cangjie.resolve.calls.results.OverloadResolutionResults.Code.SUCCESS_NAME_NOT_FOUND;
import static com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE;


public class CallResolver {
    private static final PerformanceCounter callResolvePerfCounter = PerformanceCounter.Companion.create("Call resolve", ExpressionTypingVisitorDispatcher.typeInfoPerfCounter);
    private final LanguageVersionSettings languageVersionSettings;
    private final CangJieBuiltIns builtIns;
    private final DataFlowValueFactory dataFlowValueFactory;
    private ExpressionTypingServices expressionTypingServices;
    private SyntheticScopes syntheticScopes;
    private ArgumentTypeResolver argumentTypeResolver;
    private NewResolutionOldInference newResolutionOldInference;
    private PSICallResolver PSICallResolver;
    private TypeResolver typeResolver;

    public CallResolver(
            @NotNull CangJieBuiltIns builtIns,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory
    ) {
        this.builtIns = builtIns;
        this.languageVersionSettings = languageVersionSettings;
        this.dataFlowValueFactory = dataFlowValueFactory;
    }

    @NotNull
    private static Pair<Collection<OldResolutionCandidate<ConstructorDescriptor>>, BasicCallResolutionContext> prepareCandidatesAndContextForConstructorCall(
            @NotNull CangJieType superType,
            @NotNull BasicCallResolutionContext context,
            @NotNull SyntheticScopes syntheticScopes
    ) {
        if (!(superType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor)) {
            return new Pair<>(Collections.emptyList(), context);
        }

        // If any constructor has type parameter (currently it only can be true for ones from Java), try to infer arguments for them
        // Otherwise use NO_EXPECTED_TYPE and known type substitutor
        boolean anyConstructorHasDeclaredTypeParameters =
                anyConstructorHasDeclaredTypeParameters(superType.getConstructor().getDeclarationDescriptor());

        if (anyConstructorHasDeclaredTypeParameters) {
            context = context.replaceExpectedType(superType);
        }

        List<OldResolutionCandidate<ConstructorDescriptor>> candidates =
                CallResolverUtilKt.createResolutionCandidatesForConstructors(
                        context.scope, context.call, superType, !anyConstructorHasDeclaredTypeParameters, syntheticScopes
                );

        return new Pair<>(candidates, context);
    }

    private static boolean anyConstructorHasDeclaredTypeParameters(@Nullable ClassifierDescriptor classDescriptor) {
        if (!(classDescriptor instanceof ClassDescriptor)) return false;
        for (ConstructorDescriptor constructor : ((ClassDescriptor) classDescriptor).getConstructors()) {
            if (constructor.getTypeParameters().size() > constructor.getContainingDeclaration().getDeclaredTypeParameters().size())
                return true;
        }

        return false;
    }

    @Inject
    public void setSyntheticScopes(@NotNull SyntheticScopes syntheticScopes) {
        this.syntheticScopes = syntheticScopes;
    }

    @Inject
    public void setPSICallResolver(@NotNull PSICallResolver PSICallResolver) {
        this.PSICallResolver = PSICallResolver;
    }

    // component dependency cycle
    @Inject
    public void setTypeResolver(@NotNull TypeResolver typeResolver) {
        this.typeResolver = typeResolver;
    }

    /**
     * 计算任务并解析调用
     * <p>
     * 此方法用于在给定的上下文中计算任务并解析特定名称的调用它接受一个基本的调用解析上下文、一个名称、一个引用表达式和一个解析类型，
     * 并返回一个OverloadResolutionResults的实例，该实例包含解析的结果和任务
     *
     * @param context             调用解析的上下文，包含有关调用的信息和设置
     * @param name                要解析的调用名称
     * @param referenceExpression 一个CjReferenceExpression对象，表示对某个元素的引用
     * @param kind                解析的类型，指示解析应该如何执行
     * @param <D>                 一个扩展了CallableDescriptor的类型，表示可以被解析的调用描述符
     * @return 返回一个OverloadResolutionResults的实例，包含解析的结果和任务
     */
    @SuppressWarnings("WeakerAccess")
    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksAndResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull CjReferenceExpression referenceExpression,
            @NotNull NewResolutionOldInference.ResolutionKind kind
    ) {
        // 创建一个追踪策略，用于在解析过程中记录和追踪信息
        TracingStrategy tracing = TracingStrategyImpl.create(referenceExpression, context.call);
        // 调用重载的方法，计算任务并解析调用
        return computeTasksAndResolveCall(context, name, tracing, kind);
    }

    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> checkArgumentTypesAndFail(BasicCallResolutionContext context) {
        argumentTypeResolver.checkTypesWithNoCallee(context);
        return OverloadResolutionResultsImpl.nameNotFound();
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull ResolutionContext<?> context,
            @NotNull Call call,
            @NotNull CjReferenceExpression functionReference,
            @NotNull Name name
    ) {
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
        return computeTasksAndResolveCall(
                callResolutionContext, name, functionReference,
                NewResolutionOldInference.ResolutionKind.Function.INSTANCE);
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithGivenName(
            @NotNull ResolutionContext<?> context,
            @NotNull Call call,
            @NotNull Name name,
            @NotNull TracingStrategy tracing
    ) {
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
        return computeTasksAndResolveCall(callResolutionContext, name, tracing, NewResolutionOldInference.ResolutionKind.Function.INSTANCE);
    }

    public OverloadResolutionResults<FunctionDescriptor> resolveCallWithKnownCandidate(
            @NotNull Call call,
            @NotNull TracingStrategy tracing,
            @NotNull ResolutionContext<?> context,
            @NotNull OldResolutionCandidate<FunctionDescriptor> candidate,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        return callResolvePerfCounter.time(() -> {
            BasicCallResolutionContext basicCallResolutionContext =
                    BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS, dataFlowInfoForArguments);

            Set<OldResolutionCandidate<FunctionDescriptor>> candidates = Collections.singleton(candidate);

            ResolutionTask<FunctionDescriptor> resolutionTask = new ResolutionTask<>(
                    new NewResolutionOldInference.ResolutionKind.GivenCandidates(), null, candidates
            );

            return doResolveCallOrGetCachedResults(basicCallResolutionContext, resolutionTask, tracing);
        });
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveBinaryCall(
            ExpressionTypingContext context,
            ExpressionReceiver receiver,
            CjBinaryExpression binaryExpression,
            Name name
    ) {
        return resolveCallWithGivenName(
                context,
                CallMaker.makeCall(receiver, binaryExpression),
                binaryExpression.getOperationReference(),
                name
        );
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveBinaryCall(
            ExpressionTypingContext context,
            ExpressionReceiver receiver,
            CjBinaryExpression binaryExpression,
            @NotNull Collection<FunctionDescriptor> functionDescriptors
    ) {
        Call call = CallMaker.makeCall(receiver, binaryExpression);
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
        List<OldResolutionCandidate<FunctionDescriptor>> candidates = CollectionsKt.map(functionDescriptors, descriptor ->
                OldResolutionCandidate.create(
                        call,
                        descriptor,
                        null,
                        ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                        null));

        return computeTasksFromCandidatesAndResolvedCall(
                callResolutionContext, candidates, TracingStrategyImpl.create(binaryExpression.getOperationReference(), call));
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveRangeLiteralCallWithGivenDescriptor(
            @NotNull ExpressionTypingContext context,
            @NotNull CjRangeExpression expression,
            @NotNull Call call,
            @NotNull Collection<FunctionDescriptor> functionDescriptors
    ) {
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
        List<OldResolutionCandidate<FunctionDescriptor>> candidates = CollectionsKt.map(functionDescriptors, descriptor ->
                OldResolutionCandidate.create(
                        call,
                        descriptor,
                        null,
                        ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                        null));

        return computeTasksFromCandidatesAndResolvedCall(
                callResolutionContext, candidates, TracingStrategyImpl.create(expression, call));
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveBloackReturnCallWithGivenDescriptor(
            @NotNull ExpressionTypingContext context,
            @NotNull CjBlockExpression expression,
            @NotNull Call call,
            @NotNull Collection<FunctionDescriptor> functionDescriptors
    ) {
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
        List<OldResolutionCandidate<FunctionDescriptor>> candidates = CollectionsKt.map(functionDescriptors, descriptor ->
                OldResolutionCandidate.create(
                        call,
                        descriptor,
                        null,
                        ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                        null));

        return computeTasksFromCandidatesAndResolvedCall(
                callResolutionContext, candidates, new TracingStrategyBlockExpression(expression, call));
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCallExpressionWithGivenDescriptor(
            @NotNull ExpressionTypingContext context,
            @NotNull CjCallExpression expression,
            @NotNull Call call,
            @NotNull Collection<FunctionDescriptor> functionDescriptors
    ) {
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
        List<OldResolutionCandidate<FunctionDescriptor>> candidates = CollectionsKt.map(functionDescriptors, descriptor ->
                OldResolutionCandidate.create(
                        call,
                        descriptor,
                        null,
                        ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                        null));

        return computeTasksFromCandidatesAndResolvedCall(
                callResolutionContext, candidates, TracingStrategyImpl.create(expression, call));
    }

    @NotNull
    public OverloadResolutionResults<FunctionDescriptor> resolveCollectionLiteralCallWithGivenDescriptor(
            @NotNull ExpressionTypingContext context,
            @NotNull CjCollectionLiteralExpression expression,
            @NotNull Call call,
            @NotNull Collection<FunctionDescriptor> functionDescriptors
    ) {
        BasicCallResolutionContext callResolutionContext = BasicCallResolutionContext.create(context, call, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS);
        List<OldResolutionCandidate<FunctionDescriptor>> candidates = CollectionsKt.map(functionDescriptors, descriptor ->
                OldResolutionCandidate.create(
                        call,
                        descriptor,
                        null,
                        ExplicitReceiverKind.NO_EXPLICIT_RECEIVER,
                        null));

        return computeTasksFromCandidatesAndResolvedCall(
                callResolutionContext, candidates, TracingStrategyImpl.create(expression, call));
    }

    // component dependency cycle
    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }

    @NotNull
    private OverloadResolutionResults<ConstructorDescriptor> resolveConstructorDelegationCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull CjConstructorDelegationCall call,
            @NotNull CjConstructorDelegationReferenceExpression calleeExpression,
            @NotNull ClassDescriptor currentClassDescriptor
    ) {
        context.trace.record(BindingContext.LEXICAL_SCOPE, call, context.scope);

        boolean isThisCall = calleeExpression.isThis() /*|| currentClassDescriptor.getKind() == ClassKind.STRUCT*/;
        if (currentClassDescriptor.getKind() == ClassKind.ENUM && !isThisCall) {
            context.trace.report(DELEGATION_SUPER_CALL_IN_ENUM_CONSTRUCTOR.on(calleeExpression));
            return checkArgumentTypesAndFail(context);
        }

        ClassDescriptor delegateClassDescriptor = isThisCall ? currentClassDescriptor :
                DescriptorUtilsKt.getSuperClassOrAny(currentClassDescriptor);
        Collection<ClassConstructorDescriptor> constructors = delegateClassDescriptor.getConstructors();

//        if (!isThisCall && currentClassDescriptor.getUnsubstitutedPrimaryConstructor() != null) {
//            if (DescriptorUtils.canHaveDeclaredConstructors(currentClassDescriptor)) {
//                // Diagnostic is meaningless when reporting on interfaces and object
//                PsiElement reportOn = calcReportOn(calleeExpression);
//                context.trace.report(PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED.on(reportOn));
//            }
//            if (call.isImplicit()) return OverloadResolutionResultsImpl.nameNotFound();
//        }

        if (constructors.isEmpty()) {
            context.trace.report(NO_CONSTRUCTOR.on(CallUtilKt.getValueArgumentListOrElement(context.call)));
            return checkArgumentTypesAndFail(context);
        }


        CangJieType superType =
                isThisCall ? currentClassDescriptor.getDefaultType() : DescriptorUtils.getSuperClassType(currentClassDescriptor);

        Pair<Collection<OldResolutionCandidate<ConstructorDescriptor>>, BasicCallResolutionContext> candidatesAndContext =
                prepareCandidatesAndContextForConstructorCall(superType, context, syntheticScopes);
        Collection<OldResolutionCandidate<ConstructorDescriptor>> candidates = candidatesAndContext.getFirst();
        context = candidatesAndContext.getSecond();

        TracingStrategy tracing = call.isImplicit() ?
                new TracingStrategyForImplicitConstructorDelegationCall(call, context.call) :
                TracingStrategyImpl.create(calleeExpression, context.call);

        PsiElement reportOn = call.isImplicit() ? call : calleeExpression;
//
//        if (delegateClassDescriptor.isInner()
//                && !DescriptorResolver.checkHasOuterClassInstance(context.scope, context.trace, reportOn,
//                (ClassDescriptor) delegateClassDescriptor.getContainingDeclaration())) {
//            return checkArgumentTypesAndFail(context);
//        }

        return computeTasksFromCandidatesAndResolvedCall(context, candidates, tracing);
    }

    @NotNull
    private <D extends FunctionDescriptor> OverloadResolutionResults<D> computeTasksFromCandidatesAndResolvedCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull Collection<OldResolutionCandidate<D>> candidates,
            @NotNull TracingStrategy tracing
    ) {
        return callResolvePerfCounter.time(() -> {
            ResolutionTask<D> resolutionTask = new ResolutionTask<>(
                    new NewResolutionOldInference.ResolutionKind.GivenCandidates(), null, candidates
            );
            return doResolveCallOrGetCachedResults(context, resolutionTask, tracing);
        });
    }

    @NotNull
    private <D extends FunctionDescriptor> OverloadResolutionResults<D> computeTasksFromCandidatesAndResolvedCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull CjReferenceExpression referenceExpression,
            @NotNull Collection<OldResolutionCandidate<D>> candidates
    ) {
        return computeTasksFromCandidatesAndResolvedCall(context, candidates,
                TracingStrategyImpl.create(referenceExpression, context.call));
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public  OverloadResolutionResults<? extends CallableDescriptor> resolveEnumCall(@NotNull BasicCallResolutionContext context) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

        CjExpression calleeExpression = context.call.getCalleeExpression();

        CjElement callExpression = context.call.getCallElement();

        boolean isCall = callExpression instanceof CjCallExpression && ((CjCallExpression) callExpression).getValueArgumentList() != null;
        OverloadResolutionResults result = null;
        if (calleeExpression instanceof CjSimpleNameExpression expression) {
            if(isCall){
                context.call.setNoValueArgument(true);

            }

            result = computeTasksAndResolveCall(
                    context, expression.getReferencedNameAsName(), expression,
                    NewResolutionOldInference.ResolutionKind.Enum.INSTANCE);
            if(isCall){
                context.call.setNoValueArgument(false);


            }
        }

        if (result == null || result.isNothing()) {
            return OverloadResolutionResultsImpl.nameNotFound();
        }
        if (result.isSuccess() && !isCall) {
            return result;
        }
        EnumClassCallableDescriptor resultDescriptor;

        if (result.getResultingDescriptor() == null) {
            return result;
        } else {
            resultDescriptor = (EnumClassCallableDescriptor) result.getResultingDescriptor();
        }

//        判断结果是否有无参构造，如果没有则调用 resolveCallForInvoke
        if (resultDescriptor.hashUnsubstitutedPrimaryConstructor() && !isCall) {
            return result;
        }


//        CangJieType expectedType = NO_EXPECTED_TYPE;
//        if (calleeExpression instanceof CjLambdaExpression) {
//            int parameterNumber = ((CjLambdaExpression) calleeExpression).getValueParameters().size();
//            List<CangJieType> parameterTypes = new ArrayList<>(parameterNumber);
//            for (int i = 0; i < parameterNumber; i++) {
//                parameterTypes.add(NO_EXPECTED_TYPE);
//            }
//            expectedType = FunctionTypesKt.createFunctionType(
//                    builtIns, Annotations.EMPTY, null, Collections.emptyList(), parameterTypes, null, context.expectedType
//            );
//        }
        CangJieType calleeType = resultDescriptor.getReturnType();

        ExpressionReceiver expressionReceiver = ExpressionReceiver.Companion.create(calleeExpression, calleeType, context.trace.getBindingContext());

        Call call = new CallTransformer.CallForImplicitInvoke(context.call.getExplicitReceiver(), expressionReceiver, context.call,
                false);
        TracingStrategyForInvoke tracingForInvoke = new TracingStrategyForInvoke(calleeExpression, call, calleeType);
        if (isCall) {
            if(!resultDescriptor.getTypeParameters().isEmpty()){
//                如果有类型参数，去掉表达式中的类型参数
//                然而实际上，操作符函数不能有类型参数，可以直接设置为null
         call.setNoTypeParameter(true);
            }
            OverloadResolutionResults<FunctionDescriptor> temp =
                    resolveCallForInvoke(context.replaceCall(call), tracingForInvoke);
            call.setNoTypeParameter(false);


            return   temp.replaceCode(SUCCESS_NAME_NOT_FOUND);
        }

        return OverloadResolutionResultsImpl.nameNotFound();
    }

    @NotNull
    @SuppressWarnings("unchecked")
    public OverloadResolutionResults<FunctionDescriptor> resolveFunctionCall(@NotNull BasicCallResolutionContext context) {
        ProgressIndicatorAndCompilationCanceledStatus.checkCanceled();

//        Call.CallType callType = context.call.getCallType();
//        if (callType == Call.CallType.ARRAY_GET_METHOD || callType == Call.CallType.ARRAY_SET_METHOD) {
//            Name name = callType == Call.CallType.ARRAY_GET_METHOD ? OperatorNameConventions.GET : OperatorNameConventions.SET;
//            CjArrayAccessExpression arrayAccessExpression = (CjArrayAccessExpression) context.call.getCallElement();
//            return computeTasksAndResolveCall(
//                    context, name, arrayAccessExpression,
//                    NewResolutionOldInference.ResolutionKind.Function.INSTANCE);
//        }

        CjExpression calleeExpression = context.call.getCalleeExpression();
        if (calleeExpression instanceof CjSimpleNameExpression expression) {
            return computeTasksAndResolveCall(
                    context, expression.getReferencedNameAsName(), expression,
                    NewResolutionOldInference.ResolutionKind.Function.INSTANCE);
        } else if (calleeExpression instanceof CjConstructorCalleeExpression) {
            return (OverloadResolutionResults) resolveCallForConstructor(context, (CjConstructorCalleeExpression) calleeExpression);
        } else if (calleeExpression instanceof CjConstructorDelegationReferenceExpression) {
            CjConstructorDelegationCall delegationCall = (CjConstructorDelegationCall) context.call.getCallElement();
            DeclarationDescriptor container = context.scope.getOwnerDescriptor();
            assert container instanceof ConstructorDescriptor : "Trying to resolve KtConstructorDelegationCall not in constructor. scope.ownerDescriptor = " + container;
            return (OverloadResolutionResults) resolveConstructorDelegationCall(
                    context,
                    delegationCall,
                    (CjConstructorDelegationReferenceExpression) calleeExpression,
                    (ClassDescriptor) container.getContainingDeclaration()
            );
        } else if (calleeExpression == null) {
            return checkArgumentTypesAndFail(context);
        }

        // Here we handle the case where the callee expression must be something of type function, e.g. (foo.bar())(1, 2)
        CangJieType expectedType = NO_EXPECTED_TYPE;
        if (calleeExpression instanceof CjLambdaExpression) {
            int parameterNumber = ((CjLambdaExpression) calleeExpression).getValueParameters().size();
            List<CangJieType> parameterTypes = new ArrayList<>(parameterNumber);
            for (int i = 0; i < parameterNumber; i++) {
                parameterTypes.add(NO_EXPECTED_TYPE);
            }
            expectedType = FunctionTypesKt.createFunctionType(
                    builtIns, Annotations.EMPTY, null, Collections.emptyList(), parameterTypes, null, context.expectedType
            );
        }
        CangJieType calleeType = expressionTypingServices.safeGetType(
                context.scope, calleeExpression, expectedType, context.dataFlowInfo, context.inferenceSession, context.trace);
        ExpressionReceiver expressionReceiver = ExpressionReceiver.Companion.create(calleeExpression, calleeType, context.trace.getBindingContext());

        Call call = new CallTransformer.CallForImplicitInvoke(context.call.getExplicitReceiver(), expressionReceiver, context.call,
                false);
        TracingStrategyForInvoke tracingForInvoke = new TracingStrategyForInvoke(calleeExpression, call, calleeType);
        return resolveCallForInvoke(context.replaceCall(call), tracingForInvoke);
    }

    @NotNull
    private OverloadResolutionResults<FunctionDescriptor> resolveCallForInvoke(
            @NotNull BasicCallResolutionContext context,
            @NotNull TracingStrategy tracing
    ) {
        return computeTasksAndResolveCall(
                context, OperatorNameConventions.INVOKE, tracing,
                NewResolutionOldInference.ResolutionKind.Invoke.INSTANCE);
    }

    @Nullable
    public OverloadResolutionResults<ConstructorDescriptor> resolveConstructorDelegationCall(
            @NotNull BindingTrace trace, @NotNull LexicalScope scope, @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ClassConstructorDescriptor constructorDescriptor,
            @Nullable CjConstructorDelegationCall call,
            @Nullable InferenceSession inferenceSession
    ) {
        if (call == null) {
            return null;
        }
        // Method returns `null` when there is nothing to resolve in trivial cases like `null` call expression or
        // when super call should be conventional enum constructor and super call should be empty

        BasicCallResolutionContext context = BasicCallResolutionContext.create(
                trace, scope,
                CallMaker.makeCall(null, null, call),
                NO_EXPECTED_TYPE,
                dataFlowInfo, ContextDependency.INDEPENDENT, CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS,
                false,
                languageVersionSettings,
                dataFlowValueFactory,
                inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault());

        CjConstructorDelegationReferenceExpression calleeExpression = call.getCalleeExpression();

        if (calleeExpression == null) return checkArgumentTypesAndFail(context);

        ClassDescriptor currentClassDescriptor = constructorDescriptor.getContainingDeclaration();

        if (constructorDescriptor.getConstructedClass().getKind() == ClassKind.ENUM && call.isImplicit()) {
            if (currentClassDescriptor.getUnsubstitutedPrimaryConstructor() != null) {
                DiagnosticFactory0<PsiElement> warningOrError;

                if (languageVersionSettings.supportsFeature(LanguageFeature.RequiredPrimaryConstructorDelegationCallInEnums)) {
                    warningOrError = PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED; // error
                } else {
                    warningOrError = PRIMARY_CONSTRUCTOR_DELEGATION_CALL_EXPECTED_IN_ENUM; // warning
                }
                PsiElement reportOn = calcReportOn(calleeExpression);
                context.trace.report(warningOrError.on(reportOn));
            }
            return null;
        }

        return resolveConstructorDelegationCall(context, call, call.getCalleeExpression(), currentClassDescriptor);
    }

    @Nullable
    private PsiElement calcReportOn(@NotNull CjConstructorDelegationReferenceExpression calleeExpression) {
        PsiElement delegationCall = calleeExpression.getParent();
        return CallResolverUtilKt.reportOnElement(delegationCall);
    }

    private OverloadResolutionResults<ConstructorDescriptor> resolveCallForConstructor(
            @NotNull BasicCallResolutionContext context,
            @NotNull CjConstructorCalleeExpression expression
    ) {
        assert context.call.getExplicitReceiver() == null :
                "Constructor can't be invoked with explicit receiver: " + context.call.getCallElement().getText();

        context.trace.record(BindingContext.LEXICAL_SCOPE, context.call.getCallElement(), context.scope);

        CjReferenceExpression functionReference = expression.getConstructorReferenceExpression();
        CjTypeReference typeReference = expression.getTypeReference();
        if (functionReference == null || typeReference == null) {
            CallResolverUtilKt.checkForConstructorCallOnFunctionalType(typeReference, context);
            return checkArgumentTypesAndFail(context); // No type there
        }
        CangJieType constructedType = typeResolver.resolveType(context.scope, typeReference, context.trace, true);
        if (CangJieTypeKt.isError(constructedType)) {
            return checkArgumentTypesAndFail(context);
        }

        DeclarationDescriptor declarationDescriptor = constructedType.getConstructor().getDeclarationDescriptor();
        if (!(declarationDescriptor instanceof ClassDescriptor classDescriptor)) {
            context.trace.report(NOT_A_CLASS.on(expression));
            return checkArgumentTypesAndFail(context);
        }

        Collection<ClassConstructorDescriptor> constructors = classDescriptor.getConstructors();
        if (constructors.isEmpty()) {
            context.trace.report(NO_CONSTRUCTOR.on(CallUtilKt.getValueArgumentListOrElement(context.call)));
            return checkArgumentTypesAndFail(context);
        }

        return resolveConstructorCall(context, functionReference, constructedType);
    }

    @NotNull
    public OverloadResolutionResults<ConstructorDescriptor> resolveConstructorCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull CjReferenceExpression functionReference,
            @NotNull CangJieType constructedType
    ) {
        Pair<Collection<OldResolutionCandidate<ConstructorDescriptor>>, BasicCallResolutionContext> candidatesAndContext =
                prepareCandidatesAndContextForConstructorCall(constructedType, context, syntheticScopes);

        Collection<OldResolutionCandidate<ConstructorDescriptor>> candidates = candidatesAndContext.getFirst();
        context = candidatesAndContext.getSecond();

        return computeTasksFromCandidatesAndResolvedCall(context, functionReference, candidates);
    }

    /**
     * 计算任务并解析调用
     * <p>
     * 该方法主要用于在给定的调用上下文中，计算所有可能的解析任务，并根据这些任务解析调用
     * 它结合了调用的基本上下文、调用的名字、追踪策略以及解析的种类，来执行具体的解析逻辑
     *
     * @param context 调用解析的基本上下文，包含了进行解析所需的所有信息
     * @param name    调用的名字，用于识别和区分不同的调用
     * @param tracing 追踪策略，用于在解析过程中追踪和记录解析的路径
     * @param kind    解析的种类，表示解析的具体类型，可以是函数、构造器等
     * @param <D>     CallableDescriptor的子类，表示可以被调用的描述符类型
     * @return 返回一个OverloadResolutionResults对象，包含了解析的结果和相关信息
     */
    @SuppressWarnings("WeakerAccess")
    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksAndResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull TracingStrategy tracing,
            @NotNull NewResolutionOldInference.ResolutionKind kind
    ) {
        // 通过性能计数器来记录解析调用的时间
        return callResolvePerfCounter.time(() -> {
            // 创建一个解析任务，该任务根据解析的种类、调用的名字等信息初始化
            ResolutionTask<D> resolutionTask = new ResolutionTask<>(kind, name, null);
            // 执行解析任务或获取缓存的解析结果
            return doResolveCallOrGetCachedResults(context, resolutionTask, tracing);
        });
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
     */
    private <D extends CallableDescriptor> OverloadResolutionResults<D> doResolveCallOrGetCachedResults(
            @NotNull BasicCallResolutionContext context,
            @NotNull ResolutionTask<D> resolutionTask,
            @NotNull TracingStrategy tracing
    ) {
        // 获取调用表达式和追踪对象
        Call call = context.call;
        tracing.bindCall(context.trace, call);

        // 判断是否启用新的推断功能，并根据解析任务的种类选择解析策略
        boolean newInferenceEnabled = languageVersionSettings.supportsFeature(LanguageFeature.NewInference);
        NewResolutionOldInference.ResolutionKind resolutionKind = resolutionTask.resolutionKind;

        // 如果启用新推断功能且解析种类在默认解析种类列表中，则执行新的解析和推断过程
        if (
                newInferenceEnabled &&
                        PSICallResolver.getDefaultResolutionKinds().contains(resolutionKind)) {
            assert resolutionTask.name != null;
            BindingContextUtilsKt.recordScope(context.trace, context.scope, context.call.getCalleeExpression());
            return PSICallResolver.runResolutionAndInference(context, resolutionTask.name, resolutionKind, tracing);
        }

        // 如果启用新推断功能且解析种类为给定候选，则执行针对给定候选的解析和推断过程
        if (newInferenceEnabled && resolutionKind instanceof NewResolutionOldInference.ResolutionKind.GivenCandidates) {
            assert resolutionTask.givenCandidates != null;
            BindingContextUtilsKt.recordScope(context.trace, context.scope, context.call.getCalleeExpression());
            return PSICallResolver.runResolutionAndInferenceForGivenCandidates(context, resolutionTask.givenCandidates, tracing);
        }

        // 创建一个临时的追踪对象，用于记录解析调用过程中的信息
        TemporaryBindingTrace traceToResolveCall = TemporaryBindingTrace.create(context.trace, "trace to resolve call", call);

        // 使用临时追踪对象替换原有的上下文对象
        BasicCallResolutionContext newContext = context.replaceBindingTrace(traceToResolveCall);

        // 执行重载解析，并返回解析结果

        return doResolveCall(newContext, resolutionTask, tracing);
    }

    // component dependency cycle
    @Inject
    public void setResolutionOldInference(@NotNull NewResolutionOldInference newResolutionOldInference) {
        this.newResolutionOldInference = newResolutionOldInference;
    }

    @NotNull
    private <D extends CallableDescriptor> OverloadResolutionResultsImpl<D> doResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull ResolutionTask<D> resolutionTask,
            @NotNull TracingStrategy tracing
    ) {
        DataFlowInfo initialInfo = context.dataFlowInfoForArguments.getResultInfo();
        if (context.checkArguments == CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) {
            argumentTypeResolver.analyzeArgumentsAndRecordTypes(context, ResolveArgumentsMode.SHAPE_FUNCTION_ARGUMENTS);
        }

        List<CjTypeProjection> typeArguments = context.call.getTypeArguments();
        for (CjTypeProjection projection : typeArguments) {
            if (projection.getProjectionKind() != CjProjectionKind.NONE) {
                context.trace.report(PROJECTION_ON_NON_CLASS_TYPE_ARGUMENT.on(projection));
                ModifierCheckerCore.INSTANCE.check(projection, context.trace, null, languageVersionSettings);
            }
            CangJieType type = argumentTypeResolver.resolveTypeRefWithDefault(
                    projection.getTypeReference(), context.scope, context.trace,
                    null);
            if (type != null) {
                ForceResolveUtil.forceResolveAllContents(type);
            }
        }

        OverloadResolutionResultsImpl<D> result;
        if (!(resolutionTask.resolutionKind instanceof NewResolutionOldInference.ResolutionKind.GivenCandidates)) {
            assert resolutionTask.name != null;
            result = newResolutionOldInference.runResolution(context, resolutionTask.name, resolutionTask.resolutionKind, tracing);
        } else {
            assert resolutionTask.givenCandidates != null;
            result = newResolutionOldInference.runResolutionForGivenCandidates(context, tracing, resolutionTask.givenCandidates);
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
        return result;
    }

    // component dependency cycle
    @Inject
    public void setArgumentTypeResolver(@NotNull ArgumentTypeResolver argumentTypeResolver) {
        this.argumentTypeResolver = argumentTypeResolver;
    }

    @NotNull
    public OverloadResolutionResults<VariableDescriptor> resolveSimpleVariable(@NotNull BasicCallResolutionContext context) {
        CjExpression calleeExpression = context.call.getCalleeExpression();
        assert calleeExpression instanceof CjSimpleNameExpression;
        CjSimpleNameExpression nameExpression = (CjSimpleNameExpression) calleeExpression;
        Name referencedName = nameExpression.getReferencedNameAsName();
        return computeTasksAndResolveCall(
                context, referencedName, nameExpression,
                NewResolutionOldInference.ResolutionKind.Variable.INSTANCE);
    }

    //////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////////
    private static class ResolutionTask<D extends CallableDescriptor> {

        @Nullable
        final Name name;

        @Nullable
        final Collection<OldResolutionCandidate<D>> givenCandidates;

        @NotNull
        final NewResolutionOldInference.ResolutionKind resolutionKind;

        private ResolutionTask(
                @NotNull NewResolutionOldInference.ResolutionKind kind,
                @Nullable Name name,
//                ,
                @Nullable Collection<OldResolutionCandidate<D>> candidates
        ) {
            this.name = name;
            givenCandidates = candidates;
            resolutionKind = kind;
        }
    }
}
