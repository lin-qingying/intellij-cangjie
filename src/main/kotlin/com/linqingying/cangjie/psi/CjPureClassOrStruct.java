package com.linqingying.cangjie.psi;

import com.linqingying.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;



public interface CjPureClassOrStruct extends CjPureElement, CjDeclarationContainer {
    @Nullable
    String getName();

    boolean isLocal();

    @NotNull
    @ReadOnly
    List<CjSuperTypeListEntry> getSuperTypeListEntries();


    boolean hasExplicitPrimaryConstructor();

    boolean hasPrimaryConstructor();

    @Nullable
    CjPrimaryConstructor getPrimaryConstructor();

    @Nullable
    CjModifierList getPrimaryConstructorModifierList();

    @NotNull
    @ReadOnly
    List<CjParameter> getPrimaryConstructorParameters();

    @NotNull
    @ReadOnly
    List<CjSecondaryConstructor> getSecondaryConstructors();

    @NotNull
    @ReadOnly
    List<CjContextReceiver> getContextReceivers();

    @Nullable
    CjClassBody getBody();
}

