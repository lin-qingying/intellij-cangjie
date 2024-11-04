package com.linqingying.cangjie.builtins

import com.linqingying.cangjie.container.DefaultImplementation
import com.linqingying.cangjie.descriptors.ClassDescriptor


@DefaultImplementation(impl = PlatformToCangJieClassMapper.Default::class)
interface PlatformToCangJieClassMapper {
    fun mapPlatformClass(classDescriptor: ClassDescriptor): Collection<ClassDescriptor>

    class Default : PlatformToCangJieClassMapper {
        override fun mapPlatformClass(classDescriptor: ClassDescriptor): Collection<ClassDescriptor> {
            return emptyList()
        }
    }
}
