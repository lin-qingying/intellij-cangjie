package com.huawei.cangjie.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public interface CjCallElement extends CjElement {
    @Nullable
    CjExpression getCalleeExpression();

    @Nullable
    CjValueArgumentList getValueArgumentList();

    @NotNull
    List<? extends ValueArgument> getValueArguments();

    @NotNull
    List<CjLambdaArgument> getLambdaArguments();

    @NotNull
    List<CjTypeProjection> getTypeArguments();

    @Nullable
    CjTypeArgumentList getTypeArgumentList();
}
