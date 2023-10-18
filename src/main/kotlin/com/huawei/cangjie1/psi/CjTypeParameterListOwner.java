package com.huawei.cangjie1.psi;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


public interface CjTypeParameterListOwner extends CjNamedDeclaration {
    @Nullable
    CjTypeParameterList getTypeParameterList();

    @Nullable
    CjTypeConstraintList getTypeConstraintList();

    @NotNull
    List<CjTypeConstraint> getTypeConstraints();

    @NotNull
    List<CjTypeParameter> getTypeParameters();
}
