package com.linqingying.cangjie.types.expressions;

import com.linqingying.cangjie.psi.*;
import com.linqingying.cangjie.resolve.scopes.LexicalWritableScope;
import com.linqingying.cangjie.resolve.scopes.receivers.ReceiverValue;
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo;
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
          @Nullable  CjExpression initializer,
            ExpressionTypingContext context);

    void checkLetExpression(@NotNull CjLetExpression pattern, ExpressionTypingContext context);

    @NotNull
    ExpressionTypingComponents getComponents();
}
