package com.linqingying.cangjie.resolve.calls.context;


import com.linqingying.cangjie.config.LanguageVersionSettings;
import com.linqingying.cangjie.descriptors.BindingTrace;
import com.linqingying.cangjie.descriptors.CallableDescriptor;
import com.linqingying.cangjie.psi.Call;
import com.linqingying.cangjie.psi.CjExpression;
import com.linqingying.cangjie.resolve.StatementFilter;
import com.linqingying.cangjie.resolve.calls.components.InferenceSession;
import com.linqingying.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments;
import com.linqingying.cangjie.resolve.calls.model.MutableResolvedCall;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.linqingying.cangjie.resolve.calls.tasks.TracingStrategy;
import com.linqingying.cangjie.resolve.scopes.LexicalScope;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.expressions.ContextConfig;
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
            @NotNull InferenceSession inferenceSession,
            @NotNull ContextConfig config
    ) {
        super(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
                dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
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
                context.isSaveTypeInfo, context.callPosition, context.expressionContextProvider, context.languageVersionSettings, context.dataFlowValueFactory,
                context.inferenceSession, context.config);
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
                context.isSaveTypeInfo, context.callPosition, context.expressionContextProvider, context.languageVersionSettings, context.dataFlowValueFactory,
                context.inferenceSession, context.config);
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
            @NotNull InferenceSession inferenceSession,
            @NotNull ContextConfig config
    ) {
        return new CallCandidateResolutionContext<>(
                candidateCall, tracing, trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                resolutionResultsCache, dataFlowInfoForArguments, statementFilter,
                candidateResolveMode, isAnnotationContext, isDebuggerContext, collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider,
                languageVersionSettings, dataFlowValueFactory, inferenceSession, config);
    }
}
