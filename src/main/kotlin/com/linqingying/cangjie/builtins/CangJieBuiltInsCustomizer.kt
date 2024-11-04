package com.linqingying.cangjie.builtins

import com.linqingying.cangjie.descriptors.ClassConstructorDescriptor
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.SimpleFunctionDescriptor
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.serialization.deserialization.AdditionalClassPartsProvider
import com.linqingying.cangjie.serialization.deserialization.PlatformDependentDeclarationFilter
import com.linqingying.cangjie.storage.StorageManager
import com.linqingying.cangjie.types.CangJieType

class CangJieBuiltInsCustomizer(
    private val moduleDescriptor: ModuleDescriptor,
    storageManager: StorageManager,
): AdditionalClassPartsProvider, PlatformDependentDeclarationFilter {
    override fun getSupertypes(classDescriptor: ClassDescriptor): Collection<CangJieType> {
        val fqName = classDescriptor.fqNameUnsafe
        return when {

            else -> listOf()
        }
    }

    override fun getFunctions(name: Name, classDescriptor: ClassDescriptor): Collection<SimpleFunctionDescriptor> {
  return emptyList()
    }

    override fun getConstructors(classDescriptor: ClassDescriptor): Collection<ClassConstructorDescriptor> {
        return emptyList()

    }

    override fun getFunctionsNames(classDescriptor: ClassDescriptor): Collection<Name> {
        return emptyList()

    }

    override fun isFunctionAvailable(
        classDescriptor: ClassDescriptor,
        functionDescriptor: SimpleFunctionDescriptor
    ): Boolean {
        return false

    }
}
