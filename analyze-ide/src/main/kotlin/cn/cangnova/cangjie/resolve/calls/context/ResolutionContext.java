/*
 * Copyright 2024 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package cn.cangnova.cangjie.resolve.calls.context;


import cn.cangnova.cangjie.config.LanguageVersionSettings;
import cn.cangnova.cangjie.descriptors.BindingTrace;
import cn.cangnova.cangjie.psi.CjExpression;
import cn.cangnova.cangjie.resolve.StatementFilter;
import cn.cangnova.cangjie.resolve.calls.components.InferenceSession;
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import cn.cangnova.cangjie.resolve.scopes.LexicalScope;
import cn.cangnova.cangjie.types.CangJieType;
import cn.cangnova.cangjie.types.expressions.ContextConfig;
import cn.cangnova.cangjie.types.expressions.ProcessingMode;
import cn.cangnova.cangjie.types.util.TypeUtils;
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
    public   LexicalScope scope;
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
    public final boolean isSaveTypeInfo;
    @NotNull
    public final LanguageVersionSettings languageVersionSettings;
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
    // 新增枚举字段，默认为 NORMAL
    @NotNull
    public final ContextConfig config;


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
            boolean isSaveTypeInfo,
            @NotNull CallPosition callPosition,
            @NotNull Function1<CjExpression, CjExpression> expressionContextProvider,
            @NotNull LanguageVersionSettings languageVersionSettings,

            @NotNull DataFlowValueFactory factory,
            @NotNull InferenceSession inferenceSession,
            @NotNull ContextConfig config

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
        this.isSaveTypeInfo = isSaveTypeInfo;
        this.callPosition = callPosition;
        this.expressionContextProvider = expressionContextProvider;

        this.dataFlowValueFactory = factory;
        this.inferenceSession = inferenceSession;

        this.languageVersionSettings = languageVersionSettings;

        this.config = config;

    }

    public void setScope(LexicalScope scope) {
        this.scope = scope;
    }

    @NotNull
    public Context replaceExpressionContextProvider(@NotNull Function1<CjExpression, CjExpression> expressionContextProvider) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
    }

    @NotNull
    public Context replaceContextDependency(@NotNull ContextDependency newContextDependency) {
        if (newContextDependency == contextDependency) return self();
        return create(trace, scope, dataFlowInfo, expectedType, newContextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
    }


    @NotNull
    public Context replaceTraceAndCache(@NotNull TemporaryTraceAndCache traceAndCache) {
        return replaceBindingTrace(traceAndCache.trace).replaceResolutionResultsCache(traceAndCache.cache);
    }

    @NotNull
    public Context replaceResolutionResultsCache(@NotNull ResolutionResultsCache newResolutionResultsCache) {
        if (newResolutionResultsCache == resolutionResultsCache) return self();
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, newResolutionResultsCache, statementFilter,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
    }

    @NotNull
    public Context replaceStatementFilter(@NotNull StatementFilter statementFilter) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
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
            boolean isSaveTypeInfo,
            @NotNull CallPosition callPosition,
            @NotNull Function1<CjExpression, CjExpression> expressionContextProvider,
            @NotNull LanguageVersionSettings languageVersionSettings,

            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession,
            @NotNull ContextConfig config
    );

    @NotNull
    public Context replaceCollectAllCandidates(boolean newCollectAllCandidates) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                newCollectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
    }

    @NotNull
    public Context replaceProcessingMode(@NotNull ProcessingMode processingMode) {


        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config.copy(processingMode));
    }

    @NotNull
    public Context replaceCallPosition(@NotNull CallPosition callPosition) {
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
    }

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
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
    }

    @NotNull
    public Context replaceDataFlowInfo(@NotNull DataFlowInfo newDataFlowInfo) {
//        throw new UnsupportedOperationException("replaceBindingTrace is not implemented");

        if (newDataFlowInfo == dataFlowInfo) return self();
        return create(trace, scope, newDataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
    }

    @NotNull
    public Context replaceInferenceSession(@NotNull InferenceSession newInferenceSession) {
//        throw new UnsupportedOperationException("replaceBindingTrace is not implemented");

        if (newInferenceSession == inferenceSession) return self();
        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                newInferenceSession, config);
    }

    @NotNull
    public Context replaceIsSaveTypeInfo(boolean isSaveTypeInfo) {


        return create(trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
    }

    @NotNull
    public Context replaceExpectedType(@Nullable CangJieType newExpectedType) {

        if (newExpectedType == null) return replaceExpectedType(TypeUtils.NO_EXPECTED_TYPE);
        if (expectedType == newExpectedType) return self();
        return create(trace, scope, dataFlowInfo, newExpectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
    }

    @NotNull
    public Context replaceScope(@NotNull LexicalScope newScope) {

        if (newScope == scope) return self();
        return create(trace, newScope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache, statementFilter,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, config);
    }


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
