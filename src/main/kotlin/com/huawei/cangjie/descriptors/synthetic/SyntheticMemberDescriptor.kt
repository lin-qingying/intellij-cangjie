package com.huawei.cangjie.descriptors.synthetic

import com.huawei.cangjie.descriptors.ClassDescriptor
import com.huawei.cangjie.descriptors.DeclarationDescriptor
import com.huawei.cangjie.descriptors.FunctionDescriptor


interface SyntheticMemberDescriptor<out T : DeclarationDescriptor> {
    val baseDescriptorForSynthetic: T
}

interface FunctionInterfaceConstructorDescriptor : SyntheticMemberDescriptor<ClassDescriptor>

interface FunctionInterfaceAdapterDescriptor<out T : FunctionDescriptor> : SyntheticMemberDescriptor<T>

interface FunctionInterfaceAdapterExtensionFunctionDescriptor : SyntheticMemberDescriptor<FunctionDescriptor>
