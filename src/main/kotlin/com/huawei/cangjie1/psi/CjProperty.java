package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.psi.stubs.CangJiePropertyStub;
import com.huawei.cangjie1.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;
import com.intellij.psi.PsiModifiableCodeBlock;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public class CjProperty extends CjTypeParameterListOwnerStub<CangJiePropertyStub>
        implements CjVariableDeclaration, PsiModifiableCodeBlock {

    private static final Logger LOG = Logger.getInstance(CjProperty.class);

    public CjProperty(@NotNull CangJiePropertyStub stub ) {
        super(stub, CjStubElementTypes.PROPERTY);
    }
    public CjProperty(@NotNull ASTNode node) {
        super(node);
    }


    @Override
    public boolean shouldChangeModificationCount(PsiElement place) {
        return false;
    }

    @Nullable
    @Override
    public CjParameterList getValueParameterList() {
        return null;
    }


    @Override
    public String toString() {
        return getNode().getElementType().toString();
    }

    @Override
    public @NotNull List<CjParameter> getValueParameters() {
        return null;
    }

    @Override
    public @Nullable CjTypeReference getReceiverTypeReference() {
        return null;
    }
    public boolean isTopLevel() {
        CangJiePropertyStub stub = getStub();
        if (stub != null) {
            return stub.isTopLevel();
        }

        return getParent() instanceof CjFile;
    }
    @Override
    public @Nullable CjTypeReference getTypeReference() {
        return null;
    }
    public boolean isLocal() {
        return !isTopLevel() && !isMember();
    }


    public boolean isMember() {
        PsiElement parent = getParent();
        return parent instanceof CjClassOrObject || parent instanceof CjClassBody  ;

    }

    @Override
    public @Nullable CjTypeReference setTypeReference(@Nullable CjTypeReference typeRef) {
        return null;
    }

    @Override
    public @Nullable PsiElement getColon() {
        return null;
    }

    @Override
    public boolean isVar() {
        return false;
    }

    @Nullable
    @Override
    public PsiElement getValOrVarKeyword() {
        return null;
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
}
