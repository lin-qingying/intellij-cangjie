package com.huawei.cangjie.descriptors

import com.huawei.cangjie.descriptors.impl.TypeAliasConstructorDescriptor
import com.huawei.cangjie.mpp.TypeAliasSymbolMarker
import com.huawei.cangjie.types.SimpleType


interface TypeAliasDescriptor : ClassifierDescriptorWithTypeParameters, TypeAliasSymbolMarker {
    /// Right-hand side of the type alias definition.
    /// May contain type aliases.
    val underlyingType: SimpleType

    /// Fully expanded type with non-substituted type parameters.
    /// May not contain type aliases.
    val expandedType: SimpleType

    val classDescriptor: ClassDescriptor?


    override val original: TypeAliasDescriptor

    val constructors: Collection<TypeAliasConstructorDescriptor>
}
