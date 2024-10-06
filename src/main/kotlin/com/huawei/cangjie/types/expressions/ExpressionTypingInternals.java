package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope;
import com.huawei.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/*package*/ public interface ExpressionTypingInternals extends ExpressionTypingFacade {
    @NotNull
    CangJieTypeInfo checkInExpression(
            @NotNull CjElement callElement,
            @NotNull CjSimpleNameExpression operationSign,
            @NotNull ValueArgument leftArgument,
            @Nullable CjExpression right,
            @NotNull ExpressionTypingContext context
    );

    void checkStatementType(@NotNull CjExpression expression, ExpressionTypingContext context);


    void defineLocalVariablesFromPattern(
            LexicalWritableScope writableScope,
            CjCasePattern casePattern,
            ReceiverValue receiver,
            CjExpression initializer,
            ExpressionTypingContext context);

    void checkLetExpression(@NotNull CjLetExpression pattern, ExpressionTypingContext context);

    @NotNull
    ExpressionTypingComponents getComponents();
}
