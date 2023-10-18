package com.huawei.cangjie1.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.util.IncorrectOperationException;
import org.jetbrains.annotations.NotNull;



public abstract class CjExpressionImplStub<T extends StubElement<?>> extends CjElementImplStub<T> implements CjExpression {
    public CjExpressionImplStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public CjExpressionImplStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitExpression(this, data);
    }

    @NotNull
    @Override
    public PsiElement replace(@NotNull PsiElement newElement) throws IncorrectOperationException {
        return CjExpressionImpl.Companion.replaceExpression(this, newElement, true, this::rawReplace);
    }

    @NotNull
    public PsiElement rawReplace(@NotNull PsiElement newElement) {
        return super.replace(newElement);
    }

    @Override
    public PsiElement getParent() {
        T stub = getStub();
        if (stub != null) {
            //noinspection unchecked
            return stub.getParentStub().getPsi();
        }
        return super.getParent();
    }
}
