package com.huawei.cangjie.psi;

import com.huawei.cangjie.psi.stubs.CangJieStubWithFqName;
import com.huawei.cangjie.psi.stubs.elements.CjStubElementTypes;
import com.intellij.lang.ASTNode;
import com.intellij.psi.stubs.IStubElementType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;



public abstract class CjTypeParameterListOwnerStub<T extends CangJieStubWithFqName<?>>
        extends CjNamedDeclarationStub<T> implements CjTypeParameterListOwner {
    public CjTypeParameterListOwnerStub(@NotNull T stub, @NotNull IStubElementType nodeType) {
        super(stub, nodeType);
    }

    public CjTypeParameterListOwnerStub(@NotNull ASTNode node) {
        super(node);
    }

    @Override
    @Nullable
    public CjTypeParameterList getTypeParameterList() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_PARAMETER_LIST);
    }

    @Override
    @Nullable
    public CjTypeConstraintList getTypeConstraintList() {
        return getStubOrPsiChild(CjStubElementTypes.TYPE_CONSTRAINT_LIST);
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
