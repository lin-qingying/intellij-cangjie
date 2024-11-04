package com.linqingying.cangjie.resolve.caches

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.resolve.SealedClassInheritorsProvider

object IdeSealedClassInheritorsProvider : SealedClassInheritorsProvider() {
    override fun computeSealedSubclasses(
        sealedClass: ClassDescriptor,
        allowSealedInheritorsInDifferentFilesOfSamePackage: Boolean
    ): Collection<ClassDescriptor> {
        return emptyList()
    }
}
