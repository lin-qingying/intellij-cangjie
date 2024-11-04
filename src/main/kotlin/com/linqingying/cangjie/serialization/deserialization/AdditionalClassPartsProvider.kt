package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.ClassConstructorDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.SimpleFunctionDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.types.CangJieType

interface AdditionalClassPartsProvider {
    fun getSupertypes(classDescriptor: ClassDescriptor): Collection<CangJieType>
    fun getFunctions(name: Name, classDescriptor: ClassDescriptor): Collection<SimpleFunctionDescriptor>
    fun getConstructors(classDescriptor: ClassDescriptor): Collection<ClassConstructorDescriptor>
    fun getFunctionsNames(classDescriptor: ClassDescriptor): Collection<Name>

    object None : AdditionalClassPartsProvider {
        override fun getSupertypes(classDescriptor: ClassDescriptor): Collection<CangJieType> = emptyList()
        override fun getFunctions(name: Name, classDescriptor: ClassDescriptor): Collection<SimpleFunctionDescriptor> = emptyList()
        override fun getFunctionsNames(classDescriptor: ClassDescriptor): Collection<Name> = emptyList()
        override fun getConstructors(classDescriptor: ClassDescriptor): Collection<ClassConstructorDescriptor> = emptyList()
    }
}
