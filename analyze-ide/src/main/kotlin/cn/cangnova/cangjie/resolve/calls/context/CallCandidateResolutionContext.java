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
import cn.cangnova.cangjie.descriptors.CallableDescriptor;
import cn.cangnova.cangjie.psi.Call;
import cn.cangnova.cangjie.psi.CjExpression;
import cn.cangnova.cangjie.resolve.StatementFilter;
import cn.cangnova.cangjie.resolve.calls.components.InferenceSession;
import cn.cangnova.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments;
import cn.cangnova.cangjie.resolve.calls.model.MutableResolvedCall;
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import cn.cangnova.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import cn.cangnova.cangjie.resolve.calls.tasks.TracingStrategy;
import cn.cangnova.cangjie.resolve.scopes.LexicalScope;
import cn.cangnova.cangjie.types.CangJieType;
import cn.cangnova.cangjie.types.expressions.ContextConfig;
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
