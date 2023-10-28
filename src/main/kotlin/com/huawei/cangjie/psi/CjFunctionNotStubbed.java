package com.huawei.cangjie.psi;

import com.huawei.cangjie.CjNodeTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;



@SuppressWarnings("deprecation")
public abstract class CjFunctionNotStubbed extends CjTypeParameterListOwnerNotStubbed implements CjFunction {

    public CjFunctionNotStubbed(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public CjParameterList getValueParameterList() {
        return (CjParameterList) findChildByType(CjNodeTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    @NotNull
    public List<CjParameter> getValueParameters() {
        CjParameterList list = getValueParameterList();
        return list != null ? list.getParameters() : Collections.emptyList();
    }

    @Override
    @Nullable
    public CjExpression getBodyExpression() {
        return findChildByClass(CjExpression.class);
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return false;
    }

    @Override
    @Nullable
    public CjTypeReference getReceiverTypeReference() {
        return null;
    }

    @NotNull
    @Override
    public List<CjContextReceiver> getContextReceivers() {
        return Collections.emptyList();
    }

    @Override
    @Nullable
    public CjTypeReference getTypeReference() {
        return null;
    }

    @Nullable
    @Override
    public CjTypeReference setTypeReference(@Nullable CjTypeReference typeRef) {
        if (typeRef == null) return null;
        throw new IllegalStateException("Lambda expressions can't have type reference");
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return null;
    }

    @Override
    public boolean isLocal() {
        PsiElement parent = getParent();
        return !(parent instanceof CjFile || parent instanceof CjClassBody);
    }
}
