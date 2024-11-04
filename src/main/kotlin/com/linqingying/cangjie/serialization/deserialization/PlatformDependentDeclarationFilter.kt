package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.builtins.StandardNames
import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.SimpleFunctionDescriptor
import com.linqingying.cangjie.name.FqName

interface PlatformDependentDeclarationFilter {

    fun isFunctionAvailable(classDescriptor: ClassDescriptor, functionDescriptor: SimpleFunctionDescriptor): Boolean

    object All : PlatformDependentDeclarationFilter {
        override fun isFunctionAvailable(classDescriptor: ClassDescriptor, functionDescriptor: SimpleFunctionDescriptor) = true
    }

    object NoPlatformDependent : PlatformDependentDeclarationFilter {
        override fun isFunctionAvailable(classDescriptor: ClassDescriptor, functionDescriptor: SimpleFunctionDescriptor) =
            !functionDescriptor.annotations.hasAnnotation(PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME)
    }
}

val PLATFORM_DEPENDENT_ANNOTATION_FQ_NAME: FqName = StandardNames.FqNames.platformDependent
