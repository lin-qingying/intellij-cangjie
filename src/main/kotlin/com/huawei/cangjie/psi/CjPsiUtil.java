package com.huawei.cangjie.psi;

import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.name.SpecialNames;
import com.huawei.cangjie.parsing.CangJieExpressionParsing;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;
import  com.huawei.cangjie.lexer.CjTokens;
public class CjPsiUtil {

    public static boolean isAssignment(@NotNull PsiElement element) {
        return element instanceof CjBinaryExpression &&
                CjTokens.ALL_ASSIGNMENTS.contains(((CjBinaryExpression) element).getOperationToken());
    }
    public static boolean isLocal(@NotNull CjDeclaration declaration) {
        return getEnclosingElementForLocalDeclaration(declaration) != null;
    }
    @Nullable
    public static CjElement getEnclosingElementForLocalDeclaration(@NotNull CjDeclaration declaration) {
        return getEnclosingElementForLocalDeclaration(declaration, true);
    }
    @NotNull
    public static String unquoteIdentifierOrFieldReference(@NotNull String quoted) {
        if (quoted.indexOf('`') < 0) {
            return quoted;
        }

        if (quoted.startsWith("$")) {
            return "$" + unquoteIdentifier(quoted.substring(1));
        }
        else {
            return unquoteIdentifier(quoted);
        }
    }
    @Nullable
    public static CjClassOrStruct getClassIfParameterIsProperty(@NotNull CjParameter cjParameter) {
        if (cjParameter.hasValOrVar()) {
            PsiElement grandParent = null;
            if (cjParameter.getParent() != null) {
                grandParent = cjParameter.getParent().getParent();
            }
            if (grandParent instanceof CjPrimaryConstructor) {
                return ((CjPrimaryConstructor) grandParent).getContainingClassOrStruct();
            }
        }

        return null;
    }

    @NotNull
    public static Name safeName(@Nullable String name) {
        return name == null ? SpecialNames.NO_NAME_PROVIDED : Name.identifier(name);
    }
    @Nullable
    public static CjSimpleNameExpression getLastReference(@NotNull CjExpression importedReference) {
        CjElement selector = CjPsiUtilKt.getQualifiedElementSelector(importedReference);
        return selector instanceof CjSimpleNameExpression ? (CjSimpleNameExpression) selector : null;
    }
    private static boolean isNonLocalCallable(@Nullable CjDeclaration declaration) {
        if (declaration instanceof CjVariable) {
            return !((CjVariable) declaration).isLocal();
        }

        return false;
    }
    public static <D> void visitChildren(@NotNull CjElement element, @NotNull CjVisitor<Void, D> visitor, D data) {
        PsiElement child = element.getFirstChild();
        while (child != null) {
            if (child instanceof CjElement) {
                ((CjElement) child).accept(visitor, data);
            }
            child = child.getNextSibling();
        }
    }
    @Nullable
    private static IElementType getOperation(@NotNull CjExpression expression) {
        if (expression instanceof CjQualifiedExpression) {
            return ((CjQualifiedExpression) expression).getOperationSign();
        }
        else if (expression instanceof CjOperationExpression) {
            return ((CjOperationExpression) expression).getOperationReference().getReferencedNameElementType();
        }
        return null;
    }

    private static int getPriority(@NotNull CjExpression expression) {
        int maxPriority = CangJieExpressionParsing.Precedence.values().length + 1;


        if (
                expression instanceof CjQualifiedExpression ||
                expression instanceof CjCallExpression
                ) {
            return maxPriority - 1;
        }





        if (expression instanceof CjDeclaration || expression instanceof CjStatementExpression) {
            return 0;
        }

        IElementType operation = getOperation(expression);
        for (CangJieExpressionParsing.Precedence precedence : CangJieExpressionParsing.Precedence.values()) {
            if (precedence != CangJieExpressionParsing.Precedence.PREFIX && precedence != CangJieExpressionParsing.Precedence.POSTFIX &&
                    precedence.getOperations().contains(operation)) {
                return maxPriority - precedence.ordinal() - 1;
            }
        }

        return maxPriority;
    }
    public static boolean areParenthesesNecessary(
            @NotNull CjExpression innerExpression,
            @NotNull CjExpression currentInner,
            @NotNull CjElement parentElement
    ) {




        if (parentElement instanceof CjPackageDirective) return false;






        if (parentElement instanceof CjCallExpression parentCall && currentInner == ((CjCallExpression) parentElement).getCalleeExpression()) {
            CjExpression targetInnerExpression = innerExpression;
            if (targetInnerExpression instanceof CjDotQualifiedExpression) {
                CjExpression selector = ((CjDotQualifiedExpression) targetInnerExpression).getSelectorExpression();
                if (selector != null) {
                    targetInnerExpression = selector;
                }
            }
            if (targetInnerExpression instanceof CjSimpleNameExpression) return false;
            if (CjPsiUtilKt.getQualifiedExpressionForSelector(parentElement) != null) return true;

            return !(   targetInnerExpression instanceof CjCallExpression);
        }

        if (parentElement instanceof CjValueArgument) {
            // a(___, d > (e + f)) => a((b < c), d > (e + f)) to prevent parsing < c, d > as type argument list
            CjValueArgument nextArg = PsiTreeUtil.getNextSiblingOfType(parentElement, CjValueArgument.class);
            PsiElement nextExpression = nextArg != null ? nextArg.getArgumentExpression() : null;

        }

        IElementType innerOperation = getOperation(innerExpression);



        if (!(parentElement instanceof CjExpression)) return false;

        IElementType parentOperation = getOperation((CjExpression) parentElement);









        int innerPriority = getPriority(innerExpression);
        int parentPriority = getPriority((CjExpression) parentElement);

        if (innerPriority == parentPriority) {



            return false;
        }

        return innerPriority < parentPriority;
    }

    @Nullable
    public static CjElement getEnclosingElementForLocalDeclaration(@NotNull CjDeclaration declaration, boolean skipParameters) {
        if (declaration instanceof CjTypeParameter && skipParameters) {
            declaration = PsiTreeUtil.getParentOfType(declaration, CjNamedDeclaration.class);
        }


        if (declaration instanceof PsiFile) {
            return declaration;
        }


        PsiElement current = PsiTreeUtil.getStubOrPsiParent(declaration);
        boolean isNonLocalCallable = isNonLocalCallable(declaration);
        while (current != null) {
            PsiElement parent = PsiTreeUtil.getStubOrPsiParent(current);


            if (current instanceof CjParameter) {
                return (CjElement) current;
            }
            if (current instanceof CjValueArgument) {
                   if (!isNonLocalCallable) {
                    return (CjElement) current;
                }
            }



            current = parent;
        }
        return null;
    }
    @NotNull
    public static String unquoteIdentifier(@NotNull String quoted) {
        if (quoted.indexOf('`') < 0) {
            return quoted;
        }

        if (quoted.startsWith("`") && quoted.endsWith("`") && quoted.length() >= 2) {
            return quoted.substring(1, quoted.length() - 1);
        }
        else {
            return quoted;
        }
    }
}
