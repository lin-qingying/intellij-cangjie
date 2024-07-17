package com.huawei.cangjie.descriptors

public interface DeclarationDescriptorVisitor<R, D>{
    fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, data: D): R
    fun visitTypeParameterDescriptor(descriptor:  TypeParameterDescriptor, data: D): R

    fun visitValueParameterDescriptor(
        descriptor: ValueParameterDescriptor,
        data: D
    ): R
    fun visitPropertyDescriptor(descriptor:  PropertyDescriptor, data: D): R

    fun visitModuleDeclaration(descriptor: ModuleDescriptor, data: D): R

    fun visitClassDescriptor(descriptor:  ClassDescriptor, data: D): R

    fun visitVariableDescriptor(descriptor:VariableDescriptor, data: D): R
    fun visitPackageFragmentDescriptor(
        descriptor: PackageFragmentDescriptor,
        data: D
    ): R
    fun visitFunctionDescriptor(descriptor: FunctionDescriptor, data: D): R
    fun visitReceiverParameterDescriptor(
        descriptor: ReceiverParameterDescriptor,
        data: D
    ): R

}
