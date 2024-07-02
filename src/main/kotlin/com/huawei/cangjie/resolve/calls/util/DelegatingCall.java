package com.huawei.cangjie.resolve.calls.util;

import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.scopes.receivers.Receiver;
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.huawei.cangjie.utils.ReadOnly;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class DelegatingCall implements Call {

    private final Call delegate;

    public DelegatingCall(@NotNull Call delegate) {
        this.delegate = delegate;
    }

//    @Override
//    @Nullable
//    public ASTNode getCallOperationNode() {
//        return delegate.getCallOperationNode();
//    }
@Override
@Nullable
public Receiver getExplicitReceiver() {
    return delegate.getExplicitReceiver();
}


    @Nullable
    @Override
    public ReceiverValue getDispatchReceiver() {
        return delegate.getDispatchReceiver();
    }
//
    @Override
    @Nullable
    public CjExpression getCalleeExpression() {
        return delegate.getCalleeExpression();
    }


    //
//    @Override
//    @Nullable
//    public CjValueArgumentList getValueArgumentList() {
//        return delegate.getValueArgumentList();
//    }
//
    @Override
    @NotNull
    @ReadOnly
    public List<? extends ValueArgument> getValueArguments() {
        return delegate.getValueArguments();
    }



    @Override
    @NotNull
    public List<? extends LambdaArgument> getFunctionLiteralArguments() {
        return delegate.getFunctionLiteralArguments();
    }

    @Override
    @NotNull
    public List<CjTypeProjection> getTypeArguments() {
        return delegate.getTypeArguments();
    }
//
//    @Override
//    @Nullable
//    public CjTypeArgumentList getTypeArgumentList() {
//        return delegate.getTypeArgumentList();
//    }



    @NotNull
    @Override
    public CjElement getCallElement() {
        return delegate.getCallElement();
    }

    @NotNull
    @Override
    public CallType getCallType() {
        return delegate.getCallType();
    }

    @Override
    public String toString() {
        return "*" + delegate.toString();
    }
}
