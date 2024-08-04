package com.linqingying.cangjie.psi;

import com.intellij.lang.ASTNode;
import com.intellij.psi.PsiElement;
import com.intellij.psi.tree.IElementType;
import org.jetbrains.annotations.NotNull;


public class CjContainerNode extends CjElementImpl {
    public CjContainerNode(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    protected <T> T findChildByClass(Class<T> aClass) {
        return super.findChildByClass(aClass);
    }

    @Override
    protected <T extends PsiElement> T findChildByType(IElementType type) {
        return super.findChildByType(type);
    }
}
