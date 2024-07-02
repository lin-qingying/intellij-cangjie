package com.huawei.cangjie.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public interface CjCallElement extends CjElement {
    @Nullable
    CjExpression getCalleeExpression();
    @NotNull
    List<CjLambdaArgument> getLambdaArguments();
    @Nullable
    CjTypeArgumentList getTypeArgumentList();
    @Nullable
   CjValueArgumentList getValueArgumentList();
}
