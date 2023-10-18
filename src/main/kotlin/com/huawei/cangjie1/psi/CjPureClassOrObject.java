package com.huawei.cangjie1.psi;

import com.huawei.cangjie1.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.List;


/**
 * A minimal interface that {@link CjClassOrObject} implements for the purpose of code-generation that does not need the full power of PSI.
 * This interface can be easily implemented by synthetic elements to generate code for them.
 */
public interface CjPureClassOrObject extends CjPureElement, CjDeclarationContainer {
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

