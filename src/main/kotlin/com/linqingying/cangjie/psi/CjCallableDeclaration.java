package com.linqingying.cangjie.psi;

import com.intellij.psi.PsiElement;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;



public interface CjCallableDeclaration extends CjNamedDeclaration, CjTypeParameterListOwner {
    @Nullable
    CjParameterList getValueParameterList();

    @NotNull
    List<CjParameter> getValueParameters();

    @Nullable
    CjTypeReference getReceiverTypeReference();

    @NotNull
    default List<CjContextReceiver> getContextReceivers() {
        return Collections.emptyList();
    }

    @Nullable
    CjTypeReference getTypeReference();

    @Nullable
    CjTypeReference setTypeReference(@Nullable CjTypeReference typeRef);

    @Nullable
    PsiElement getColon();
}
