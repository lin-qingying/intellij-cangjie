package com.huawei.cangjie1.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;


public class CjContainerNode extends CjElementImpl {
    public CjContainerNode(@NotNull ASTNode node) {
        super(node);
    }

    @Override // for visibility
    protected <T> T findChildByClass(Class<T> aClass) {
        return super.findChildByClass(aClass);
    }

    @Override // for visibility
    protected <T extends PsiElement> T findChildByType(IElementType type) {
        return super.findChildByType(type);
    }
}
