/*
 * Copyright 2025 LinQingYing. and contributors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 * The use of this source code is governed by the Apache License 2.0,
 * which allows users to freely use, modify, and distribute the code,
 * provided they adhere to the terms of the license.
 *
 * The software is provided "as-is", and the authors are not responsible for
 * any damages or issues arising from its use.
 *
 */

package org.cangnova.cangjie.resolve

import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.Modality
import org.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import org.cangnova.cangjie.descriptors.TypeAliasDescriptor
import org.cangnova.cangjie.incremental.components.NoLookupLocation
import org.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import org.cangnova.cangjie.resolve.scopes.MemberScope

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
                val refinedDescriptor = descriptor


                if (DescriptorUtils.isDirectSubclass(refinedDescriptor, sealedClass)) {
                    result.add(refinedDescriptor)
                }

                if (collectNested) {
                    collectSubclasses(refinedDescriptor.unsubstitutedMemberScope, collectNested)
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
        collectSubclasses(sealedClass.unsubstitutedMemberScope, collectNested = true)
        return result.sortedBy { it.fqNameSafe.asString() }
    }

}
