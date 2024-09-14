package com.huawei.cangjie.resolve.calls.context;


import com.huawei.cangjie.config.LanguageVersionSettings;
import com.huawei.cangjie.descriptors.BindingTrace;
import com.huawei.cangjie.psi.Call;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.resolve.StatementFilter;
import com.huawei.cangjie.resolve.calls.components.InferenceSession;
import com.huawei.cangjie.resolve.calls.model.DataFlowInfoForArgumentsImpl;
import com.huawei.cangjie.resolve.calls.model.MutableDataFlowInfoForArguments;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowInfo;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.huawei.cangjie.resolve.scopes.LexicalScope;
import com.huawei.cangjie.types.CangJieType;
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
            @NotNull InferenceSession inferenceSession
    ) {
        super(trace, scope, expectedType, dataFlowInfo, contextDependency, resolutionResultsCache,
                statementFilter, isAnnotationContext, isDebuggerContext, collectAllCandidates,isSaveTypeInfo, callPosition, expressionContextProvider,
                languageVersionSettings,
                dataFlowValueFactory, inferenceSession);
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
