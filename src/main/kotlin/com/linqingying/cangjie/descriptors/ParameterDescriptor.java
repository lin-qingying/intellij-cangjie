package com.linqingying.cangjie.descriptors;

import org.jetbrains.annotations.NotNull;

public interface ParameterDescriptor extends ValueDescriptor {
    @NotNull
    @Override
    ParameterDescriptor getOriginal();
}
