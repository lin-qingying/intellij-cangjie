package com.linqingying.cangjie.psi;

import org.jetbrains.annotations.NotNull;


public interface CjOperationExpression extends CjExpression {
    @NotNull
    CjSimpleNameExpression getOperationReference();
}
