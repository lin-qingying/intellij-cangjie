/*
 * Copyright 2024 LinQingYing. and contributors.
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

package cn.cangnova.cangjie.resolve

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.resolve.descriptorUtil.classId
import cn.cangnova.cangjie.types.util.supertypes

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
