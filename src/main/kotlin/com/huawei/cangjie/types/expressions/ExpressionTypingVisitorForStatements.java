package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.psi.CjBlockExpression;
import com.huawei.cangjie.psi.CjElement;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.resolve.scopes.LexicalWritableScope;
import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import org.jetbrains.annotations.NotNull;

import static com.huawei.cangjie.descriptors.Errors.UNSUPPORTED;

@SuppressWarnings("SuspiciousMethodCalls")
public class ExpressionTypingVisitorForStatements extends ExpressionTypingVisitor{
    private final LexicalWritableScope scope;
//    private final BasicExpressionTypingVisitor basic;
//    private final ControlStructureTypingVisitor controlStructures;
//    private final PatternMatchingTypingVisitor patterns;
//    private final FunctionsTypingVisitor functions;

    public ExpressionTypingVisitorForStatements(
            @NotNull ExpressionTypingInternals facade,
            @NotNull LexicalWritableScope scope
//            @NotNull BasicExpressionTypingVisitor basic,
//            @NotNull ControlStructureTypingVisitor controlStructures,
//            @NotNull PatternMatchingTypingVisitor patterns,
//            @NotNull FunctionsTypingVisitor functions
    ) {
        super(facade);
        this.scope = scope;
//        this.basic = basic;
//        this.controlStructures = controlStructures;
//        this.patterns = patterns;
//        this.functions = functions;
    }
    @Override
    public CangJieTypeInfo visitBlockExpression(@NotNull CjBlockExpression expression, ExpressionTypingContext context) {
        return components.expressionTypingServices.getBlockReturnedType(expression, context, true);
    }

    @Override
    public CangJieTypeInfo visitCjElement(@NotNull CjElement element, ExpressionTypingContext context) {
        context.trace.report(UNSUPPORTED.on(element, "in a block"));
        return TypeInfoFactoryKt.noTypeInfo(context);
    }

    @Override
    public CangJieTypeInfo visitExpression(@NotNull CjExpression expression, ExpressionTypingContext context) {
        return facade.getTypeInfo(expression, context);
    }
}