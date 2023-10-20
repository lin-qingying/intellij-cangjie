package com.huawei.cangjie.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public class CjKeyword extends CjElementImpl implements CjElement {


    public CjKeyword(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
         return  visitor.visitKeyword(this, data);
    }

}
