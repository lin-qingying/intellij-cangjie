package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.psi.CjElement;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.psi.CjSimpleNameExpression;
import com.huawei.cangjie.psi.ValueArgument;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*package*/ interface ExpressionTypingInternals extends ExpressionTypingFacade {
    @NotNull
    CangJieTypeInfo checkInExpression(
            @NotNull CjElement callElement,
            @NotNull CjSimpleNameExpression operationSign,
            @NotNull ValueArgument leftArgument,
            @Nullable CjExpression right,
            @NotNull ExpressionTypingContext context
    );

    void checkStatementType(@NotNull CjExpression expression, ExpressionTypingContext context);

    @NotNull
    ExpressionTypingComponents getComponents();
}
