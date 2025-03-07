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

package cn.cangnova.cangjie.types.expressions;

import cn.cangnova.cangjie.config.LanguageVersionSettings;
import cn.cangnova.cangjie.descriptors.BindingTrace;
import cn.cangnova.cangjie.psi.CjExpression;
import cn.cangnova.cangjie.resolve.StatementFilter;
import cn.cangnova.cangjie.resolve.calls.components.InferenceSession;
import cn.cangnova.cangjie.resolve.calls.context.*;
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import cn.cangnova.cangjie.resolve.scopes.LexicalScope;
import cn.cangnova.cangjie.types.CangJieType;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExpressionTypingContext extends ResolutionContext<ExpressionTypingContext> {

    private ExpressionTypingContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull CangJieType expectedType,
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
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession,
            @NotNull ContextConfig config

    ) {
        super(trace, scope, expectedType, dataFlowInfo, contextDependency, resolutionResultsCache,
                statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider,
                languageVersionSettings, dataFlowValueFactory, inferenceSession, config);
    }



    //    protected ExpressionTypingContext(@NotNull BindingTrace trace, @NotNull LexicalScope scope, @NotNull CangJieType expectedType, @NotNull DataFlowInfo dataFlowInfo, bool isAnnotationContext, bool isDebuggerContext, bool collectAllCandidates, @NotNull Function1<CjExpression, CjExpression> expressionContextProvider, @NotNull DataFlowValueFactory factory, @NotNull InferenceSession inferenceSession) {
//        super(trace, scope, expectedType, dataFlowInfo, isAnnotationContext, isDebuggerContext, collectAllCandidates, expressionContextProvider, factory, inferenceSession);
//    }
    @NotNull
    public static ExpressionTypingContext newContext(@NotNull ResolutionContext<?> context) {
        return new ExpressionTypingContext(
                context.trace, context.scope, context.dataFlowInfo, context.expectedType,
                context.contextDependency, context.resolutionResultsCache,
                context.statementFilter,
                context.isAnnotationContext, context.isDebuggerContext, context.collectAllCandidates,
                context.isSaveTypeInfo,
                context.callPosition, context.expressionContextProvider,
                context.languageVersionSettings,
                context.dataFlowValueFactory, context.inferenceSession,
                context.config
        );
    }



    @NotNull
    public static ExpressionTypingContext newContext(@NotNull ResolutionContext<?> context, boolean isDebuggerContext) {
        return new ExpressionTypingContext(
                context.trace, context.scope, context.dataFlowInfo, context.expectedType,
                context.contextDependency, context.resolutionResultsCache,
                context.statementFilter,
                context.isAnnotationContext, isDebuggerContext, context.collectAllCandidates, context.isSaveTypeInfo,
                context.callPosition, context.expressionContextProvider,
                context.languageVersionSettings,
                context.dataFlowValueFactory, context.inferenceSession, context.config);
    }

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull CangJieType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull StatementFilter statementFilter,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession
    ) {
        return newContext(trace, scope, dataFlowInfo, expectedType, contextDependency,
                new ResolutionResultsCacheImpl(), statementFilter, false, languageVersionSettings, dataFlowValueFactory,
                inferenceSession);
    }

    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull CangJieType expectedType,
            @NotNull ContextDependency contextDependency,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @NotNull StatementFilter statementFilter,
            boolean isAnnotationContext,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @NotNull InferenceSession inferenceSession


    ) {
        return new ExpressionTypingContext(
                trace, scope, dataFlowInfo, expectedType, contextDependency, resolutionResultsCache,
                statementFilter, isAnnotationContext, false, false, true,
                CallPosition.Unknown.INSTANCE, DEFAULT_EXPRESSION_CONTEXT_PROVIDER, languageVersionSettings, dataFlowValueFactory,
                inferenceSession, ContextConfig.DEFAULT);
    }
    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull CangJieType expectedType,
            @NotNull LanguageVersionSettings languageVersionSettings,
            @NotNull DataFlowValueFactory dataFlowValueFactory
    ) {
        return newContext(trace, scope, dataFlowInfo, expectedType, ContextDependency.INDEPENDENT, StatementFilter.NONE,
                languageVersionSettings, dataFlowValueFactory, InferenceSession.Companion.getDefault());
    }
    @NotNull
    public static ExpressionTypingContext newContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull CangJieType expectedType,
            @NotNull LanguageVersionSettings languageVersionSettings,

            @NotNull DataFlowValueFactory dataFlowValueFactory,
            @Nullable InferenceSession inferenceSession
    ) {
        return newContext(
                trace, scope, dataFlowInfo, expectedType, ContextDependency.INDEPENDENT, StatementFilter.NONE, languageVersionSettings,
                dataFlowValueFactory, inferenceSession != null ? inferenceSession : InferenceSession.Companion.getDefault()
        );
    }


    @Override
    protected ExpressionTypingContext create(@NotNull BindingTrace trace,
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
        return new ExpressionTypingContext(trace, scope, dataFlowInfo,
                expectedType, contextDependency, resolutionResultsCache,
                statementFilter, isAnnotationContext, isDebuggerContext,
                collectAllCandidates, isSaveTypeInfo, callPosition, expressionContextProvider, languageVersionSettings,
                dataFlowValueFactory, inferenceSession, config);
    }
}
