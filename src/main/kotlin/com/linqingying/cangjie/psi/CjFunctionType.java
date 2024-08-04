package com.linqingying.cangjie.psi;

import com.google.common.collect.Lists;
import com.linqingying.cangjie.lexer.CjToken;
import com.linqingying.cangjie.lexer.CjTokens;
import com.linqingying.cangjie.psi.stubs.CangJiePlaceHolderStub;
import com.linqingying.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;



public class CjFunctionType extends CjElementImplStub<CangJiePlaceHolderStub<CjFunctionType>> implements CjTypeElement {

    public static final CjToken RETURN_TYPE_SEPARATOR = CjTokens.ARROW;

    public CjFunctionType(@NotNull ASTNode node) {
        super(node);
    }

    public CjFunctionType(@NotNull CangJiePlaceHolderStub<CjFunctionType> stub) {
        super(stub, CjStubElementTypes.FUNCTION_TYPE);
    }

    @NotNull
    @Override
    public List<CjTypeReference> getTypeArgumentsAsTypes() {
        ArrayList<CjTypeReference> result = Lists.newArrayList();
        List<CjTypeReference> contextReceiversTypeRefs = getContextReceiversTypeReferences();
        if (contextReceiversTypeRefs != null) {
            result.addAll(contextReceiversTypeRefs);
        }
        CjTypeReference receiverTypeRef = getReceiverTypeReference();
        if (receiverTypeRef != null) {
            result.add(receiverTypeRef);
        }
        for (CjParameter ktParameter : getParameters()) {
            result.add(ktParameter.getTypeReference());
        }
        CjTypeReference returnTypeRef = getReturnTypeReference();
        if (returnTypeRef != null) {
            result.add(returnTypeRef);
        }
        return result;
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitFunctionType(this, data);
    }

    @Nullable
    public CjParameterList getParameterList() {
        return getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @NotNull
    public List<CjParameter> getParameters() {
        CjParameterList list = getParameterList();
        return list != null ? list.getParameters() : Collections.emptyList();
    }

    @Nullable
    public CjFunctionTypeReceiver getReceiver() {
        return getStubOrPsiChild(CjStubElementTypes.FUNCTION_TYPE_RECEIVER);
    }

    @Nullable
    public CjTypeReference getReceiverTypeReference() {
        CjFunctionTypeReceiver receiverDeclaration = getReceiver();
        if (receiverDeclaration == null) {
            return null;
        }
        return receiverDeclaration.getTypeReference();
    }

    @Nullable
    public CjContextReceiverList getContextReceiverList() {
        return getStubOrPsiChild(CjStubElementTypes.CONTEXT_RECEIVER_LIST);
    }

    public List<CjTypeReference> getContextReceiversTypeReferences() {
        CjContextReceiverList contextReceiverList = getContextReceiverList();
        if (contextReceiverList != null) {
            return contextReceiverList.typeReferences();
        } else {
            return Collections.emptyList();
        }
    }

    @Nullable
    public CjTypeReference getReturnTypeReference() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_REFERENCE);
    }
}
