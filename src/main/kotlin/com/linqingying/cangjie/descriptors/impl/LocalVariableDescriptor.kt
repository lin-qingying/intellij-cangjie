package com.linqingying.cangjie.descriptors.impl;

import com.linqingying.cangjie.descriptors.*;
import com.linqingying.cangjie.descriptors.annotations.Annotations;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.TypeSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

import static com.linqingying.cangjie.descriptors.CallableMemberDescriptor.Kind.DECLARATION;

public class LocalVariableDescriptor  extends VariableDescriptorImpl    {

//    public LocalVariableDescriptor(
//            @NotNull DeclarationDescriptor containingDeclaration,
//            @NotNull Annotations annotations,
//            @NotNull Name name,
//            @Nullable CangJieType type,
//            boolean mutable,
//
//            @NotNull SourceElement source
//    ) {
//        super(containingDeclaration, null,annotations,Modality.FINAL, DescriptorVisibilities.LOCAL, mutable,   name,DECLARATION, type,source);
//
//    }
    public LocalVariableDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @Nullable CangJieType type,
            boolean mutable,

            @NotNull SourceElement source
    ) {
        super( containingDeclaration,    name, type, mutable, source,DescriptorVisibilities.LOCAL);

    }



    @Override
    public @NotNull LocalVariableDescriptor substitute(@NotNull TypeSubstitutor substitutor) {
        if (substitutor.isEmpty()) return this;
        throw new UnsupportedOperationException(); // TODO
    }


//    @NotNull
//    @Override
//    public DescriptorVisibility getVisibility() {
//        return DescriptorVisibilities.LOCAL;
//    }



    @Override
    public <R, D> R accept(DeclarationDescriptorVisitor<R, D> visitor, D data) {
        return super.accept(visitor, data);
    }


}
