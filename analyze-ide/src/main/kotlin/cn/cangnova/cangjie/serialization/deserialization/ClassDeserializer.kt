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

package cn.cangnova.cangjie.serialization.deserialization

import cn.cangnova.cangjie.builtins.StandardNames
import cn.cangnova.cangjie.descriptors.ClassDescriptor
import cn.cangnova.cangjie.descriptors.SourceElement
import cn.cangnova.cangjie.descriptors.packageFragments
import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.deserialization.BinaryVersion
import cn.cangnova.cangjie.metadata.deserialization.NameResolver
import cn.cangnova.cangjie.metadata.deserialization.TypeTable
import cn.cangnova.cangjie.metadata.deserialization.VersionRequirementTable
import cn.cangnova.cangjie.name.ClassId
import cn.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedClassDescriptor

data class ClassData(
    val nameResolver: NameResolver,
    val classProto: ProtoBuf.Class,
    val metadataVersion: BinaryVersion,
    val sourceElement: SourceElement
)

class ClassDeserializer(private val components: DeserializationComponents) {
    companion object {
        /**
         * FQ names of classes that should be ignored during deserialization.
         *
         * We ignore kotlin.Cloneable because since Kotlin 1.1, the descriptor for it is created via JvmBuiltInClassDescriptorFactory,
         * but the metadata is still serialized for kotlin-reflect 1.0 to work (see BuiltInsSerializer.kt).
         */
        val BLACK_LIST = setOf(
            ClassId.topLevel(StandardNames.FqNames.cloneable.toSafe())
        )
    }
    private val classes: (ClassKey) -> ClassDescriptor? =
        components.storageManager.createMemoizedFunctionWithNullableValues { key -> createClass(key) }

    // Additional ClassData parameter is needed to avoid calling ClassDataFinder#findClassData()
    // if it is already computed at the call site
    fun deserializeClass(classId: ClassId, classData: ClassData? = null): ClassDescriptor? =
        classes(ClassKey(classId, classData))

    private fun createClass(key: ClassKey): ClassDescriptor? {
        val classId = key.classId
        for (factory in components.fictitiousClassDescriptorFactories) {
            factory.createClass(classId)?.let { return it }
        }
        if (classId in BLACK_LIST) return null

        val (nameResolver, classProto, metadataVersion, sourceElement) = key.classData
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
            val fragment = fragments.firstOrNull { it !is DeserializedPackageFragment || it.hasTopLevelClass(classId.shortClassName) }
                ?: return null

            components.createContext(
                fragment, nameResolver,
                TypeTable(classProto.typeTable),
                VersionRequirementTable.create(classProto.versionRequirementTable),
                metadataVersion,
                containerSource = null
            )
        }

        return DeserializedClassDescriptor(outerContext, classProto, nameResolver, metadataVersion, sourceElement)
    }
    private class ClassKey(val classId: ClassId, val classData: ClassData?) {
        // classData *intentionally* not used in equals() / hashCode()
        override fun equals(other: Any?) = other is ClassKey && classId == other.classId

        override fun hashCode() = classId.hashCode()
    }
}
