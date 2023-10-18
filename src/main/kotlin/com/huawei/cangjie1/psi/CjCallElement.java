package com.huawei.cangjie1.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public interface CjCallElement extends CjElement {
    @Nullable
    CjExpression getCalleeExpression();


}
