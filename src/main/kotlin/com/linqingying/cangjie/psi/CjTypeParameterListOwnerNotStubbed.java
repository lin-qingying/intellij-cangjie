package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.CjNodeTypes;
import com.intellij.lang.ASTNode;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;


@Deprecated
abstract class CjTypeParameterListOwnerNotStubbed extends CjNamedDeclarationNotStubbed implements CjTypeParameterListOwner {
    public CjTypeParameterListOwnerNotStubbed(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public CjTypeParameterList getTypeParameterList() {
        return findChildByType(CjNodeTypes.TYPE_PARAMETER_LIST);
    }

    @Override
    @Nullable
    public CjTypeConstraintList getTypeConstraintList() {
        return findChildByType(CjNodeTypes.TYPE_CONSTRAINT_LIST);
    }

    @Override
    @NotNull
    public List<CjTypeConstraint> getTypeConstraints() {
        CjTypeConstraintList typeConstraintList = getTypeConstraintList();
        if (typeConstraintList == null) {
            return Collections.emptyList();
        }
        return typeConstraintList.getConstraints();
    }

    @Override
    @NotNull
    public List<CjTypeParameter> getTypeParameters() {
        CjTypeParameterList list = getTypeParameterList();
        if (list == null) return Collections.emptyList();

        return list.getParameters();
    }
}
