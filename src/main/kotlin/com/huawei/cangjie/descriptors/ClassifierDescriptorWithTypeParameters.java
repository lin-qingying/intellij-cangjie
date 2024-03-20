package com.huawei.cangjie.descriptors;

import com.huawei.cangjie.mpp.ClassLikeSymbolMarker;
import com.huawei.cangjie.mpp.ClassifierSymbolMarker;
import com.huawei.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;

import java.util.List;



public interface ClassifierDescriptorWithTypeParameters
        extends ClassifierDescriptor, DeclarationDescriptorWithVisibility, MemberDescriptor,
        Substitutable<ClassifierDescriptorWithTypeParameters>, ClassLikeSymbolMarker, ClassifierSymbolMarker {
    /**
     * @return <code>true</code> if this class contains a reference to its outer class (as opposed to static nested class)
     */
    boolean isInner();

    @ReadOnly
    @NotNull
    List<TypeParameterDescriptor> getDeclaredTypeParameters();
}
