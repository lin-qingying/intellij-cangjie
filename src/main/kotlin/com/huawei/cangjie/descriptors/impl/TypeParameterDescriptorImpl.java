package com.huawei.cangjie.descriptors.impl;

import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.SourceElement;
import com.huawei.cangjie.descriptors.SupertypeLoopChecker;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.storage.StorageManager;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.Variance;
import kotlin.jvm.functions.Function1;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class TypeParameterDescriptorImpl extends AbstractTypeParameterDescriptor{
    @Nullable
    private final Function1<CangJieType, Void> reportCycleError;
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


    @Override
    public void validate() {
        super.validate();
    }


}