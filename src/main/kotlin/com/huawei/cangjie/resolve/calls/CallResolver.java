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
import static com.huawei.cangjie.types.util.TypeUtils.NO_EXPECTED_TYPE;


public class CallResolver {
    private ExpressionTypingServices expressionTypingServices;
    private final LanguageVersionSettings languageVersionSettings;
    private SyntheticScopes syntheticScopes;

    private static final PerformanceCounter callResolvePerfCounter = PerformanceCounter.Companion.create("Call resolve", ExpressionTypingVisitorDispatcher.typeInfoPerfCounter);
    private ArgumentTypeResolver argumentTypeResolver;
    private NewResolutionOldInference newResolutionOldInference;
    private PSICallResolver PSICallResolver;
    private TypeResolver typeResolver;
    private final CangJieBuiltIns builtIns;
    private final DataFlowValueFactory dataFlowValueFactory;
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
    public CallResolver(
            @NotNull CangJieBuiltIns builtIns,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory
    ) {
        this.builtIns = builtIns;
        this.languageVersionSettings = languageVersionSettings;
        this.dataFlowValueFactory = dataFlowValueFactory;
    }
    @SuppressWarnings("WeakerAccess")
    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksAndResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull CjReferenceExpression referenceExpression,
            @NotNull NewResolutionOldInference.ResolutionKind kind
    ) {
        TracingStrategy tracing = TracingStrategyImpl.create(referenceExpression, context.call);
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
        return callResolvePerfCounter.<OverloadResolutionResults<FunctionDescriptor>>time(() -> {
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
        Call call =  CallMaker.makeCall(receiver, binaryExpression);
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

        boolean isThisCall = calleeExpression.isThis();
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
    private static Pair<Collection<OldResolutionCandidate<ConstructorDescriptor>>, BasicCallResolutionContext> prepareCandidatesAndContextForConstructorCall(
            @NotNull CangJieType superType,
            @NotNull BasicCallResolutionContext context,
            @NotNull SyntheticScopes syntheticScopes
    ) {
        if (!(superType.getConstructor().getDeclarationDescriptor() instanceof ClassDescriptor)) {
            return new Pair<>(Collections.<OldResolutionCandidate<ConstructorDescriptor>>emptyList(), context);
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
            if (constructor.getTypeParameters().size() > constructor.getContainingDeclaration().getDeclaredTypeParameters().size()) return true;
        }

        return false;
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
            @NotNull CjConstructorDelegationCall call,
            @Nullable InferenceSession inferenceSession
    ) {
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

    @SuppressWarnings("WeakerAccess")
    @NotNull
    public <D extends CallableDescriptor> OverloadResolutionResults<D> computeTasksAndResolveCall(
            @NotNull BasicCallResolutionContext context,
            @NotNull Name name,
            @NotNull TracingStrategy tracing,
            @NotNull NewResolutionOldInference.ResolutionKind kind
    ) {
//        return null;
        return callResolvePerfCounter.time(() -> {
            ResolutionTask<D> resolutionTask = new ResolutionTask<>(kind, name
                    , null
            );
            return doResolveCallOrGetCachedResults(context, resolutionTask, tracing);
        });
    }

    private <D extends CallableDescriptor> OverloadResolutionResults<D> doResolveCallOrGetCachedResults(
            @NotNull BasicCallResolutionContext context,
            @NotNull ResolutionTask<D> resolutionTask,
            @NotNull TracingStrategy tracing
    ) {
        Call call = context.call;
        tracing.bindCall(context.trace, call);


        boolean newInferenceEnabled = languageVersionSettings.supportsFeature(LanguageFeature.NewInference);
        NewResolutionOldInference.ResolutionKind resolutionKind = resolutionTask.resolutionKind;
        if (
                newInferenceEnabled &&
                PSICallResolver.getDefaultResolutionKinds().contains(resolutionKind)) {
            assert resolutionTask.name != null;
            BindingContextUtilsKt.recordScope(context.trace, context.scope, context.call.getCalleeExpression());
            return PSICallResolver.runResolutionAndInference(context, resolutionTask.name, resolutionKind, tracing);
        }
        if (newInferenceEnabled && resolutionKind instanceof NewResolutionOldInference.ResolutionKind.GivenCandidates) {
            assert resolutionTask.givenCandidates != null;
            BindingContextUtilsKt.recordScope(context.trace, context.scope, context.call.getCalleeExpression());
            return PSICallResolver.runResolutionAndInferenceForGivenCandidates(context, resolutionTask.givenCandidates, tracing);
        }
        TemporaryBindingTrace traceToResolveCall = TemporaryBindingTrace.create(context.trace, "trace to resolve call", call);

        BasicCallResolutionContext newContext = context.replaceBindingTrace(traceToResolveCall);

        OverloadResolutionResultsImpl<D> results = doResolveCall(newContext, resolutionTask, tracing);

        return results;

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
        }
        else {
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
