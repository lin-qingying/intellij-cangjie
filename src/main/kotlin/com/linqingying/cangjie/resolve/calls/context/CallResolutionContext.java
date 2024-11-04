package com.linqingying.cangjie.resolve.calls.context;


import com.linqingying.cangjie.config.LanguageVersionSettings;
import com.linqingying.cangjie.descriptors.BindingTrace;
import com.linqingying.cangjie.psi.Call;
import com.linqingying.cangjie.psi.CjExpression;
import com.linqingying.cangjie.resolve.StatementFilter;
import com.linqingying.cangjie.resolve.calls.components.InferenceSession;
import com.linqingying.cangjie.resolve.calls.model.DataFlowInfoForArgumentsImpl;
import com.linqingying.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.linqingying.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.linqingying.cangjie.resolve.scopes.LexicalScope;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.expressions.ContextConfig;
import com.linqingying.cangjie.types.expressions.ProcessingMode;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public abstract class CallResolutionContext<Context extends CallResolutionContext<Context>> extends ResolutionContext<Context> {
    @NotNull
    public final Call call;
    @NotNull
    public final CheckArgumentTypesMode checkArguments;
    @NotNull
    public final MutableDataFlowInfoForArguments dataFlowInfoForArguments;

    protected CallResolutionContext(
            @NotNull BindingTrace trace,
            @NotNull LexicalScope scope,
            @NotNull Call call,
            @NotNull CangJieType expectedType,
            @NotNull DataFlowInfo dataFlowInfo,
            @NotNull ContextDependency contextDependency,
            @NotNull CheckArgumentTypesMode checkArguments,
            @NotNull ResolutionResultsCache resolutionResultsCache,
            @SuppressWarnings("NullableProblems")
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
        super(trace, scope, expectedType, dataFlowInfo, contextDependency, resolutionResultsCache,
                statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,isSaveTypeInfo, callPosition, expressionContextProvider,
                languageVersionSettings,
                dataFlowValueFactory, inferenceSession,config);
        this.call = call;
        this.checkArguments = checkArguments;
        if (dataFlowInfoForArguments != null) {
            this.dataFlowInfoForArguments = dataFlowInfoForArguments;
        }
        else if (checkArguments == CheckArgumentTypesMode.CHECK_VALUE_ARGUMENTS) {
            this.dataFlowInfoForArguments = new DataFlowInfoForArgumentsImpl(dataFlowInfo, call);
        }
        else {
            this.dataFlowInfoForArguments = new MutableDataFlowInfoForArguments.WithoutArgumentsCheck(dataFlowInfo);
        }
    }
}
