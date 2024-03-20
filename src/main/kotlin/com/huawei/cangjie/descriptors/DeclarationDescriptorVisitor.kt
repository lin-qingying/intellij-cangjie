package com.huawei.cangjie.descriptors

public interface DeclarationDescriptorVisitor<R, D>{
    fun visitVariableDescriptor(descriptor:VariableDescriptor?, data: D?): R

}