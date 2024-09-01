package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.impl.AnonymousFunctionDescriptor;
import com.huawei.cangjie.descriptors.impl.FunctionExpressionDescriptor;
import com.huawei.cangjie.psi.CjDestructuringDeclarationEntry;
import com.huawei.cangjie.psi.CjExpression;
import com.huawei.cangjie.psi.CjParameter;
import com.huawei.cangjie.psi.CjUnaryExpression;
import com.huawei.cangjie.resolve.DescriptorToSourceUtils;
import com.huawei.cangjie.resolve.OverloadChecker;
import com.huawei.cangjie.resolve.scopes.*;
import com.intellij.psi.PsiElement;
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

    /**
     * The primary case for local extensions is the following:
     *
     * I had a locally declared extension function or a local variable of function type called foo
     * And I called it on my x
     * Now, someone added function foo() to the class of x
     * My code should not change
     *
     * thus
     *
     * local extension prevail over members (and members prevail over all non-local extensions)
     */
    public static boolean isLocal(DeclarationDescriptor containerOfTheCurrentLocality, DeclarationDescriptor candidate) {
        if (candidate instanceof ValueParameterDescriptor) {
            return true;
        }
        DeclarationDescriptor parent = candidate.getContainingDeclaration();
        if (!(parent instanceof FunctionDescriptor)) {
            return false;
        }
        FunctionDescriptor functionDescriptor = (FunctionDescriptor) parent;
        DeclarationDescriptor current = containerOfTheCurrentLocality;
        while (current != null) {
            if (current == functionDescriptor) {
                return true;
            }
            current = current.getContainingDeclaration();
        }
        return false;
    }

    @SuppressWarnings("deprecation")
    public static void checkVariableShadowing(
            @NotNull LexicalScope scope,
            @NotNull BindingTrace trace,
            @NotNull VariableDescriptorBase variableDescriptor
    ) {
        VariableDescriptorBase oldDescriptor = ScopeUtilsKt.findLocalVariable(scope, variableDescriptor.getName());
        if (oldDescriptor == null) return;

        DeclarationDescriptor variableContainingDeclaration = variableDescriptor.getContainingDeclaration();
        if (!isLocal(variableContainingDeclaration, oldDescriptor)) return;

        if (variableDescriptor instanceof ParameterDescriptor) {
            if (!isFunctionLiteral(variableContainingDeclaration)) {
                return;
            }

            // parameter of lambda
            if (variableContainingDeclaration.getContainingDeclaration() != oldDescriptor.getContainingDeclaration()) {
                return;
            }
        }

        PsiElement declaration = DescriptorToSourceUtils.descriptorToDeclaration(variableDescriptor);
        if (declaration != null) {
            if (declaration instanceof CjDestructuringDeclarationEntry && declaration.getParent().getParent() instanceof CjParameter) {
                // foo { a, (a, b) -> } -- do not report NAME_SHADOWING on the second 'a', because REDECLARATION must be reported here
                PsiElement oldElement = DescriptorToSourceUtils.descriptorToDeclaration(oldDescriptor);

                if (oldElement != null && oldElement.getParent().equals(declaration.getParent().getParent().getParent())) return;
            }
            trace.report(Errors.NAME_SHADOWING.on(declaration, variableDescriptor.getName().asString()));
        }
    }
    public static boolean isFunctionExpression(@Nullable DeclarationDescriptor descriptor) {
        return descriptor instanceof FunctionExpressionDescriptor;
    }
    public static boolean isFunctionLiteral(@Nullable DeclarationDescriptor descriptor) {
        return descriptor instanceof AnonymousFunctionDescriptor;
    }
}
