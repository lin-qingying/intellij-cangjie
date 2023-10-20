package com.huawei.cangjie.psi;

import org.jetbrains.annotations.Nullable;


public interface CjCallElement extends CjElement {
    @Nullable
    CjExpression getCalleeExpression();


}
