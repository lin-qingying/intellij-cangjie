package com.linqingying.cangjie.types.expressions;

import com.linqingying.cangjie.psi.CjExpression;
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo;
import org.jetbrains.annotations.NotNull;

public interface ExpressionTypingFacade {
    @NotNull
    CangJieTypeInfo safeGetTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context);

    @NotNull
    CangJieTypeInfo getTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context);

    @NotNull
    CangJieTypeInfo getTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context, boolean isStatement);
}
