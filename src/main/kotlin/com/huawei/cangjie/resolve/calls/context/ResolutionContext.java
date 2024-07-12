package com.huawei.cangjie.resolve.calls.context;


import com.huawei.cangjie.config.LanguageVersionSettings;
import com.huawei.cangjie.config.LanguageVersionSettingsImpl;
import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.resolve.StatementFilter;
import com.huawei.cangjie.resolve.calls.components.InferenceSession;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeUtils;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class together with its descendants is intended to transfer data flow analysis information
 * in top-down direction, from AST parents to children.
 * <p>
 * NB: all descendants must be immutable!
 */
public abstract class ResolutionContext<Context extends ResolutionContext<Context>> {
    public static final Function1<CjExpression, CjExpression> DEFAULT_EXPRESSION_CONTEXT_PROVIDER = expression -> null;
    @NotNull
    public final BindingTrace trace;
    @NotNull
    public final LexicalScope scope;
    @NotNull
    public final CangJieType expectedType;
    @NotNull
    public final DataFlowInfo dataFlowInfo;
    @NotNull
    public final ContextDependency contextDependency;
    @NotNull
    public final ResolutionResultsCache resolutionResultsCache;
    @NotNull
    public final StatementFilter statementFilter;
    public final boolean isAnnotationContext;
    public final boolean isDebuggerContext;
    public final boolean collectAllCandidates;
    @NotNull
    public final CallPosition callPosition;
    @NotNull
    public final DataFlowValueFactory dataFlowValueFactory;
    @NotNull
    public final InferenceSession inferenceSession;

    @NotNull
    public final LanguageVersionSettings languageVersionSettings ;
    /**
     * Used for analyzing expression in the given context.
     * Should be used for going through parents to find containing function, loop etc.
     * The provider should return specific context expression (which can be used instead of parent)
     * for the given expression or null otherwise.
     *
     * @see #getContextParentOfType
     */
    @NotNull
    public final Function1<CjExpression, CjExpression> expressionContextProvider;
    @NotNull
    public Context replaceContextDependency(@NotNull ContextDependency newContextDependency) {
        if (newContextDependency == contextDependency) return self();
        return create(trace, scope, dataFlowInfo, expectedType, newContextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession);
    }
    protected ResolutionContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull CangJieType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull StatementFilter statementFilter,
            boolean isAnnotationContext,
            boolean isDebuggerContext,
            boolean collectAllCandidates,
            @NotNull CallPosition callPosition,
            @NotNull Function1<CjExpression, CjExpression> expressionContextProvider,
            @NotNull LanguageVersionSettings languageVersionSettings,

            @NotNull DataFlowValueFactory factory,
            @NotNull InferenceSession inferenceSession
    ) {
        this.trace = trace;
        this.scope = scope;
        this.expectedType = expectedType;
        this.dataFlowInfo = dataFlowInfo;
        this.contextDependency = contextDependency;
        this.resolutionResultsCache = resolutionResultsCache;
        this.statementFilter = statementFilter;
        this.isAnnotationContext = isAnnotationContext;
        this.isDebuggerContext = isDebuggerContext;
        this.collectAllCandidates = collectAllCandidates;
        this.callPosition = callPosition;
        this.expressionContextProvider = expressionContextProvider;

        this.dataFlowValueFactory = factory;
        this.inferenceSession = inferenceSession;

    this.languageVersionSettings = languageVersionSettings;




    }
    @NotNull
    public Context replaceTraceAndCache(@NotNull TemporaryTraceAndCache traceAndCache) {
        return replaceBindingTrace(traceAndCache.trace).replaceResolutionResultsCache(traceAndCache.cache);
    }
    @NotNull
    public Context replaceResolutionResultsCache(@NotNull ResolutionResultsCache newResolutionResultsCache) {
        if (newResolutionResultsCache == resolutionResultsCache) return self();
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, newResolutionResultsCache, statementFilter,
                collectAllCandidates, callPosition, expressionContextProvider , languageVersionSettings, dataFlowValueFactory,
                inferenceSession);
    }

    @NotNull
    public Context replaceStatementFilter(@NotNull StatementFilter statementFilter) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, callPosition, expressionContextProvider,languageVersionSettings, dataFlowValueFactory,
                inferenceSession);
    }

    protected abstract Context create(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull CangJieType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull StatementFilter statementFilter,
            boolean collectAllCandidates,
            @NotNull CallPosition callPosition,
            @NotNull Function1<CjExpression, CjExpression> expressionContextProvider,
            @NotNull LanguageVersionSettings languageVersionSettings,

            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    );

    @NotNull
    @SuppressWarnings("unchecked")
    private Context self() {
        return (Context) this;
    }

    @NotNull
    public Context replaceBindingTrace(@NotNull BindingTrace trace) {
//        throw new UnsupportedOperationException("replaceBindingTrace is not implemented");
        if (this.trace == trace) return self();
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, callPosition, expressionContextProvider,languageVersionSettings,  dataFlowValueFactory,
                inferenceSession);
    }

    @NotNull
    public Context replaceDataFlowInfo(@NotNull DataFlowInfo newDataFlowInfo) {
//        throw new UnsupportedOperationException("replaceBindingTrace is not implemented");

        if (newDataFlowInfo == dataFlowInfo) return self();
        return create(trace, scope, newDataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, callPosition, expressionContextProvider,languageVersionSettings,  dataFlowValueFactory,
                inferenceSession);
    }

    @NotNull
    public Context replaceInferenceSession(@NotNull InferenceSession newInferenceSession) {
//        throw new UnsupportedOperationException("replaceBindingTrace is not implemented");

        if (newInferenceSession == inferenceSession) return self();
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, callPosition, expressionContextProvider,languageVersionSettings,  dataFlowValueFactory,
                newInferenceSession);
    }

    @NotNull
    public Context replaceExpectedType(@Nullable CangJieType newExpectedType) {

        if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedType == newExpectedType) return self();
        return create(trace, scope, dataFlowInfo, newExpectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, callPosition, expressionContextProvider,languageVersionSettings,  dataFlowValueFactory,
                inferenceSession);
    }

    @NotNull
    public Context replaceScope(@NotNull LexicalScope newScope) {
//        throw new UnsupportedOperationException("replaceBindingTrace is not implemented");
//
        if (newScope == scope) return self();
        return create(trace, newScope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, callPosition, expressionContextProvider,languageVersionSettings,  dataFlowValueFactory,
                inferenceSession);
    }

