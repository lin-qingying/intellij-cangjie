package com.linqingying.cangjie.types;


import com.linqingying.cangjie.builtins.CangJieBuiltIns;
import com.linqingying.cangjie.descriptors.ClassDescriptor;
import com.linqingying.cangjie.descriptors.ClassifierDescriptor;
import com.linqingying.cangjie.descriptors.ModalityUtilsKt;
import com.linqingying.cangjie.resolve.descriptorUtil.DescriptorUtilsKt;
import com.linqingying.cangjie.storage.StorageManager;
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
