package com.huawei.cangjie.types.expressions;

import com.huawei.cangjie.descriptors.*;
import com.huawei.cangjie.descriptors.impl.AnonymousFunctionDescriptor;
import com.huawei.cangjie.descriptors.impl.FunctionExpressionDescriptor;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.*;
import com.huawei.cangjie.resolve.DescriptorToSourceUtils;
import com.huawei.cangjie.resolve.OverloadChecker;
import com.huawei.cangjie.resolve.scopes.*;
import com.huawei.cangjie.resolve.scopes.receivers.ExpressionReceiver;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.expressions.typeInfoFactory.TypeInfoFactoryKt;
import com.huawei.cangjie.utils.exceptions.CangJieTypeInfo;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.huawei.cangjie.utils.exceptions.OperatorConventions;

import static com.huawei.cangjie.resolve.BindingContext.PROCESSED;

public class ExpressionTypingUtils {

    public static boolean isExclExclExpression(@Nullable CjExpression expression) {
        return expression instanceof CjUnaryExpression;
//                && ((CjUnaryExpression) expression).getOperationReference().getReferencedNameElementType() == CjTokens.EXCLEXCL;
    }
    @Nullable
    public static ExpressionReceiver getExpressionReceiver(
            @NotNull ExpressionTypingFacade facade,
            @NotNull CjExpression expression,
            ExpressionTypingContext context
    ) {
        CangJieType type = facade.getTypeInfo(expression, context).getType();
        if (type == null) return null;
        return ExpressionReceiver.Companion.create(expression, type, context.trace.getBindingContext());
    }

    public static CjExpression createFakeExpressionOfType(
            @NotNull Project project,
            @NotNull BindingTrace trace,
            @NotNull String argumentName,
            @NotNull CangJieType argumentType
    ) {
       CjExpression fakeExpression = new CjPsiFactory(project, false).createExpression(argumentName);
        trace.recordType(fakeExpression, argumentType);
        trace.record(PROCESSED, fakeExpression);
        return fakeExpression;
    }

    @NotNull
    public static CangJieTypeInfo getTypeInfoOrNullType(
            @Nullable CjExpression expression,
            @NotNull ExpressionTypingContext context,
            @NotNull ExpressionTypingInternals facade
    ) {
        return expression != null
                ? facade.getTypeInfo(expression, context)
                : TypeInfoFactoryKt.noTypeInfo(context);
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
    public static boolean dependsOnExpectedType(@Nullable CjExpression expression) {
        CjExpression expr = CjPsiUtil.deparenthesize(expression);
        if (expr == null) return false;

        if (expr instanceof CjBinaryExpressionWithTypeRHS) {
            return false;
        }
        if (expr instanceof CjBinaryExpression) {
            return isBinaryExpressionDependentOnExpectedType((CjBinaryExpression) expr);
        }
//        if (expr instanceof CjUnaryExpression) {
//            return isUnaryExpressionDependentOnExpectedType((CjUnaryExpression) expr);
//        }
        return true;
    }
    @SuppressWarnings("SuspiciousMethodCalls")
    public static boolean isBinaryExpressionDependentOnExpectedType(@NotNull CjBinaryExpression expression) {
        IElementType operationType = expression.getOperationReference().getReferencedNameElementType();
        return (operationType == CjTokens.IDENTIFIER || OperatorConventions.BINARY_OPERATION_NAMES.containsKey(operationType)
                || operationType == CjTokens.ELVIS);
    }
    @NotNull
    public static CangJieType safeGetType(@NotNull CangJieTypeInfo typeInfo) {
        CangJieType type = typeInfo.getType();
        assert type != null : "safeGetType should be invoked on safe KotlinTypeInfo; safeGetTypeInfo should return @NotNull type";
        return type;
    }
    @NotNull
    public static ExpressionReceiver safeGetExpressionReceiver(
            @NotNull ExpressionTypingFacade facade,
            @NotNull CjExpression expression,
            ExpressionTypingContext context
    ) {
        CangJieType type = safeGetType(facade.safeGetTypeInfo(expression, context));
        return ExpressionReceiver.Companion.create(expression, type, context.trace.getBindingContext());
    }
//    public static boolean isUnaryExpressionDependentOnExpectedType(@NotNull CjUnaryExpression expression) {
//        return expression.getOperationReference().getReferencedNameElementType() == CjTokens.EXCLEXCL;
//    }

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
            @NotNull VariableDescriptor variableDescriptor
    ) {
        VariableDescriptor oldDescriptor = ScopeUtilsKt.findLocalVariable(scope, variableDescriptor.getName());
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
