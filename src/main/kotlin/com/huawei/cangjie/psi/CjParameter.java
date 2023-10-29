package com.huawei.cangjie.psi;



import com.huawei.cangjie.CjNodeTypes;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.stubs.CangJieParameterStub;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.PsiElement;
import com.intellij.psi.search.LocalSearchScope;
import com.intellij.psi.search.SearchScope;
import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;

import java.util.Collections;
import java.util.List;

public class CjParameter extends CjNamedDeclarationStub<CangJieParameterStub> implements CjCallableDeclaration, CjValVarKeywordOwner {


    public CjParameter(@NotNull ASTNode node) {
        super(node);
    }

    public CjParameter(@NotNull CangJieParameterStub stub) {
        super(stub, CjStubElementTypes.VALUE_PARAMETER);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitParameter(this, data);
    }

    @Override
    @Nullable
    public CjTypeReference getTypeReference() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE);
    }

    @Override
    @Nullable
    public CjTypeReference setTypeReference(@Nullable CjTypeReference typeRef) {
        return TypeRefHelpersKt.setTypeReference(this, getNameIdentifier(), typeRef);
    }
    @Nullable
    public CjDestructuringDeclaration getDestructuringDeclaration() {
        // No destructuring declaration in stubs
        if (getStub() != null) return null;

        return findChildByType(CjNodeTypes.DESTRUCTURING_DECLARATION);
    }
    @Nullable
    @Override
    public PsiElement getColon() {
        return findChildByType(CjTokens.COLON);
    }

    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(CjTokens.EQ);
    }

    public boolean hasDefaultValue() {
        CangJieParameterStub stub = getStub();
        if (stub != null) {
            return stub.hasDefaultValue();
        }
        return getDefaultValue() != null;
    }

    @Nullable
    public CjExpression getDefaultValue() {
        CangJieParameterStub stub = getStub();
        if (stub != null) {
            if (!stub.hasDefaultValue()) {
                return null;
            }

            if (getContainingCjFile().isCompiled()) {
                //don't load ast
                return null;
            }
        }

        PsiElement equalsToken = getEqualsToken();
        return equalsToken != null ? PsiTreeUtil.getNextSiblingOfType(equalsToken, CjExpression.class) : null;
    }

    public boolean isMutable() {
        CangJieParameterStub stub = getStub();
        if (stub != null) {
            return stub.isMutable();
        }

        return findChildByType(CjTokens.VAR_KEYWORD) != null;
    }



    public boolean hasValOrVar() {
        CangJieParameterStub stub = getStub();
        if (stub != null) {
            return stub.hasValOrVar();
        }
        return getValOrVarKeyword() != null;
    }

    @Override
    @Nullable
    public PsiElement getValOrVarKeyword() {
        CangJieParameterStub stub = getStub();
        if (stub != null && !stub.hasValOrVar()) {
            return null;
        }
        return findChildByType(VAL_VAR_TOKEN_SET);
    }


    public static final TokenSet VAL_VAR_TOKEN_SET = TokenSet.create(CjTokens.LET_KEYWORD, CjTokens.VAR_KEYWORD);

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }


    private <T extends PsiElement> boolean checkParentOfParentType(Class<T> klass) {
        // `parent` is supposed to be [CjParameterList]
        PsiElement parent = getParent();
        if (parent == null) {
            return false;
        }
        return klass.isInstance(parent.getParent());
    }

    public boolean isCatchParameter() {
        return checkParentOfParentType(CjCatchClause.class);
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

    @Nullable
    public CjDeclarationWithBody getOwnerFunction() {
        PsiElement parent = getParentByStub();
        if (!(parent instanceof CjParameterList)) return null;
        return ((CjParameterList) parent).getOwnerFunction();
    }
    @NotNull
    @Override
    public SearchScope getUseScope() {
        CjExpression owner = getOwnerFunction();
        if (owner instanceof CjPrimaryConstructor) {
            if (hasValOrVar()) return super.getUseScope();
            owner = ((CjPrimaryConstructor) owner).getContainingClassOrStruct();
        }
        if (owner == null) {
            owner = PsiTreeUtil.getParentOfType(this, CjExpression.class);
        }
        return new LocalSearchScope(owner != null ? owner : this);
    }
}
