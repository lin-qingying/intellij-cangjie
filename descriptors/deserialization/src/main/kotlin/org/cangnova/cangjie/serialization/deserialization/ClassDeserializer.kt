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

package org.cangnova.cangjie.serialization.deserialization

import cn.cangnova.cangjie.serialization.deserialization.DeserializedPackageFragment
import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.packageFragments
import org.cangnova.cangjie.name.ClassId
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedClassDescriptor

class ClassDeserializer(private val components: DeserializationComponents) {


    private val classes: (ClassKey) -> ClassDescriptor? =
        components.storageManager.createMemoizedFunctionWithNullableValues { key -> createClass(key) }

    // Additional ClassData parameter is needed to avoid calling ClassDataFinder#findClassData()
    // if it is already computed at the call site
    fun deserializeClass(
        classId: ClassId,
        classData: ClassData? = null
    ): ClassDescriptor? =
        classes(ClassKey(classId, classData))

    private fun createClass(key: ClassKey): ClassDescriptor? {
        val classId = key.classId
        for (factory in components.fictitiousClassDescriptorFactories) {
            factory.createClass(classId)?.let { return it }
        }
        if (classId in BLACK_LIST) return null

        val (decl, `package`, metadataVersion, sourceElement) = key.classData
            ?: components.classDataFinder.findClassData(classId)
            ?: return null

        val outerClassId = classId.outerClassId
        val outerContext = if (outerClassId != null) {
            val outerClass = deserializeClass(outerClassId) as? DeserializedClassDescriptor ?: return null

            // Find the outer class first and check if he knows anything about the nested class we're looking for
            if (!outerClass.hasNestedClass(classId.shortClassName)) return null

            outerClass.c
        } else {
            val fragments = components.packageFragmentProvider.packageFragments(classId.packageFqName)
            val fragment =
                fragments.firstOrNull { it !is DeserializedPackageFragment || it.hasTopLevelClass(classId.shortClassName) }
                    ?: return null

            components.createContext(
                fragment, `package`,


                metadataVersion,
                containerSource = null
            )
        }

        return DeserializedClassDescriptor(outerContext, decl, metadataVersion, sourceElement)

    }


}

