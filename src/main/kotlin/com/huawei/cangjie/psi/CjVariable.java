package com.huawei.cangjie.psi;

import com.huawei.cangjie.lexer.CjTokens;
import com.huawei.cangjie.psi.stubs.CangJieVariableStub;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.diagnostic.Logger;
import com.intellij.psi.PsiElement;

import com.intellij.psi.tree.TokenSet;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

import static com.huawei.cangjie.lexer.CjTokens.CONST_KEYWORD;
import static com.huawei.cangjie.lexer.CjTokens.EQ;


public class CjVariable extends CjTypeParameterListOwnerStub<CangJieVariableStub>
        implements CjVariableDeclaration {

    private static final Logger LOG = Logger.getInstance(CjVariable.class);

    public CjVariable(@NotNull CangJieVariableStub stub ) {
        super(stub, CjStubElementTypes.VARIABLE);
    }
    public CjVariable(@NotNull ASTNode node) {
        super(node);
    }


//    @Override
//    public boolean shouldChangeModificationCount(PsiElement place) {
//        return false;
//    }

    @Nullable
    @Override
    public CjParameterList getValueParameterList() {
        return null;
    }


    @Override
    public String toString() {
        return super.toString() + ": " + getName();
    }

    @Override
    public @NotNull List<CjParameter> getValueParameters() {
        return Collections.emptyList();
    }

    @Override
    public @Nullable CjTypeReference getReceiverTypeReference() {
        return null;
    }
    public boolean isTopLevel() {
        CangJieVariableStub stub = getStub();
        if (stub != null) {
            return stub.isTopLevel();
        }

        return getParent() instanceof CjFile;
    }
    @Override
    public @Nullable CjTypeReference getTypeReference() {
        CangJieVariableStub stub = getStub();
        if (stub != null) {
            if (!stub.hasReturnTypeRef()) {
                return null;
            }
            else {
                List<CjTypeReference> typeReferences = getStubOrPsiChildrenAsList(CjStubElementTypes.TYPE_REFERENCE);
                int returnTypeRefPositionInPsi = stub.isExtension() ? 1 : 0;
                if (typeReferences.size() <= returnTypeRefPositionInPsi) {
                    LOG.error("Invalid stub structure built for property:\n" + getText());
                    return null;
                }
                return typeReferences.get(returnTypeRefPositionInPsi);
            }
        }
        return TypeRefHelpersKt.getTypeReference(this);
    }
    public boolean isLocal() {
        return !isTopLevel() && !isMember();
    }


    public boolean isMember() {
        PsiElement parent = getParent();
        return parent instanceof CjClassOrStruct || parent instanceof CjClassBody  ;

    }

    @Override
    public @Nullable CjTypeReference setTypeReference(@Nullable CjTypeReference typeRef) {
        return null;
    }

    @Override
    public @Nullable PsiElement getColon() {
        return findChildByType(CjTokens.COLON);
    }

    @Override
    public boolean isVar() {
        CangJieVariableStub stub = getStub();
        if (stub != null) {
            return stub.isVar();
        }

        return getNode().findChildByType(CjTokens.VAR_KEYWORD) != null;
    }
    private static final TokenSet LET_VAR_TOKEN_SET = TokenSet.create(CjTokens.LET_KEYWORD,CONST_KEYWORD, CjTokens.VAR_KEYWORD);

    @Nullable
    @Override
    public PsiElement getLetOrVarKeyword() {
        PsiElement element = findChildByType(LET_VAR_TOKEN_SET);
        assert element != null : "Let or var should always exist for property" + this.getText();
        return element;
    }

    @Nullable
    @Override
    public CjExpression getInitializer() {
        CangJieVariableStub stub = getStub();
        if (stub != null) {
            if (!stub.hasInitializer()) {
                return null;
            }

            if (getContainingCjFile().isCompiled()) {

                return null;
            }
        }

        return PsiTreeUtil.getNextSiblingOfType(findChildByType(EQ), CjExpression.class);
    }

    @Override
    public boolean hasInitializer() {
        CangJieVariableStub stub = getStub();
        if (stub != null) {
            return stub.hasInitializer();
        }

        return getInitializer() != null;
    }



}
