package com.huawei.cangjie.descriptors.impl;

import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.SourceElement;
import com.huawei.cangjie.descriptors.SupertypeLoopChecker;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.resolve.DescriptorUtils;
import com.huawei.cangjie.storage.StorageManager;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.CangJieTypeKt;
import com.huawei.cangjie.types.Variance;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

public class TypeParameterDescriptorImpl extends AbstractTypeParameterDescriptor {
    @Nullable
    private final Function1<CangJieType, Void> reportCycleError;
    private boolean initialized = false;
    private final List<CangJieType> upperBounds = new ArrayList<CangJieType>(1);

    public void addUpperBound(@NotNull CangJieType bound) {
        checkUninitialized();
        doAddUpperBound(bound);
    }
    private void doAddUpperBound(CangJieType bound) {
        if (CangJieTypeKt.isError(bound)) return;
        upperBounds.add(bound); // TODO : Duplicates?
    }
    private TypeParameterDescriptorImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull Name name,
            int index,
            @NotNull SourceElement source,
            @Nullable Function1<CangJieType, Void> reportCycleError,
            @NotNull SupertypeLoopChecker supertypeLoopsChecker,
            @NotNull StorageManager storageManager
    ) {
        super(storageManager, containingDeclaration, annotations, name, variance, reified, index, source, supertypeLoopsChecker);
        this.reportCycleError = reportCycleError;
    }

    public static TypeParameterDescriptorImpl createForFurtherModification(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull Name name,
            int index,
            @NotNull SourceElement source,
            @NotNull StorageManager storageManager
    ) {
        return createForFurtherModification(
                containingDeclaration, annotations, reified, variance, name, index, source,
                null, SupertypeLoopChecker.EMPTY.INSTANCE, storageManager
        );
    }

    public static TypeParameterDescriptorImpl createForFurtherModification(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            boolean reified,
            @NotNull Variance variance,
            @NotNull Name name,
            int index,
            @NotNull SourceElement source,
            @Nullable Function1<CangJieType, Void> reportCycleError,
            @NotNull SupertypeLoopChecker supertypeLoopsResolver,
            @NotNull StorageManager storageManager
    ) {
        return new TypeParameterDescriptorImpl(
                containingDeclaration, annotations, reified, variance, name,
                index, source, reportCycleError, supertypeLoopsResolver, storageManager
        );
    }

    public boolean isInitialized() {
        return initialized;
    }

    private String nameForAssertions() {
        return getName() + " declared in " + DescriptorUtils.getFqName(getContainingDeclaration());
    }

    private void checkUninitialized() {
        if (initialized) {
            throw new IllegalStateException("Type parameter descriptor is already initialized: " + nameForAssertions());
        }
    }

    public void setInitialized() {
        checkUninitialized();
        initialized = true;
    }


    @Override
    protected void reportSupertypeLoopError(@NotNull CangJieType type) {
        if (reportCycleError == null) return;
        reportCycleError.invoke(type);
    }
    private void checkInitialized() {
        if (!initialized) {
            throw new IllegalStateException("Type parameter descriptor is not initialized: " + nameForAssertions());
        }
    }
    @Override
    public void validate() {
        super.validate();
    }

    @Override
    protected @NotNull List<CangJieType> resolveUpperBounds() {
        checkInitialized();
        return upperBounds;
    }


}
