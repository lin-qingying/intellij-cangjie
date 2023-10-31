package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.huawei.cangjie.psi.stubs.CangJieFunctionStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.navigation.ItemPresentation;
import com.intellij.navigation.ItemPresentationProviders;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;


public class CjNamedFunction extends CjFunctionImpl {
    public CjNamedFunction(@NotNull ASTNode node) {
        super(node);
    }

    public CjNamedFunction(@NotNull CangJieFunctionStub stub) {
        super(stub, CjStubElementTypes.FUNCTION);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, @Nullable D data) {
        return visitor.visitNamedFunction(this, data);
    }

    public boolean hasTypeParameterListBeforeFunctionName() {
        CangJieFunctionStub stub = getStub();
        if (stub != null) {
            return stub.hasTypeParameterListBeforeFunctionName();
        }
        return hasTypeParameterListBeforeFunctionNameByTree();
    }

    private boolean hasTypeParameterListBeforeFunctionNameByTree() {
        CjTypeParameterList typeParameterList = getTypeParameterList();
        if (typeParameterList == null) {
            return false;
        }
        PsiElement nameIdentifier = getNameIdentifier();
        if (nameIdentifier == null) {
            return true;
        }
        return nameIdentifier.getTextOffset() > typeParameterList.getTextOffset();
    }

    @Override
    public boolean hasBlockBody() {
        CangJieFunctionStub stub = getStub();
        if (stub != null) {
            return stub.hasBlockBody();
        }
        return getEqualsToken() == null;
    }

    @Nullable
    @IfNotParsed // "function" with no "fun" keyword is created by parser for "{...}" on top-level or in class body
    public PsiElement getFunKeyword() {
        return findChildByType(CjTokens.FUNC_KEYWORD);
    }

    @Override
    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(CjTokens.EQ);
    }

    @Override
    @Nullable
    public CjExpression getInitializer() {
        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), CjExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        return getInitializer() != null;
    }

    @Override
    public ItemPresentation getPresentation() {
        return ItemPresentationProviders.getItemPresentation(this);
    }

    @Override
    @Nullable
    public CjParameterList getValueParameterList() {
        return getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST);
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
        CangJieFunctionStub stub = getStub();
        if (stub != null) {
            if (!stub.hasBody()) {
                return null;
            }
            if (getContainingCjFile().isCompiled()) {
                //don't load ast
                return null;
            }
        }

        return findChildByClass(CjExpression.class);
    }

    @Nullable
    @Override
    public CjBlockExpression getBodyBlockExpression() {
        CangJieFunctionStub stub = getStub();
        if (stub != null) {
            if (!(stub.hasBlockBody() && stub.hasBody())) {
                return null;
            }
            if (getContainingCjFile().isCompiled()) {
                //don't load ast
                return null;
            }
        }

        CjExpression bodyExpression = findChildByClass(CjExpression.class);
        if (bodyExpression instanceof CjBlockExpression) {
            return (CjBlockExpression) bodyExpression;
        }

        return null;
    }

    @Override
    public boolean hasBody() {
        CangJieFunctionStub stub = getStub();
        if (stub != null) {
            return stub.hasBody();
        }
        return getBodyExpression() != null;
    }

    @Override
    public boolean hasDeclaredReturnType() {
        return getTypeReference() != null;
    }

    @Override
    @Nullable
    public CjTypeReference getReceiverTypeReference() {
        CangJieFunctionStub stub = getStub();
        if (stub != null) {
            if (!stub.isExtension()) {
                return null;
            }
            List<CjTypeReference> childTypeReferences = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE);
            if (!childTypeReferences.isEmpty()) {
                return childTypeReferences.get(0);
            } else {
                return null;
            }
        }
        return getReceiverTypeRefByTree();
    }

    @Nullable
    private CjTypeReference getReceiverTypeRefByTree() {
        PsiElement child = getFirstChild();
        while (child != null) {
            IElementType tt = child.getNode().getElementType();
            if (tt == CjTokens.LPAR || tt == CjTokens.COLON) break;
            if (child instanceof CjTypeReference) {
                return (CjTypeReference) child;
            }
            child = child.getNextSibling();
        }

        return null;
    }

    @Override
    public String toString() {
//        return getNode().getElementType().toString();
        return getNode().getElementType() + ": " + getName();
    }

    @NotNull
    @Override
    public List<CjContextReceiver> getContextReceivers() {
        CjContextReceiverList contextReceiverList = getStubOrPsiChild(CjStubElementTypes.CONTEXT_RECEIVER_LIST);
        if (contextReceiverList != null) {
            return contextReceiverList.contextReceivers();
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    @Nullable
    public CjTypeReference getTypeReference() {
        CangJieFunctionStub stub = getStub();
        if (stub != null) {
            List<CjTypeReference> typeReferences = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE);
            int returnTypeIndex = stub.isExtension() ? 1 : 0;
            if (returnTypeIndex >= typeReferences.size()) {
                return null;
            }
            return typeReferences.get(returnTypeIndex);
        }
        return TypeRefHelpersKt.getTypeReference(this);
    }

    @Override
    @Nullable
    public CjTypeReference setTypeReference(@Nullable CjTypeReference typeRef) {
        return TypeRefHelpersKt.setTypeReference(this, getValueParameterList(), typeRef);
    }

    @Nullable
    @Override
    public PsiElement getColon() {
        return findChildByType(CjTokens.COLON);
    }

    @Override
    public boolean isLocal() {
        PsiElement parent = getParent();
        return !(parent instanceof CjFile || parent instanceof CjClassBody);
    }

    public boolean isAnonymous() {
        return getName() == null && isLocal();
    }

    public boolean isTopLevel() {
        CangJieFunctionStub stub = getStub();
        if (stub != null) {
            return stub.isTopLevel();
        }

        return getParent() instanceof CjFile;
    }

//    @Override
//    public boolean shouldChangeModificationCount(PsiElement place) {
//        // Suppress Java check for out-of-block
//        return false;
//    }


//    public boolean mayHaveContract() {
//        return mayHaveContract(true);
//    }
//
//    public boolean mayHaveContract(boolean isAllowedOnMembers) {
//        CangJieFunctionStub stub = getStub();
//        if (stub != null) {
//            return stub.mayHaveContract();
//        }
//
//        return CjPsiUtilKt.isContractPresentPsiCheck(this, isAllowedOnMembers);
//    }
}
