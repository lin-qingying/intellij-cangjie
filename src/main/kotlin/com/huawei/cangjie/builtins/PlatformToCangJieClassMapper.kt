package com.huawei.cangjie.builtins

import com.huawei.cangjie.container.DefaultImplementation
import com.huawei.cangjie.descriptors.ClassDescriptor


@DefaultImplementation(impl = PlatformToCangJieClassMapper.Default::class)
interface PlatformToCangJieClassMapper {
    fun mapPlatformClass(classDescriptor: ClassDescriptor): Collection<ClassDescriptor>

    class Default : PlatformToCangJieClassMapper {
        override fun mapPlatformClass(classDescriptor: ClassDescriptor): Collection<ClassDescriptor> {
            return emptyList()
        }
    }
}
