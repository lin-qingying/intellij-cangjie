package com.huawei.cangjie.descriptors

class ModuleCapability<T>(val name: String) {
    override fun toString() = name
}

interface ModuleDescriptor : DeclarationDescriptor{
    val isValid: Boolean


    fun <T> getCapability(capability: ModuleCapability<T>): T?
}