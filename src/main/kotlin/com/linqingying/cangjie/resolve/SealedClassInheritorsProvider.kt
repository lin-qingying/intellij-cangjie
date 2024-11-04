package com.linqingying.cangjie.resolve

import com.linqingying.cangjie.descriptors.ClassDescriptor
import com.linqingying.cangjie.descriptors.Modality
import com.linqingying.cangjie.descriptors.PackageFragmentDescriptor
import com.linqingying.cangjie.descriptors.TypeAliasDescriptor
import com.linqingying.cangjie.incremental.components.NoLookupLocation
import com.linqingying.cangjie.resolve.descriptorUtil.fqNameSafe
import com.linqingying.cangjie.resolve.descriptorUtil.parents
import com.linqingying.cangjie.resolve.scopes.DescriptorKindFilter
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.types.TypeRefinement

abstract class SealedClassInheritorsProvider {
    /**
     * This method may be called by compiler only for classes/interfaces with sealed modality
     */
    abstract fun computeSealedSubclasses(
        sealedClass: ClassDescriptor,
        allowSealedInheritorsInDifferentFilesOfSamePackage: Boolean
    ): Collection<ClassDescriptor>
}
object CliSealedClassInheritorsProvider : SealedClassInheritorsProvider() {
    // Note this is a generic and slow implementation which would work almost for any subclass of ClassDescriptor.
    // Please avoid using it in new code.
    // TODO: do something more clever instead at call sites of this function
    @OptIn(TypeRefinement::class)
    override fun computeSealedSubclasses(
        sealedClass: ClassDescriptor,
        allowSealedInheritorsInDifferentFilesOfSamePackage: Boolean
    ): Collection<ClassDescriptor> {
        if (sealedClass.modality != Modality.SEALED) return emptyList()

        val result = linkedSetOf<ClassDescriptor>()

        fun collectSubclasses(scope: MemberScope, collectNested: Boolean) {
            for (descriptor in scope.getContributedDescriptors(DescriptorKindFilter.CLASSIFIERS)) {
                if (descriptor !is ClassDescriptor) continue
                /*
                 * scope.getContributedDescriptors() doesn't discriminate expect classes in presence
                 *   of theirs actuals, so we need to lookup for descriptor once again via
                 *   scope.getContributedClassifier() to match expects (if possible)
                 */
                val refinedDescriptor = if (descriptor.isExpect) {
                    when (val actualDescriptor = scope.getContributedClassifier(descriptor.name, NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS)) {
                        is ClassDescriptor -> actualDescriptor
                        is TypeAliasDescriptor -> actualDescriptor.classDescriptor
                        else -> null
                    }
                } else {
                    descriptor
                } ?: continue

                if (DescriptorUtils.isDirectSubclass(refinedDescriptor, sealedClass)) {
                    result.add(refinedDescriptor)
                }

                if (collectNested) {
                    collectSubclasses(refinedDescriptor.unsubstitutedInnerClassesScope, collectNested)
                }
            }
        }

        val container = if (!allowSealedInheritorsInDifferentFilesOfSamePackage) {
            sealedClass.containingDeclaration
        } else {
            sealedClass.parents.firstOrNull { it is PackageFragmentDescriptor }
        }
        if (container is PackageFragmentDescriptor) {
            collectSubclasses(
                container.getMemberScope(),
                collectNested = allowSealedInheritorsInDifferentFilesOfSamePackage
            )
        }
        collectSubclasses(sealedClass.unsubstitutedInnerClassesScope, collectNested = true)
        return result.sortedBy { it.fqNameSafe.asString() }
    }

}
