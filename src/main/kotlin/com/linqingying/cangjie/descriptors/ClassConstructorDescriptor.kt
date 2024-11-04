package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.mpp.ConstructorSymbolMarker
import com.linqingying.cangjie.types.TypeSubstitutor

interface ClassConstructorDescriptor : ConstructorDescriptor, ConstructorSymbolMarker {

    override val containingDeclaration: ClassDescriptor



    override val original: ClassConstructorDescriptor


    override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor?

    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): ClassConstructorDescriptor
}
