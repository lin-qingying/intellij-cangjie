package com.huawei.cangjie.descriptors

import com.huawei.cangjie.mpp.ConstructorSymbolMarker
import com.huawei.cangjie.types.TypeSubstitutor

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
