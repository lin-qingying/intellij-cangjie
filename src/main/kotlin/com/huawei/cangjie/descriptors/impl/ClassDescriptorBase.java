package com.huawei.cangjie.descriptors.impl;


import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.SourceElement;
import com.huawei.cangjie.storage.StorageManager;

import org.jetbrains.annotations.NotNull;
import com.huawei.cangjie.name.Name;

public abstract class ClassDescriptorBase extends AbstractClassDescriptor {

    private final DeclarationDescriptor containingDeclaration;
    private final SourceElement source;
    private final boolean isExternal;

    protected ClassDescriptorBase(
            @NotNull StorageManager storageManager,
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Name name,
            @NotNull SourceElement source,
            boolean isExternal
    ) {
        super(storageManager, name);
        this.containingDeclaration = containingDeclaration;
        this.source = source;
        this.isExternal = isExternal;
    }

//    @Override
//    public bool isExternal() {
//        return isExternal;
//    }

    @NotNull
    @Override
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }

    @NotNull
    @Override
    public SourceElement getSource() {
        return source;
    }
}
