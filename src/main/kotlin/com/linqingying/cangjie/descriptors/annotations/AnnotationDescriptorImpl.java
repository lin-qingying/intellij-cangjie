package com.linqingying.cangjie.descriptors.annotations;


import com.linqingying.cangjie.descriptors.SourceElement;
import com.linqingying.cangjie.name.FqName;
import com.linqingying.cangjie.name.Name;
import com.linqingying.cangjie.renderer.DescriptorRenderer;
import com.linqingying.cangjie.resolve.constants.ConstantValue;
import com.linqingying.cangjie.types.CangJieType;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class AnnotationDescriptorImpl implements AnnotationDescriptor {
    private final CangJieType annotationType;
    private final Map<Name, ConstantValue<?>> valueArguments;
    private final SourceElement source;

    public AnnotationDescriptorImpl(
            @NotNull CangJieType annotationType,
            @NotNull Map<Name, ConstantValue<?>> valueArguments,
            @NotNull SourceElement source
    ) {
        this.annotationType = annotationType;
        this.valueArguments = valueArguments;
        this.source = source;
    }

    @Override
    @NotNull
    public CangJieType getType() {
        return annotationType;
    }

//    @Nullable
//    @Override
//    public FqName getFqName() {
//        return AnnotationDescriptor.DefaultImpls.getFqName(this);
//    }

    @NotNull
    @Override
    public Map<Name, ConstantValue<?>> getAllValueArguments() {
        return valueArguments;
    }

    @Override
    @NotNull
    public SourceElement getSource() {
        return source;
    }

    @Override
    public String toString() {
        return DescriptorRenderer.FQ_NAMES_IN_TYPES.renderAnnotation(this, null);
    }
}
