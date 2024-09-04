package com.huawei.cangjie.descriptors.impl;


import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.FunctionDescriptor;
import com.huawei.cangjie.descriptors.SimpleFunctionDescriptor;
import com.huawei.cangjie.descriptors.SourceElement;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
import com.huawei.cangjie.name.SpecialNames;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AnonymousFunctionDescriptor extends SimpleFunctionDescriptorImpl {

    public AnonymousFunctionDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Kind kind,
            @NotNull SourceElement source

    ) {
        this(containingDeclaration, null, annotations, SpecialNames.ANONYMOUS, kind, source );
    }

    private AnonymousFunctionDescriptor(
            @NotNull DeclarationDescriptor declarationDescriptor,
            @Nullable SimpleFunctionDescriptor original,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull SourceElement source

    ) {
        super(declarationDescriptor, original, annotations, name, kind, source);

    }

    @NotNull
    @Override
    protected FunctionDescriptorImpl createSubstitutedCopy(
            @NotNull DeclarationDescriptor newOwner,
            @Nullable FunctionDescriptor original,
            @NotNull Kind kind,
            @Nullable Name newName,
            @NotNull Annotations annotations,
            @NotNull SourceElement source
    ) {
        return new AnonymousFunctionDescriptor(
                newOwner,
                (SimpleFunctionDescriptor) original,
                annotations,
                newName != null ? newName : getName(),
                kind,
                source

        );
    }


}
