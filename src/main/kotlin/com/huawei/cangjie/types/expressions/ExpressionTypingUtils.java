package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.impl.AnonymousFunctionDescriptor;
import com.huawei.cangjie.descriptors.impl.FunctionExpressionDescriptor;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.psi.CjUnaryExpression;
import com.huawei.cangjie.resolve.OverloadChecker;
import com.huawei.cangjie.resolve.scopes.LexicalScopeKind;
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope;
import com.huawei.cangjie.resolve.scopes.TraceBasedLocalRedeclarationChecker;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class ExpressionTypingUtils {

    public static boolean isExclExclExpression(@Nullable CjExpression expression) {
        return expression instanceof CjUnaryExpression;
//                && ((CjUnaryExpression) expression).getOperationReference().getReferencedNameElementType() == CjTokens.EXCLEXCL;
    }
    @NotNull
    public static LexicalWritableScope newWritableScopeImpl(
            @NotNull ExpressionTypingContext context,
            @NotNull LexicalScopeKind scopeKind,
            @NotNull OverloadChecker overloadChecker
    ) {
        return new LexicalWritableScope(context.scope, context.scope.getOwnerDescriptor(), false,
                new TraceBasedLocalRedeclarationChecker(context.trace, overloadChecker), scopeKind);
    }

    public static boolean isFunctionExpression(@Nullable DeclarationDescriptor descriptor) {
        return descriptor instanceof FunctionExpressionDescriptor;
    }
    public static boolean isFunctionLiteral(@Nullable DeclarationDescriptor descriptor) {
        return descriptor instanceof AnonymousFunctionDescriptor;
    }
}
