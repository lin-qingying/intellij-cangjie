package com.huawei.cangjie.resolve.calls.context;


import com.huawei.cangjie.config.LanguageVersionSettings;
import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.descriptors.CallableDescriptor;
import com.huawei.cangjie.psi.Call;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.resolve.StatementFilter;
import com.huawei.cangjie.resolve.calls.components.InferenceSession;
import com.huawei.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments;
import com.huawei.cangjie.resolve.calls.model.MutableResolvedCall;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.huawei.cangjie.resolve.calls.tasks.TracingStrategy;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.types.CangJieType;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public final class CallCandidateResolutionContext<D extends CallableDescriptor> extends CallResolutionContext<CallCandidateResolutionContext<D>> {
    @NotNull
    public final MutableResolvedCall<D> candidateCall;
    @NotNull
    public final TracingStrategy tracing;
    @NotNull
    public final CandidateResolveMode candidateResolveMode;

    private CallCandidateResolutionContext(
            @NotNull MutableResolvedCall<D> candidateCall,
            @NotNull TracingStrategy tracing,
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull Call call,
            @NotNull CangJieType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckArgumentTypesMode checkArguments,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments,
            @NotNull StatementFilter statementFilter,
            @NotNull CandidateResolveMode candidateResolveMode,
            boolean isAnnotationContext,
            boolean isDebuggerContext,
            boolean collectAllCandidates,
            boolean isSaveTypeInfo,
            @NotNull CallPosition callPosition,
            @NotNull Function1<CjExpression, CjExpression> expressionContextProvider,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
                dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext,
                collectAllCandidates,isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession);
        this.candidateCall = candidateCall;
        this.tracing = tracing;
        this.candidateResolveMode = candidateResolveMode;
    }

    public static <D extends CallableDescriptor> CallCandidateResolutionContext<D> create(
            @NotNull MutableResolvedCall<D> candidateCall, @NotNull CallResolutionContext<?> context, @NotNull BindingTrace trace,
            @NotNull TracingStrategy tracing, @NotNull Call call,
            @NotNull CandidateResolveMode candidateResolveMode
    ) {
        return new CallCandidateResolutionContext<>(
                candidateCall, tracing, trace, context.scope, call, context.expectedType,
                context.dataFlowInfo, context.contextDependency, context.checkArguments,
                context.resolutionResultsCache, context.dataFlowInfoForArguments,
                context.statementFilter,
                candidateResolveMode, context.isAnnotationContext, context.isDebuggerContext, context.collectAllCandidates,
                context.isSaveTypeInfo,       context.callPosition, context.expressionContextProvider, context.languageVersionSettings, context.dataFlowValueFactory,
                context.inferenceSession);
    }

    @NotNull
    public static <D extends CallableDescriptor> CallCandidateResolutionContext<D> createForCallBeingAnalyzed(
            @NotNull MutableResolvedCall<D> candidateCall, @NotNull BasicCallResolutionContext context, @NotNull TracingStrategy tracing
    ) {
        return new CallCandidateResolutionContext<>(
                candidateCall, tracing, context.trace, context.scope, context.call, context.expectedType,
                context.dataFlowInfo, context.contextDependency, context.checkArguments, context.resolutionResultsCache,
                context.dataFlowInfoForArguments, context.statementFilter,
                CandidateResolveMode.FULLY, context.isAnnotationContext, context.isDebuggerContext, context.collectAllCandidates,
             context.isSaveTypeInfo,   context.callPosition, context.expressionContextProvider, context.languageVersionSettings, context.dataFlowValueFactory,
                context.inferenceSession);
    }

    @Override
    protected CallCandidateResolutionContext<D> create(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull CangJieType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull StatementFilter statementFilter,
            boolean collectAllCandidates,
            boolean isSaveTypeInfo,
            @NotNull CallPosition callPosition,
            @NotNull Function1<CjExpression, CjExpression> expressionContextProvider,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    ) {
        return new CallCandidateResolutionContext<>(
                candidateCall, tracing, trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                resolutionResultsCache, dataFlowInfoForArguments, statementFilter,
                candidateResolveMode, isAnnotationContext, isDebuggerContext, collectAllCandidates,isSaveTypeInfo, callPosition, expressionContextProvider,
                languageVersionSettings, dataFlowValueFactory, inferenceSession);
    }
}
