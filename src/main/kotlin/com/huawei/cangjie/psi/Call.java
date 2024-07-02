package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.resolve.scopes.receivers.Receiver;
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.huawei.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface Call {
    enum CallType {
        DEFAULT, ARRAY_GET_METHOD, ARRAY_SET_METHOD, INVOKE, CONTAINS
    }
    default boolean isSemanticallyEquivalentToSafeCall() {
        return true;
//        return getCallOperationNode() != null && getCallOperationNode().getElementType() == CjTokens.SAFE_ACCESS;
    }
    @Nullable
    ReceiverValue getDispatchReceiver();
    @ReadOnly
    @NotNull
    List<? extends ValueArgument> getValueArguments();
    @Nullable
    Receiver getExplicitReceiver();
    @ReadOnly
    @NotNull
    List<? extends LambdaArgument> getFunctionLiteralArguments();

    @ReadOnly
    @NotNull
    List<CjTypeProjection> getTypeArguments();
    @NotNull
    CjElement getCallElement();
    @Nullable
    CjExpression getCalleeExpression();
    @NotNull
    Call.CallType getCallType();
}
