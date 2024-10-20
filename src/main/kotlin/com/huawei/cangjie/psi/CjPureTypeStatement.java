package com.huawei.cangjie.psi;

import com.huawei.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;



public interface CjPureTypeStatement extends CjPureElement, CjDeclarationContainer {
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
    List<CjEndSecondaryConstructor> getEndSecondaryConstructors();
    @NotNull
    @ReadOnly
    List<CjSecondaryConstructor> getSecondaryConstructors();
    @NotNull
    List<CjPrimaryConstructor> getPrimaryConstructors();
    @NotNull
    @ReadOnly
    List<CjContextReceiver> getContextReceivers();

    @Nullable
    CjAbstractClassBody getBody();
}

