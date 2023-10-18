package com.huawei.cangjie1.psi;

import org.jetbrains.annotations.NotNull;


public interface CjOperationExpression extends CjExpression {
    @NotNull
    CjSimpleNameExpression getOperationReference();
}
