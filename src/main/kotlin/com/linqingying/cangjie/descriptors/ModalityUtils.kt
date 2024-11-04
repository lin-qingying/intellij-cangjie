package com.linqingying.cangjie.descriptors


val ClassDescriptor.isFinalOrEnum: Boolean
    get() = modality == Modality.FINAL
val CallableMemberDescriptor.isOverridable: Boolean
    get() = visibility != DescriptorVisibilities.PRIVATE
            && modality != Modality.FINAL
            && (containingDeclaration as? ClassDescriptor)?.isFinalClass != true
val ClassDescriptor.isFinalClass: Boolean
    get() = modality == Modality.FINAL && kind != ClassKind.ENUM
