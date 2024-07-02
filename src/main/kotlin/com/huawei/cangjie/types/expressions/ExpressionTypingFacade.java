package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import org.jetbrains.annotations.NotNull;

public interface ExpressionTypingFacade {
    @NotNull
    CangJieTypeInfo safeGetTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context);

    @NotNull
    CangJieTypeInfo getTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context);

    @NotNull
    CangJieTypeInfo getTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context, boolean isStatement);
}
