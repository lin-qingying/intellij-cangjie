package com.huawei.cangjie.descriptors;


import com.huawei.cangjie.mpp.TypeParameterSymbolMarker;
import com.huawei.cangjie.storage.StorageManager;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeConstructor;
import com.huawei.cangjie.types.Variance;
import com.huawei.cangjie.types.model.TypeParameterMarker;

import org.jetbrains.annotations.NotNull;

import java.util.List;

public interface TypeParameterDescriptor extends ClassifierDescriptor, TypeParameterMarker, TypeParameterSymbolMarker {
    boolean isReified();


    @NotNull
    Variance getVariance();
    @NotNull
    List<CangJieType> getUpperBounds();

    @NotNull
    @Override
    TypeConstructor getTypeConstructor();

    @NotNull
    @Override
    TypeParameterDescriptor getOriginal();

    int getIndex();

    /**
     * Is current parameter just a copy of another type parameter (getOriginal) from outer declaration
     * to be used for type constructor of inner declaration (i.e. inner class).
     *
     * If this method returns true:
     * 1. Containing declaration for current parameter is the inner one
     * 2. 'getOriginal' returns original type parameter from outer declaration
     * 3. 'getTypeConstructor' is the same as for original declaration (at least in means of 'equals')
     */
    boolean isCapturedFromOuterDeclaration();

    @NotNull
    StorageManager getStorageManager();
}
