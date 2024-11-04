package com.linqingying.cangjie.types.expressions;

import com.linqingying.cangjie.psi.CjVisitor;
import com.linqingying.cangjie.utils.exceptions.CangJieTypeInfo;
import org.jetbrains.annotations.NotNull;

public abstract class ExpressionTypingVisitor extends CjVisitor<CangJieTypeInfo, ExpressionTypingContext> {

    protected final ExpressionTypingInternals facade;
    protected final ExpressionTypingComponents components;

    protected ExpressionTypingVisitor(@NotNull ExpressionTypingInternals facade) {
        this.facade = facade;
        this.components = facade.getComponents();
    }
}
