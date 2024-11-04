package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.mpp.SimpleFunctionSymbolMarker

interface SimpleFunctionDescriptor : FunctionDescriptor, SimpleFunctionSymbolMarker {
    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): SimpleFunctionDescriptor

    override val original: SimpleFunctionDescriptor

    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out SimpleFunctionDescriptor>
}
