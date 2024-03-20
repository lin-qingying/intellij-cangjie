package com.huawei.cangjie.psi;

import com.huawei.cangjie.doc.psi.CDoc;
import com.huawei.cangjie.psi.psiUtil.FindDocCommentKt;
import com.intellij.lang.ASTNode;
import com.intellij.openapi.application.ApplicationManager;
import com.intellij.psi.PsiElement;
import com.intellij.psi.stubs.IStubElementType;
import com.intellij.psi.stubs.StubElement;
import com.intellij.psi.util.PsiTreeUtil;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.atomic.AtomicLong;



public abstract class CjDeclarationStub<T extends StubElement<?>> extends CjModifierListOwnerStub<T> implements CjDeclaration {
    private final AtomicLong modificationStamp = new AtomicLong();

    public CjDeclarationStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public CjDeclarationStub(@NotNull ASTNode node) {
        super(node);
    }
    @Override
    public @Nullable CjExpression getExpression() {
        return PsiTreeUtil.getStubChildOfType(this, CjExpression.class);

    }
    @Override
    public void subtreeChanged() {
        super.subtreeChanged();
        modificationStamp.getAndIncrement();
    }

    public long getModificationStamp() {
        return modificationStamp.get();
    }

    @Nullable
    @Override
    public CDoc getDocComment() {
        return FindDocCommentKt.findDocComment(this);
    }

    @Override
    public PsiElement getParent() {
        T stub = getStub();
        // we build stubs for local classes/objects too but they have wrong parent
//        if (stub != null && !(stub instanceof CangJieClassOrStructStub && ((CangJieClassOrStructStub) stub).isLocal())) {
//            return stub.getParentStub().getPsi();
//        }
        return super.getParent();
    }



    @Override
    public PsiElement getOriginalElement() {
        CangJieDeclarationNavigationPolicy navigationPolicy = ApplicationManager.getApplication().getService(CangJieDeclarationNavigationPolicy.class);
        return navigationPolicy != null ? navigationPolicy.getOriginalElement(this) : this;
    }

    @NotNull
    @Override
    public PsiElement getNavigationElement() {
        CangJieDeclarationNavigationPolicy navigationPolicy = ApplicationManager.getApplication().getService(CangJieDeclarationNavigationPolicy.class);
        return navigationPolicy != null ? navigationPolicy.getNavigationElement(this) : this;
    }
}
