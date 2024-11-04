package com.linqingying.cangjie.descriptors.impl;

import com.linqingying.cangjie.descriptors.DeclarationDescriptor;
import com.linqingying.cangjie.descriptors.SourceElement;
import com.linqingying.cangjie.descriptors.annotations.Annotations;
import com.linqingying.cangjie.name.Name;
import org.jetbrains.annotations.NotNull;

public class FunctionExpressionDescriptor extends SimpleFunctionDescriptorImpl {
    public FunctionExpressionDescriptor(
            @NotNull DeclarationDescriptor containingDeclaration,
            @NotNull Annotations annotations,
            @NotNull Name name,
            @NotNull Kind kind,
            @NotNull SourceElement source
    ) {
        super(containingDeclaration, null, annotations, name, kind, source);
    }
}
