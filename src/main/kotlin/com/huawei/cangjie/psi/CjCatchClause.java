package com.huawei.cangjie.psi;

import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;

 import com.huawei.cangjie.CjNodeTypes;

public class CjCatchClause extends CjElementImpl {
    public CjCatchClause(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    public <R, D> R accept(@NotNull CjVisitor<R, D> visitor, D data) {
        return visitor.visitCatchSection(this, data);
    }

    @Nullable
    @IfNotParsed
    public CjParameterList getParameterList() {
        return findChildByType(CjNodeTypes.VALUE_PARAMETER_LIST);
    }

    @Nullable @IfNotParsed
    public CjParameter getCatchParameter() {
        CjParameterList list = getParameterList();
        if (list == null) return null;
        List<CjParameter> parameters = list.getParameters();
        return parameters.size() == 1 ? parameters.get(0) : null;
    }


    @Nullable @IfNotParsed
    public CjExpression getCatchBody() {
        return findChildByClass(CjExpression.class);
    }
}
