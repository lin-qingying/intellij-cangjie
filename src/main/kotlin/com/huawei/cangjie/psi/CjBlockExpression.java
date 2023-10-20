package com.huawei.cangjie.psi;

import com.huawei.cangjie.lang.CangJieLanguage;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.psiUtil.CjElementUtilsKt;
import com.huawei.cangjie.psi.psiUtil.PsiElementKt;
import com.huawei.cangjie.utils.ReadOnly;
import com.intellij.lang.Language;
import com.intellij.openapi.util.TextRange;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiElementVisitor;

import com.intellij.psi.impl.source.tree.CompositeElement;
import com.intellij.psi.impl.source.tree.LazyParseablePsiElement;
import com.intellij.psi.util.PsiUtilCore;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;

import static com.huawei.cangjie.CjNodeTypes.BLOCK;

@SuppressWarnings("deprecation")
public class CjBlockExpression extends LazyParseablePsiElement implements CjElement, CjExpression, CjStatementExpression {

    public CjBlockExpression(@Nullable CharSequence text) {
        super(BLOCK, text);
    }

//    @Override
//    public boolean shouldChangeModificationCount(PsiElement place) {
//          return false;
//    }

    @NotNull
    @Override
    public Language getLanguage() {
        return CangJieLanguage.INSTANCE;
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }

    @NotNull
    @Override
    public CjFile getContainingCjFile() {
        return PsiElementKt. getContainingCjFile(this);
    }

    @Override
    public <D> void acceptChildren(@NotNull CjVisitor<Void, D> visitor, D data) {
        CjPsiUtil.visitChildren(this, visitor, data);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitBlockExpression(this, data);
    }

    @Override
    @SuppressWarnings("unchecked")
    public final void accept(@NotNull PsiElementVisitor visitor) {
        if (visitor instanceof CjVisitor) {
            accept((CjVisitor) visitor, null);
        }
        else {
            visitor.visitElement(this);
        }
    }

    @Override
    public void delete() throws IncorrectOperationException {
        CjElementUtilsKt.deleteSemicolon(this);
        super.delete();
    }

    @Override
    @NotNull
    public PsiElement[] getChildren() {
        PsiElement psiChild = getFirstChild();

        List<PsiElement> result = null;
        while (psiChild != null) {
            if (psiChild.getNode() instanceof CompositeElement) {
                if(result == null) result = new ArrayList<>();
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

    @ReadOnly
    @NotNull
    public List<CjExpression> getStatements() {
        return Arrays.asList(findChildrenByClass(CjExpression.class));
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
