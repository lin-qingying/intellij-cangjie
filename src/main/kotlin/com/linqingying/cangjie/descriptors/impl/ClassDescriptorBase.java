package com.linqingying.cangjie.descriptors.impl;


import com.linqingying.cangjie.descriptors.DeclarationDescriptor;
import com.linqingying.cangjie.descriptors.SourceElement;
import com.linqingying.cangjie.storage.StorageManager;

import org.jetbrains.annotations.NotNull;
import com.linqingying.cangjie.name.Name;

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
