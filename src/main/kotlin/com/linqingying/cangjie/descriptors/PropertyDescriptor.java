package com.linqingying.cangjie.descriptors;


import com.linqingying.cangjie.descriptors.impl.PropertyAccessorDescriptor;
import com.linqingying.cangjie.mpp.PropertySymbolMarker;
import com.linqingying.cangjie.resolve.constants.ConstantValue;
import com.linqingying.cangjie.types.CangJieType;
import com.linqingying.cangjie.types.TypeSubstitutor;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public interface PropertyDescriptor extends PropertyDescriptorWithAccessors, PropertySymbolMarker , CallableMemberDescriptor{
    @Override
    @Nullable
    PropertyGetterDescriptor getGetter();

    @Override
    @Nullable
    PropertySetterDescriptor getSetter();

    @NotNull
    List<PropertyAccessorDescriptor> getAccessors();

    /**
     * In the following case, the setter is projected out:
     *
     *     trait Tr<T> { var v: T }
     *     fun test(tr: Tr<out String>) {
     *         tr.v = null!! // the assignment is illegal, although a read would be fine
     *     }
     */
    boolean isSetterProjectedOut();
//    @Override
//    @Nullable
//    PropertySetterDescriptor getSetter();
//    @NotNull
//    List<PropertyAccessorDescriptor> getAccessors();

    @NotNull
    @Override
    PropertyDescriptor getOriginal();

    @NotNull
    @Override
    Collection<? extends PropertyDescriptor> getOverriddenDescriptors();

    @Nullable
    ConstantValue<?> getCompileTimeInitializer();


    @Override
    PropertyDescriptor substitute(@NotNull TypeSubstitutor substitutor);

    @NotNull
    @Override
    CopyBuilder<? extends PropertyDescriptor> newCopyBuilder();

    @Nullable
    CangJieType getInType();
}
