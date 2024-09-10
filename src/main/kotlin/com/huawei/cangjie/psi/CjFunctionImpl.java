package com.huawei.cangjie.psi;

import com.huawei.cangjie.CjNodeTypes;
import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.psiUtil.CjPsiUtilKt;
import com.huawei.cangjie.psi.stubs.CangJieFunctionStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;

import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.IElementType;
import com.intellij.psi.util.PsiTreeUtil;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public abstract class CjFunctionImpl extends CjTypeParameterListOwnerStub<CangJieFunctionStub>
        implements CjFunction, CjDeclarationWithInitializer{
    public CjFunctionImpl(@NotNull ASTNode node) {
        super(node);
    }


    public CjFunctionImpl(@NotNull CangJieFunctionStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    @Override
    @Nullable
    public CjParameterList getValueParameterList() {
        return getStubOrPsiChild(CjStubElementTypes.VALUE_PARAMETER_LIST);
    }

    @Override
    public String toString() {
        return getNode().getElementType().toString();
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
    @Nullable
    public CjExpression getBodyExpression() {
        CangJieFunctionStub stub = getStub();
        if (stub != null) {
            if (!stub.hasBody()) {
                return null;
            }
            if (getContainingCjFile().isCompiled()) {

                return null;
            }
        }

        return findChildByClass(CjExpression.class);
    }

    @Override
    @Nullable
    public PsiElement getEqualsToken() {
        return findChildByType(CjTokens.EQ);
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
    public boolean hasBlockBody() {
        CangJieFunctionStub stub = getStub();
        if (stub != null) {
            return stub.hasBlockBody();
        }
        return getEqualsToken() == null;
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
        return false;
    }


    @Override
    @NotNull
    public List<CjParameter> getValueParameters() {
        CjParameterList list = getValueParameterList();
        return list != null ? list.getParameters() : Collections.emptyList();
    }

    @Override
    public boolean isLocal() {
        PsiElement parent = getParent();
        return !(parent instanceof CjFile || parent instanceof CjClassBody);
    }

    @Override
    public boolean isOperator() {
        return hasModifier(CjTokens.OPERATOR_KEYWORD);
    }
//    public bool mayHaveContract() {
//        return mayHaveContract(true);
//    }

//    public bool mayHaveContract(bool isAllowedOnMembers) {
//        CangJieFunctionStub stub = getStub();
//        if (stub != null) {
//            return stub.mayHaveContract();
//        }
//
//        return CjPsiUtilKt.isContractPresentPsiCheck(this, isAllowedOnMembers);
//    }

    @Override
    @Nullable
    public CjExpression getInitializer() {
        return PsiTreeUtil.getNextSiblingOfType(getEqualsToken(), CjExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        return getInitializer() != null;
    }

//    @Override
//    public bool shouldChangeModificationCount(PsiElement place) {
//        // Suppress Java check for out-of-block
//        return false;
//    }

    public boolean isTopLevel() {
        CangJieFunctionStub stub = getStub();
        if (stub != null) {
            return stub.isTopLevel();
        }

        return getParent() instanceof CjFile;
    }

    @Override
    public PsiElement setName(@NotNull String name) throws IncorrectOperationException {


        return super.setName(name);
    }
}
