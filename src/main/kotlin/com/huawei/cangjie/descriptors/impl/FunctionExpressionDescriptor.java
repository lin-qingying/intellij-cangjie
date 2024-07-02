package com.huawei.cangjie.descriptors.impl;

import com.huawei.cangjie.descriptors.DeclarationDescriptor;
import com.huawei.cangjie.descriptors.SourceElement;
import com.huawei.cangjie.descriptors.annotations.Annotations;
import com.huawei.cangjie.name.Name;
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
