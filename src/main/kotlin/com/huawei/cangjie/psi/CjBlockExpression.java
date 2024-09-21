package com.huawei.cangjie.psi;

import com.huawei.cangjie.lang.CangJieLanguage;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.psiUtil.CjElementUtilsKt;
import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.huawei.cangjie.psi.psiUtil.PsiElementKt;
import com.huawei.cangjie.utils.ReadOnly;
import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;
import com.intellij.psi.PsiFile;
import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;

import static com.huawei.cangjie.CjNodeTypes.BLOCK;

public class CjBlockExpression extends LazyParseablePsiElement implements CjElement, CjExpression, CjStatementExpression {
    public CjBlockExpression(@NotNull IElementType type,  @Nullable CharSequence text) {
        super(type, text);
    }
    public CjBlockExpression(@Nullable CharSequence text) {
        super(BLOCK, text);
    }


    @NotNull
    @Override
    public Language getLanguage() {
        return CangJieLanguage.INSTANCE;
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }


    @Override
    public PsiFile getContainingFile() {
        return super.getContainingFile();
    }

    @Override
    public <T extends PsiElement> T getPsi(@NotNull Class<T> clazz) {
        return super.getPsi(clazz);
    }

    @NotNull
    @Override
    public CjFile getContainingCjFile() {
        return PsiElementKt.getContainingCjFile(this);
    }

    @Override
    public <D> void acceptChildren(@NotNull CjVisitor<Void, D> visitor, D data) {
        CjPsiUtil.visitChildren(this, visitor, data);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitBlockExpression(this, data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof CjVisitor) {
            accept((CjVisitor) visitor, null);
        } else {
            visitor.visitElement(this);
        }
    }

    @Override
    public void delete() throws IncorrectOperationException {
        CjElementUtilsKt.deleteSemicolon(this);
        super.delete();
    }

    @Override

    public PsiElement @NotNull [] getChildren() {
        PsiElement psiChild = getFirstChild();

        List<PsiElement> result = null;
        while (psiChild != null) {
            if (psiChild.getNode() instanceof CompositeElement) {
                if (result == null) result = new ArrayList<>();
                result.add(psiChild);
            }
            psiChild = psiChild.getNextSibling();
        }
        return result == null ? PsiElement.EMPTY_ARRAY : PsiUtilCore.toPsiElementArray(result);
    }

    @NotNull
    @Override
    public CjElement getPsiOrParent() {
        return this;
    }

    @Override
    public PsiElement getParent() {
        PsiElement substitute = CjPsiUtilKt.getParentSubstitute(this);
        return substitute != null ? substitute : super.getParent();
    }

    @Nullable
    public CjExpression getFirstStatement() {
        return findChildByClass(CjExpression.class);
    }

    @Nullable
    public CjExpression getLastStatement() {
        List<CjExpression> statement = getStatements();

        if (!statement.isEmpty()) {

            return statement.get(statement.size() - 1);
        }

        return null;
    }

    @ReadOnly
    @NotNull
    public List<CjExpression> getStatements() {
        return Arrays.asList(findChildrenByClass(CjExpression.class));
    }

    /**
     * 没有return关键字的语句
     *
     * @return
     */
    @ReadOnly
    @NotNull
    public Set<CjExpression> getStatementsWithoutReturnKeyword() {
        CjReturnExpression[] returns = findChildrenByClass(CjReturnExpression.class);


        Set<CjExpression> result = new HashSet<>();

        for (CjReturnExpression statement : returns) {
            result.add(statement.getReturnedExpression());
        }

        CjExpression lastStatement = getLastStatement();

        if (lastStatement != null) {
            if( !(lastStatement instanceof  CjReturnExpression)  && !(lastStatement instanceof CjDeclaration)  ){
                result.add(lastStatement);

            }
        }
        return result;
    }

    @ReadOnly
    @NotNull
    public Set<CjExpression> getReturnStatements() {
        Set<CjExpression> returns = new HashSet<>(List.of(findChildrenByClass(CjReturnExpression.class)));

        CjExpression lastStatement = getLastStatement();
        if (lastStatement != null) {
            returns.add(lastStatement);
        }
        return returns;
    }


    @Nullable
    public TextRange getLastBracketRange() {
        PsiElement rBrace = getRBrace();
        return rBrace != null ? rBrace.getTextRange() : null;
    }

    @Nullable
    public PsiElement getRBrace() {
        return findPsiChildByType(CjTokens.RBRACE);
    }

    @Nullable
    public PsiElement getLBrace() {
        return findPsiChildByType(CjTokens.LBRACE);
    }
}
