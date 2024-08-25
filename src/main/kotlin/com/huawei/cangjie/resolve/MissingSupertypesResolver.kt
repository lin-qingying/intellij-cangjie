package com.huawei.cangjie.resolve

import com.huawei.cangjie.descriptors.*
import com.huawei.cangjie.resolve.descriptorUtil.classId
import com.huawei.cangjie.storage.StorageManager
import com.huawei.cangjie.types.util.supertypes

class MissingSupertypesResolver(
    storageManager: StorageManager,
    private val moduleDescriptor: ModuleDescriptor
)
{
    fun getMissingSuperClassifiers(descriptor: ClassifierDescriptor) = missingClassifiers(descriptor)

    private val missingClassifiers = storageManager.createMemoizedFunction { classifier: ClassifierDescriptor ->
        doGetMissingClassifiers(classifier)
    }


    private fun doGetMissingClassifiers(descriptor: ClassifierDescriptor): Set<ClassifierDescriptor> {
        val missingSuperClassifiers = mutableSetOf<ClassifierDescriptor>()
        val type = descriptor.defaultType

        for (supertype in type.supertypes()) {
            val supertypeDeclaration = supertype.constructor.declarationDescriptor

            /*
            * TODO: expects are not checked, because findClassAcrossModuleDependencies does not work with actualization via type alias
            * Type parameters are skipped here in favor to explicit checks for bounds, local declarations are ignored for optimization
            */
            if (supertypeDeclaration !is ClassDescriptor || supertypeDeclaration.isExpect) continue
            if (supertypeDeclaration.visibility == DescriptorVisibilities.LOCAL) continue

            val superTypeClassId = supertypeDeclaration.classId ?: continue
            val dependency = moduleDescriptor.findClassAcrossModuleDependencies(superTypeClassId)

            if (dependency == null || dependency is NotFoundClasses.MockClassDescriptor) {
                missingSuperClassifiers.add(supertypeDeclaration)
            }
        }

        return missingSuperClassifiers.toSet()
    }
}
