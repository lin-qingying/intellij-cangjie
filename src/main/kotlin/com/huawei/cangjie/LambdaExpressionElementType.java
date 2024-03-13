package com.huawei.cangjie;


import com.huawei.cangjie.lang.CangJieLanguage;
import com.huawei.cangjie.lexer.CangJieLexer;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.parsing.CangJieParser;
import com.huawei.cangjie.psi.CjFunctionLiteral;
import com.huawei.cangjie.psi.CjLambdaExpression;
import com.huawei.cangjie.psi.CjParameterList;
import com.intellij.lang.ASTNode;
import com.intellij.lang.Language;
import com.intellij.lang.PsiBuilder;
import com.intellij.lang.PsiBuilderFactory;
import com.intellij.lexer.Lexer;
import com.intellij.openapi.project.Project;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.tree.IErrorCounterReparseableElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.Nullable;

class LambdaExpressionElementType extends IErrorCounterReparseableElementType {
    public LambdaExpressionElementType() {
        super("LAMBDA_EXPRESSION", CangJieLanguage.INSTANCE);
    }

    @Override
    public ASTNode parseContents(ASTNode chameleon) {
        Project project = chameleon.getPsi().getProject();
        PsiBuilder builder = PsiBuilderFactory.getInstance().createBuilder(
                project, chameleon, null, CangJieLanguage.INSTANCE, chameleon.getChars());
        return CangJieParser.parseLambdaExpression(builder).getFirstChildNode();
    }

    @Override
    public ASTNode createNode(CharSequence text) {
        return new CjLambdaExpression(text);
    }

    @Override
    public boolean isParsable(@Nullable ASTNode parent, CharSequence buffer, Language fileLanguage, Project project) {
        return super.isParsable(parent, buffer, fileLanguage, project) &&
                !wasArrowMovedOrDeleted(parent, buffer) && !wasParameterCommaMovedOrDeleted(parent, buffer);
    }

    private static boolean wasArrowMovedOrDeleted(@Nullable ASTNode parent, CharSequence buffer) {
        CjLambdaExpression lambdaExpression = findLambdaExpression(parent);
        if (lambdaExpression == null) {
            return false;
        }

        CjFunctionLiteral literal = lambdaExpression.getFunctionLiteral();
        PsiElement arrow = literal.getArrow();

        // No arrow in original node
        if (arrow == null) return false;

        int arrowOffset = arrow.getStartOffsetInParent() + literal.getStartOffsetInParent();

        return hasTokenMoved(lambdaExpression.getText(), buffer, arrowOffset, CjTokens.ARROW);
    }

    private static boolean wasParameterCommaMovedOrDeleted(@Nullable ASTNode parent, CharSequence buffer) {
        CjLambdaExpression lambdaExpression = findLambdaExpression(parent);
        if (lambdaExpression == null) {
            return false;
        }

        CjFunctionLiteral literal = lambdaExpression.getFunctionLiteral();
        CjParameterList valueParameterList = literal.getValueParameterList();
        if (valueParameterList == null || valueParameterList.getParameters().size() <= 1) {
            return false;
        }

        PsiElement comma = valueParameterList.getFirstComma();
        if (comma == null) {
            return false;
        }

        int commaOffset = comma.getTextOffset() - lambdaExpression.getTextOffset();
        return hasTokenMoved(lambdaExpression.getText(), buffer, commaOffset, CjTokens.COMMA);
    }

    private static CjLambdaExpression findLambdaExpression(@Nullable ASTNode parent) {
        if (parent == null) return null;

        PsiElement parentPsi = parent.getPsi();
        CjLambdaExpression[] lambdaExpressions = PsiTreeUtil.getChildrenOfType(parentPsi, CjLambdaExpression.class);
        if (lambdaExpressions == null || lambdaExpressions.length != 1) return null;

        // Now works only when actual node can be spotted ambiguously. Need change in API.
        return lambdaExpressions[0];
    }

    private static boolean hasTokenMoved(String oldText, CharSequence buffer, int oldOffset, IElementType tokenType) {
        Lexer oldLexer = new CangJieLexer();
        oldLexer.start(oldText);

        Lexer newLexer = new CangJieLexer();
        newLexer.start(buffer);

        while (true) {
            IElementType oldType = oldLexer.getTokenType();
            if (oldType == null) break; // Didn't find an expected token. Consider it as no token was present.

            IElementType newType = newLexer.getTokenType();
            if (newType == null) return true; // New text was finished before reaching expected token in old text

            if (newType != oldType) {
                if (newType == CjTokens.WHITE_SPACE) {
                    newLexer.advance();
                    continue;
                } else if (oldType == CjTokens.WHITE_SPACE) {
                    oldLexer.advance();
                    continue;
                }

                return true; // Expected token was moved or deleted
            }

            if (oldType == tokenType && oldLexer.getCurrentPosition().getOffset() == oldOffset) {
                break;
            }

            oldLexer.advance();
            newLexer.advance();
        }

        return false;
    }

    @Override
    public int getErrorsCount(CharSequence seq, Language fileLanguage, Project project) {
        return ElementTypeUtils.getCangJieBlockImbalanceCount(seq);
    }
}
