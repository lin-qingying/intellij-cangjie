package com.linqingying.cangjie.descriptors;

import com.linqingying.cangjie.mpp.ClassLikeSymbolMarker;
import com.linqingying.cangjie.mpp.ClassifierSymbolMarker;
import com.linqingying.cangjie.utils.ReadOnly;
import org.jetbrains.annotations.NotNull;

import java.util.List;



public interface ClassifierDescriptorWithTypeParameters
        extends ClassifierDescriptor, DeclarationDescriptorWithVisibility, MemberDescriptor,
        Substitutable<ClassifierDescriptorWithTypeParameters>, ClassLikeSymbolMarker, ClassifierSymbolMarker {
    /**
     * @return <code>true</code> if this class contains a reference to its outer class (as opposed to static nested class)
     */
   default boolean isInner() {

       return false;
   }


    @ReadOnly
    @NotNull
    List<TypeParameterDescriptor> getDeclaredTypeParameters();
}
