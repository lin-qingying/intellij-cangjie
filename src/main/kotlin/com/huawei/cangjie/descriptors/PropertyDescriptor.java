package com.huawei.cangjie.descriptors;


import com.huawei.cangjie.mpp.PropertySymbolMarker;
import com.huawei.cangjie.types.CangJieType;
import com.huawei.cangjie.types.TypeSubstitutor;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public interface PropertyDescriptor extends VariableDescriptorWithAccessors, PropertySymbolMarker , CallableMemberDescriptor{
//    @Override
//    @Nullable
//    PropertyGetterDescriptor getGetter();
//
//    @Override
//    @Nullable
//    PropertySetterDescriptor getSetter();

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

//    @Nullable
//    FieldDescriptor getBackingField();
//
//    @Nullable
//    FieldDescriptor getDelegateField();

    @Override
    PropertyDescriptor substitute(@NotNull TypeSubstitutor substitutor);

    @NotNull
    @Override
    CopyBuilder<? extends PropertyDescriptor> newCopyBuilder();

    @Nullable
    CangJieType getInType();
}
