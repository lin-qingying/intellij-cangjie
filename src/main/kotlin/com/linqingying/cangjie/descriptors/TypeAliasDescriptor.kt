package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.descriptors.impl.TypeAliasConstructorDescriptor
import com.linqingying.cangjie.mpp.TypeAliasSymbolMarker
import com.linqingying.cangjie.types.SimpleType


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
