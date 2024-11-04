package com.linqingying.cangjie.descriptors.synthetic

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.DeclarationDescriptor
import com.linqingying.cangjie.descriptors.FunctionDescriptor


interface SyntheticMemberDescriptor<out T : DeclarationDescriptor> {
    val baseDescriptorForSynthetic: T
}

interface FunctionInterfaceConstructorDescriptor : SyntheticMemberDescriptor<ClassDescriptor>

interface FunctionInterfaceAdapterDescriptor<out T : FunctionDescriptor> : SyntheticMemberDescriptor<T>

interface FunctionInterfaceAdapterExtensionFunctionDescriptor : SyntheticMemberDescriptor<FunctionDescriptor>
