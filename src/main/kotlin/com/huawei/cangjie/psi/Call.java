package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;

import com.huawei.cangjie.utils.ReadOnly;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;
public interface Call {

    // SAFE_ACCESS or DOT or so
    @Nullable
    ASTNode getCallOperationNode();

    default boolean isSemanticallyEquivalentToSafeCall() {
        return getCallOperationNode() != null && getCallOperationNode().getElementType() == CjTokens.SAFE_ACCESS;
    }


    @Nullable
    CjExpression getCalleeExpression();

    @Nullable
    CjValueArgumentList getValueArgumentList();

    @ReadOnly
    @NotNull
    List<? extends ValueArgument> getValueArguments();

    @ReadOnly
    @NotNull
    List<? extends LambdaArgument> getFunctionLiteralArguments();

    @ReadOnly
    @NotNull
    List<CjTypeProjection> getTypeArguments();

    @Nullable
    CjTypeArgumentList getTypeArgumentList();

    @NotNull
    CjElement getCallElement();

    enum CallType {
        DEFAULT, ARRAY_GET_METHOD, ARRAY_SET_METHOD, INVOKE, CONTAINS
    }

    @NotNull
    Call.CallType getCallType();
}
