package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.name.FqName;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import com.linqingying.cangjie.CjNodeTypes;

import static com.linqingying.cangjie.lexer.CjTokens.*;

@SuppressWarnings("deprecation")
public class CjDestructuringDeclarationEntry extends CjNamedDeclarationNotStubbed implements CjVariableDeclaration {

    private static final TokenSet VAL_VAR_KEYWORDS = TokenSet.create(LET_KEYWORD, VAR_KEYWORD,CONST_KEYWORD);

    public CjDestructuringDeclarationEntry(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public CjTypeReference getTypeReference() {
        return TypeRefHelpersKt.getTypeReference(this);
    }

    @Override
    @Nullable
    public CjTypeReference setTypeReference(@Nullable CjTypeReference typeRef) {
        return TypeRefHelpersKt.setTypeReference(this, getNameIdentifier(), typeRef);
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return findChildByType(CjTokens.COLON);
    }

    @Nullable
    @Override
    public CjParameterList getValueParameterList() {
        return null;
    }

    @NotNull
    @Override
    public List<CjParameter> getValueParameters() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public CjTypeReference getReceiverTypeReference() {
        return null;
    }

    @NotNull
    @Override
    public List<CjContextReceiver> getContextReceivers() {
        return Collections.emptyList();
    }

    @Nullable
    @Override
    public CjTypeParameterList getTypeParameterList() {
        return null;
    }

    @Nullable
    @Override
    public CjTypeConstraintList getTypeConstraintList() {
        return null;
    }

    @NotNull
    @Override
    public List<CjTypeConstraint> getTypeConstraints() {
        return Collections.emptyList();
    }

    @NotNull
    @Override
    public List<CjTypeParameter> getTypeParameters() {
        return Collections.emptyList();
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitDestructuringDeclarationEntry(this, data);
    }

    @Override
    public boolean isVar() {
        return getParentNode().findChildByType(VAR_KEYWORD) != null;
    }

    @Nullable
    @Override
    public CjExpression getInitializer() {
        return null;
    }

    @Override
    public boolean hasInitializer() {
        return false;
    }

    @NotNull
    private ASTNode getParentNode() {
        ASTNode parent = getNode().getTreeParent();
        assert parent.getElementType() == CjNodeTypes.DESTRUCTURING_DECLARATION :
                "parent is " + parent.getElementType();
        return parent;
    }

    @Override
    public PsiElement getLetOrVarKeyword() {
        ASTNode node = getParentNode().findChildByType(VAL_VAR_KEYWORDS);
        if (node == null) return null;
        return node.getPsi();
    }

    @Nullable
    @Override
    public FqName getFqName() {
        return null;
    }

    @NotNull
    @Override
    public SearchScope getUseScope() {
        CjElement enclosingBlock = CjPsiUtil.getEnclosingElementForLocalDeclaration(this, false);
        if (enclosingBlock instanceof CjParameter) {
            enclosingBlock = CjPsiUtil.getEnclosingElementForLocalDeclaration((CjParameter) enclosingBlock, false);
        }
        if (enclosingBlock != null) return new LocalSearchScope(enclosingBlock);

        return super.getUseScope();
    }
}