//    @NotNull
//    public Context replaceContextDependency(@NotNull ContextDependency newContextDependency) {
//        if (newContextDependency == contextDependency) return self();
//        return create(trace, scope, dataFlowInfo, expectedType, newContextDependency, resolutionResultsCache, statementFilter,
//                collectAllCandidates, callPosition, expressionContextProvider, dataFlowValueFactory,
//                inferenceSession);
//    }

//    @NotNull
//    public Context replaceResolutionResultsCache(@NotNull ResolutionResultsCache newResolutionResultsCache) {
//        if (newResolutionResultsCache == resolutionResultsCache) return self();
//        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, newResolutionResultsCache, statementFilter,
//                collectAllCandidates, callPosition, expressionContextProvider, dataFlowValueFactory,
//                inferenceSession);
//    }
//
//    @NotNull
//    public Context replaceTraceAndCache(@NotNull TemporaryTraceAndCache traceAndCache) {
//        return replaceBindingTrace(traceAndCache.trace).replaceResolutionResultsCache(traceAndCache.cache);
//    }
//
//    @NotNull
//    public Context replaceCollectAllCandidates(boolean newCollectAllCandidates) {
//        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
//                newCollectAllCandidates, callPosition, expressionContextProvider, dataFlowValueFactory,
//                inferenceSession);
//    }
//
//    @NotNull
//    public Context replaceStatementFilter(@NotNull StatementFilter statementFilter) {
//        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
//                collectAllCandidates, callPosition, expressionContextProvider, dataFlowValueFactory,
//                inferenceSession);
//    }
//
//    @NotNull
//    public Context replaceCallPosition(@NotNull CallPosition callPosition) {
//        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
//                collectAllCandidates, callPosition, expressionContextProvider, dataFlowValueFactory,
//                inferenceSession);
//    }
//
//    @NotNull
//    public Context replaceExpressionContextProvider(@NotNull Function1<CjExpression, CjExpression> expressionContextProvider) {
//        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
//                collectAllCandidates, callPosition, expressionContextProvider, dataFlowValueFactory,
//                inferenceSession);
//    }
//@NotNull
//public Context replaceExpectedType(@Nullable CangJieType newExpectedType) {
//    if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
//    if (expectedType == newExpectedType) return self();
//    return create(trace, scope, dataFlowInfo, newExpectedType, contextDependency, resolutionResultsCache, statementFilter,
//            collectAllCandidates, callPosition, expressionContextProvider, dataFlowValueFactory,
//            inferenceSession);
//}
    @Nullable
    @SafeVarargs
    @SuppressWarnings("unchecked")
    public final <T extends PsiElement> T getContextParentOfType(@NotNull CjExpression expression, @NotNull Class<? extends T>... classes) {
        CjExpression context = expressionContextProvider.invoke(expression);
        PsiElement current = context != null ? context : expression.getParent();

        while (current != null) {
            for (Class<? extends T> klass : classes) {
                if (klass.isInstance(current)) {
                    return (T) current;
                }
            }

            if (current instanceof PsiFile) return null;

            if (current instanceof CjExpression) {
                context = expressionContextProvider.invoke((CjExpression) current);
                if (context != null) {
                    current = context;
                    continue;
                }
            }

            current = current.getParent();
        }
        return null;
    }
}
