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

package cn.cangnova.cangjie.serialization

import cn.cangnova.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import cn.cangnova.cangjie.metadata.serialization.StringTable
import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.renderer.DescriptorRenderer
import cn.cangnova.cangjie.resolve.descriptorUtil.classId
import cn.cangnova.cangjie.types.ErrorUtils


interface DescriptorAwareStringTable : StringTable {
    fun getQualifiedClassNameIndex(classId: ClassId): Int =
        getQualifiedClassNameIndex(classId.asString(), classId.isLocal)

    fun getFqNameIndex(descriptor: ClassifierDescriptorWithTypeParameters): Int {
        if (ErrorUtils.isError(descriptor)) {
            throw IllegalStateException("Cannot get FQ name of error class: ${renderDescriptor(descriptor)}")
        }

        val classId = descriptor.classId
            ?: getLocalClassIdReplacement(descriptor)
            ?: throw IllegalStateException("Cannot get FQ name of local class: ${renderDescriptor(descriptor)}")

        return getQualifiedClassNameIndex(classId)
    }

    fun getLocalClassIdReplacement(descriptor: ClassifierDescriptorWithTypeParameters): ClassId? = null

    /**
     * true if this [StringTable] replaces absent [ClassId] of a local class descriptor with a semantic equivalent that
     * still expects original type arguments when used in a type
     * false otherwise
     */
    val isLocalClassIdReplacementKeptGeneric: Boolean
        get() = false

    private fun renderDescriptor(descriptor: ClassifierDescriptorWithTypeParameters): String =
        DescriptorRenderer.COMPACT.render(descriptor) + " defined in " +
                DescriptorRenderer.FQ_NAMES_IN_TYPES.render(descriptor.containingDeclaration)
}
