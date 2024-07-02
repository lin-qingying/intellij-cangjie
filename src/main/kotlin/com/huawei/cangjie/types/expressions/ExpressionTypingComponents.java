package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.resolve.OverloadChecker;
import com.huawei.cangjie.resolve.calls.CallExpressionResolver;
import com.huawei.cangjie.resolve.calls.model.CangJieCallComponents;
import com.huawei.cangjie.resolve.calls.smartcasts.DataFlowValueFactory;
import com.huawei.cangjie.resolve.constants.evaluate.ConstantExpressionEvaluator;
import com.huawei.cangjie.types.checker.NewCangJieTypeChecker;
import com.huawei.cangjie.builtins.CangJieBuiltIns;
import jakarta.inject.Inject;
import org.jetbrains.annotations.NotNull;

public class ExpressionTypingComponents {
    public CallExpressionResolver callExpressionResolver;
    public ConstantExpressionEvaluator constantExpressionEvaluator;


    public CangJieBuiltIns builtIns;
    public DataFlowValueFactory dataFlowValueFactory;

    public ExpressionTypingServices expressionTypingServices;
    public DataFlowAnalyzer dataFlowAnalyzer;

    public NewCangJieTypeChecker cangjieTypeChecker;
    public CangJieCallComponents callComponents;

    public OverloadChecker overloadChecker;
    @Inject
    public void setDataFlowAnalyzer(@NotNull DataFlowAnalyzer dataFlowAnalyzer) {
        this.dataFlowAnalyzer = dataFlowAnalyzer;
    }
    @Inject

    public void setCallExpressionResolver(CallExpressionResolver callExpressionResolver) {
        this.callExpressionResolver = callExpressionResolver;
    }
    @Inject
    public void setCangJieTypeChecker(@NotNull NewCangJieTypeChecker cangjieTypeChecker) {
        this.cangjieTypeChecker = cangjieTypeChecker;
    }

    @Inject
    public void setDataFlowValueFactory(@NotNull DataFlowValueFactory dataFlowValueFactory) {
        this.dataFlowValueFactory = dataFlowValueFactory;
    }
    @Inject
    public void setBuiltIns(@NotNull CangJieBuiltIns builtIns) {
        this.builtIns = builtIns;
    }
    @Inject
    public void setExpressionTypingServices(@NotNull ExpressionTypingServices expressionTypingServices) {
        this.expressionTypingServices = expressionTypingServices;
    }
    @Inject
    public void setOverloadChecker(OverloadChecker overloadChecker) {
        this.overloadChecker = overloadChecker;
    }
    @Inject
    public void setCallComponents(@NotNull CangJieCallComponents callComponents) {
        this.callComponents = callComponents;
    }

}
