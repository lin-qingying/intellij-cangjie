package com.huawei.cangjie1.psi;



import com.huawei.cangjie1.lexer.CjModifierKeywordToken;
import com.huawei.cangjie1.psi.stubs.CangJieModifierListStub;
import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.tree.TokenSet;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;



public abstract class CjModifierList extends CjElementImplStub<CangJieModifierListStub> implements CjElement {

    public CjModifierList(@NotNull CangJieModifierListStub stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public CjModifierList(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitModifierList(this, data);
    }

    @NotNull


    public boolean hasModifier(@NotNull CjModifierKeywordToken tokenType) {
        CangJieModifierListStub stub = getStub();
        if (stub != null) {
            return stub.hasModifier(tokenType);
        }
        return getModifier(tokenType) != null;
    }

    @Nullable
    public PsiElement getModifier(@NotNull CjModifierKeywordToken tokenType) {
        return findChildByType(tokenType);
    }

    @Nullable
    public PsiElement getModifier(@NotNull TokenSet tokenTypes) {
        return findChildByType(tokenTypes);
    }


    public PsiElement getOwner() {
        return getParentByStub();
    }

    @Override
    public void deleteChildInternal(@NotNull ASTNode child) {
        super.deleteChildInternal(child);
        if (getFirstChild() == null) {
            delete();
        }
    }
}
