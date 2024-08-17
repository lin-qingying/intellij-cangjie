package com.huawei.cangjie.descriptors.impl;

import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.DeclarationDescriptorNonRoot;
import com.huawei.cangjie.descriptors.DeclarationDescriptorWithSource;
import com.huawei.cangjie.descriptors.SourceElement;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
import org.jetbrains.annotations.NotNull;

public abstract class DeclarationDescriptorNonRootImpl extends DeclarationDescriptorImpl
        implements DeclarationDescriptorNonRoot {
    @NotNull
    private final DeclarationDescriptor containingDeclaration;
    @Override
    @NotNull
    public DeclarationDescriptor getContainingDeclaration() {
        return containingDeclaration;
    }
    @NotNull
    private final SourceElement source;
    protected DeclarationDescriptorNonRootImpl(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull SourceElement source
    ) {
        super(annotations, name);

        this.containingDeclaration = containingDeclaration;
        this.source = source;
    }
    @NotNull
    @Override
    public DeclarationDescriptorWithSource getOriginal() {
        return (DeclarationDescriptorWithSource) super.getOriginal();
    }
    @Override
    public void validate() {
        containingDeclaration.validate();
    }
    @Override
    @NotNull
    public SourceElement getSource() {
        return source;
    }
}
