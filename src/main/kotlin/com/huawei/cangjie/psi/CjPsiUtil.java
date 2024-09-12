package com.huawei.cangjie.psi;

import com.huawei.cangjie.CjNodeTypes;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.name.SpecialNames;
import com.huawei.cangjie.parsing.CangJieExpressionParsing;
import com.huawei.cangjie.resolve.StatementFilter;
import com.huawei.cangjie.resolve.StatementFilterKt;
import com.huawei.cangjie.utils.OperatorNameConventions;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiFile;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;
import  com.huawei.cangjie.lexer.CjTokens;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class CjPsiUtil {
    public interface CjExpressionWrapper {
        CjExpression getBaseExpression();
    }
    @NotNull
    public static CjExpression safeDeparenthesize(@NotNull CjExpression expression) {
        return safeDeparenthesize(expression, false);
    }
    public static boolean isBooleanConstant(@Nullable CjExpression condition) {
        return condition != null && condition.getNode().getElementType() == CjNodeTypes.BOOLEAN_CONSTANT;
    }

    @NotNull
    public static Set<CjElement> findRootExpressions(@NotNull Collection<CjElement> unreachableElements) {
        Set<CjElement> rootElements = new HashSet<>();
        Set<CjElement> shadowedElements = new HashSet<>();
        CjVisitorVoid shadowAllChildren = new CjVisitorVoid() {
            @Override
            public void visitCjElement(@NotNull CjElement element) {
                if (shadowedElements.add(element)) {
                    element.acceptChildren(this);
                }
            }
        };

        for (CjElement element : unreachableElements) {
            if (shadowedElements.contains(element)) continue;
            element.acceptChildren(shadowAllChildren);

            rootElements.removeAll(shadowedElements);
            rootElements.add(element);
        }
        return rootElements;
    }
    public static boolean isTrueConstant(@Nullable CjExpression condition) {
        return isBooleanConstant(condition) && condition.getNode().findChildByType(CjTokens.TRUE_KEYWORD) != null;
    }
    public static boolean isSelectorInQualified(@NotNull CjSimpleNameExpression nameExpression) {
        CjElement qualifiedElement = CjPsiUtilKt.getQualifiedElement(nameExpression);
        return qualifiedElement instanceof CjQualifiedExpression
                || ((qualifiedElement instanceof CjUserType) && ((CjUserType) qualifiedElement).getQualifier() != null);
    }
    @SuppressWarnings("unused") // used in intellij repo
    public static boolean areParenthesesUseless(@NotNull CjParenthesizedExpression expression) {
        CjExpression innerExpression = expression.getExpression();
        if (innerExpression == null) return true;
        PsiElement parent = expression.getParent();
        if (!(parent instanceof CjElement)) return true;
        return !areParenthesesNecessary(innerExpression, expression, (CjElement) parent);
    }
    @Nullable
    public static CjExpression getLastStatementInABlock(@Nullable CjBlockExpression blockExpression) {
        if (blockExpression == null) return null;
        List<CjExpression> statements = blockExpression.getStatements();
        return statements.isEmpty() ? null : statements.get(statements.size() - 1);
    }

    @Nullable
    public static CjExpression getLastElementDeparenthesized(
            @Nullable CjExpression expression,
            @NotNull StatementFilter statementFilter
    ) {
        CjExpression deparenthesizedExpression = deparenthesize(expression);
        if (deparenthesizedExpression instanceof CjBlockExpression) {
            CjBlockExpression blockExpression = (CjBlockExpression) deparenthesizedExpression;
            // todo
            // This case is a temporary hack for 'if' branches.
            // The right way to implement this logic is to interpret 'if' branches as function literals with explicitly-typed signatures
            // (no arguments and no receiver) and therefore analyze them straight away (not in the 'complete' phase).
            CjExpression lastStatementInABlock = StatementFilterKt.getLastStatementInABlock(statementFilter, blockExpression);
            if (lastStatementInABlock != null) {
                return getLastElementDeparenthesized(lastStatementInABlock, statementFilter);
            }
        }
        return deparenthesizedExpression;
    }
    @Nullable
    public static CjExpression deparenthesizeOnce(
            @Nullable CjExpression expression, boolean keepAnnotations
    ) {
//        if (expression instanceof CjAnnotatedExpression && !keepAnnotations) {
//            return ((CjAnnotatedExpression) expression).getBaseExpression();
//        }
//        else if (expression instanceof CjLabeledExpression) {
//            return ((CjLabeledExpression) expression).getBaseExpression();
//        }
//        else
            if (expression instanceof CjExpressionWrapper) {
            return ((CjExpressionWrapper) expression).getBaseExpression();
        }
        else if (expression instanceof CjParenthesizedExpression) {
            return ((CjParenthesizedExpression) expression).getExpression();
        }
        return expression;
    }
    @Nullable
    public static CjExpression deparenthesizeOnce(
            @Nullable CjExpression expression
    ) {
        return deparenthesizeOnce(expression, false);
    }

    @Nullable
    public static CjExpression deparenthesize(@Nullable CjExpression expression) {
        return deparenthesize(expression, false);
    }

    @Nullable
    public static CjExpression deparenthesize(@Nullable CjExpression expression, boolean keepAnnotations) {
        while (true) {
            CjExpression baseExpression = deparenthesizeOnce(expression, keepAnnotations);

            if (baseExpression == expression) return baseExpression;
            expression = baseExpression;
        }
    }
    @NotNull
    public static CjExpression safeDeparenthesize(@NotNull CjExpression expression, boolean keepAnnotations) {
        CjExpression deparenthesized = deparenthesize(expression, keepAnnotations);
        return deparenthesized != null ? deparenthesized : expression;
    }
    public static boolean isStatementContainer(@Nullable PsiElement container) {
        return container instanceof CjBlockExpression ||
                container instanceof CjContainerNodeForControlStructureBody  ;
    }
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
    public static CjTypeStatement getClassIfParameterIsProperty(@NotNull CjParameter cjParameter) {
        if (cjParameter.hasLetOrVar()) {
            PsiElement grandParent = null;
            if (cjParameter.getParent() != null) {
                grandParent = cjParameter.getParent().getParent();
            }
            if (grandParent instanceof CjPrimaryConstructor) {
                return ((CjPrimaryConstructor) grandParent).getContainingTypeStatement();
            }
        }

        return null;
    }

    @NotNull
    public static Name safeName(@Nullable String name) {
        return name == null ? SpecialNames.NO_NAME_PROVIDED : OperatorNameConventions.INSTANCE.asOperatorName(name);
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
