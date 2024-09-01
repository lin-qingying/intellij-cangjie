package com.huawei.cangjie.descriptors

  interface DeclarationDescriptorVisitor<R, D>{
    fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, builder: D?): R
    fun visitTypeParameterDescriptor(descriptor:  TypeParameterDescriptor, builder: D?): R

    fun visitValueParameterDescriptor(
        descriptor: ValueParameterDescriptor,
        builder: D?
    ): R
    fun visitPropertyDescriptor(descriptor:  PropertyDescriptor, builder: D?): R

    fun visitModuleDeclaration(descriptor: ModuleDescriptor, builder: D?): R
      fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor , builder: D?): R
      fun visitConstructorDescriptor(
          constructorDescriptor:  ConstructorDescriptor ,
          builder: D?
      ): R

      fun visitClassDescriptor(descriptor:  ClassDescriptor, builder: D?): R

    fun visitVariableDescriptor(descriptor:VariableDescriptor , builder: D?): R
    fun visitPackageFragmentDescriptor(
        descriptor: PackageFragmentDescriptor,
        builder: D?
    ): R
    fun visitFunctionDescriptor(descriptor: FunctionDescriptor, builder: D?): R
    fun visitReceiverParameterDescriptor(
        descriptor: ReceiverParameterDescriptor,
        builder: D?
    ): R

}
