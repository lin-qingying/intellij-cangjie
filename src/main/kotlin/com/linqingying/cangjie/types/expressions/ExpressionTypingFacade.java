package com.linqingying.cangjie.types.expressions;

import com.linqingying.cangjie.psi.CjExpression;
import com.linqingying.cangjie.psi.ValueArgument;
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo;
import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface ExpressionTypingFacade {
    @NotNull
    CangJieTypeInfo safeGetTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context);

    @NotNull
    CangJieTypeInfo getTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context);

    /**
     * 该方法只会解析枚举类型
     * @param expression
     * @param context
     * @return
     */
    @NotNull
    CangJieTypeInfo getTypeInfoByEnum(@NotNull CjExpression expression, ExpressionTypingContext context);

    /**
     * 该方法只会解析case枚举类型
     * @param expression
     * @param context
     * @param argument 枚举参数 ，用于多枚举情况
     * @return
     */
    @NotNull
    CangJieTypeInfo getTypeInfoByCaseEnum(@NotNull CjExpression expression, List<ValueArgument> argument , ExpressionTypingContext context);

    @NotNull
    CangJieTypeInfo getTypeInfo(@NotNull CjExpression expression, ExpressionTypingContext context, boolean isStatement);
}
