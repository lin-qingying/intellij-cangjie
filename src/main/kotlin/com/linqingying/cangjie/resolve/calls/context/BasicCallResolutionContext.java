package com.linqingying.cangjie.resolve.calls.context;


import com.linqingying.cangjie.config.LanguageVersionSettings;
import com.linqingying.cangjie.descriptors.BindingTrace;
import com.linqingying.cangjie.psi.Call;
import com.linqingying.cangjie.psi.CjExpression;
import com.linqingying.cangjie.resolve.StatementFilter;
import com.linqingying.cangjie.resolve.calls.components.InferenceSession;
import com.linqingying.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.linqingying.cangjie.resolve.scopes.LexicalScope;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.expressions.ContextConfig;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class BasicCallResolutionContext extends CallResolutionContext<BasicCallResolutionContext> {
    private BasicCallResolutionContext(
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
                dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,
                isSaveTypeInfo, callPosition, expressionContextProvider
                ,
                languageVersionSettings,
                dataFlowValueFactory, inferenceSession, config);
    }

    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull Call call,
            @NotNull CangJieType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckArgumentTypesMode checkArguments,
            boolean isAnnotationContext,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession


    ) {
        return new BasicCallResolutionContext(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                new ResolutionResultsCacheImpl(), null,
                StatementFilter.NONE, isAnnotationContext, false, false, true,
                CallPosition.Unknown.INSTANCE, DEFAULT_EXPRESSION_CONTEXT_PROVIDER,
                languageVersionSettings,
                dataFlowValueFactory, inferenceSession, ContextConfig.DEFAULT);
    }

    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull Call call,
            @NotNull CangJieType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckArgumentTypesMode checkArguments,
            boolean isAnnotationContext,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory
    ) {
        return new BasicCallResolutionContext(trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments,
                new ResolutionResultsCacheImpl(), null,
                StatementFilter.NONE, isAnnotationContext, false, false, true,
                CallPosition.Unknown.INSTANCE, DEFAULT_EXPRESSION_CONTEXT_PROVIDER,
                languageVersionSettings,
                dataFlowValueFactory, InferenceSession.Companion.getDefault(), ContextConfig.DEFAULT);
    }

    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull ResolutionContext<?> context, @NotNull Call call, @NotNull CheckArgumentTypesMode checkArguments,
            @Nullable MutableDataFlowInfoForArguments dataFlowInfoForArguments
    ) {
        return new BasicCallResolutionContext(
                context.trace, context.scope, call, context.expectedType, context.dataFlowInfo, context.contextDependency, checkArguments,
                context.resolutionResultsCache, dataFlowInfoForArguments,
                context.statementFilter, context.isAnnotationContext, context.isDebuggerContext, context.collectAllCandidates, context.isSaveTypeInfo,
                context.callPosition, context.expressionContextProvider,
                context.languageVersionSettings,
                context.dataFlowValueFactory,
                context.inferenceSession, context.config);
    }

    @NotNull
    public static BasicCallResolutionContext create(
            @NotNull ResolutionContext context, @NotNull Call call, @NotNull CheckArgumentTypesMode checkArguments
    ) {
        return create(context, call, checkArguments, null);
    }

    @Override
    protected BasicCallResolutionContext create(
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
        return new BasicCallResolutionContext(
                trace, scope, call, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
                dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,
                isSaveTypeInfo, callPosition, expressionContextProvider,
                languageVersionSettings,
                dataFlowValueFactory, inferenceSession, config);
    }

    @NotNull
    public BasicCallResolutionContext replaceCall(@NotNull Call newCall) {
        return new BasicCallResolutionContext(
                trace, scope, newCall, expectedType, dataFlowInfo, contextDependency, checkArguments, resolutionResultsCache,
                dataFlowInfoForArguments, statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,
                isSaveTypeInfo, callPosition, expressionContextProvider,
                languageVersionSettings,
                dataFlowValueFactory, inferenceSession, config);
    }


}
