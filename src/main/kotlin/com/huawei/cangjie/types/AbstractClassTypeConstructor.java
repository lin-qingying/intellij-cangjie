package com.huawei.cangjie.types;


import com.huawei.cangjie.builtins.CangJieBuiltIns;
import com.huawei.cangjie.descriptors.ClassDescriptor;
import com.huawei.cangjie.descriptors.ClassifierDescriptor;
import com.huawei.cangjie.descriptors.ModalityUtilsKt;
import com.huawei.cangjie.resolve.descriptorUtil.DescriptorUtilsKt;
import com.huawei.cangjie.storage.StorageManager;
import org.jetbrains.annotations.NotNull;

public abstract class AbstractClassTypeConstructor extends AbstractTypeConstructor implements TypeConstructor {
    public AbstractClassTypeConstructor(@NotNull StorageManager storageManager) {
        super(storageManager);
    }

    @NotNull
    @Override
    public abstract ClassDescriptor getDeclarationDescriptor();


    @Override
    protected boolean isSameClassifier(@NotNull ClassifierDescriptor classifier) {
        return classifier instanceof ClassDescriptor && areFqNamesEqual(getDeclarationDescriptor(), classifier);
    }
    @Override
    public final boolean isFinal() {
        ClassDescriptor descriptor = getDeclarationDescriptor();
        return ModalityUtilsKt.isFinalClass(descriptor) && !descriptor.isExpect();
    }
    @NotNull
    @Override
    public CangJieBuiltIns getBuiltIns() {
        return DescriptorUtilsKt.getBuiltIns(getDeclarationDescriptor());
    }

}
