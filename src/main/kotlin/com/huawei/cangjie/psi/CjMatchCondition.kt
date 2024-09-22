package com.huawei.cangjie.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;

public abstract class CjMatchCondition  extends CjElementImpl{
    public CjMatchCondition(@NotNull ASTNode node) {
        super(node);
    }
}
