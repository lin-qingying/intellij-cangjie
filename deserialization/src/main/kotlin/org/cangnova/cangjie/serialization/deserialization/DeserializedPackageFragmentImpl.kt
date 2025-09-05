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

package org.cangnova.cangjie.serialization.deserialization


import cn.cangnova.cangjie.serialization.deserialization.DeserializedPackageFragment
import org.cangnova.cangjie.descriptors.ModuleDescriptor
import org.cangnova.cangjie.descriptors.SourceElement
import org.cangnova.cangjie.metadata.deserialization.BinaryVersion
import org.cangnova.cangjie.metadata.model.Package
import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import org.cangnova.cangjie.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import org.cangnova.cangjie.storage.StorageManager


abstract class DeserializedPackageFragmentImpl(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    protected val packageData: Package,
    private val metadataVersion: BinaryVersion,
    private val containerSource: DeserializedContainerSource?
) : DeserializedPackageFragment(fqName, storageManager, module) {

    override val classDataFinder =
        FlatBuffersBasedClassDataFinder(packageData, metadataVersion) { containerSource ?: SourceElement.NO_SOURCE }

    private lateinit var _memberScope: MemberScope

    override fun initialize(components: DeserializationComponents) {
        _memberScope = DeserializedPackageMemberScope(
            this, packageData, metadataVersion, containerSource, components,
            "scope of $this"
        ) {
            classDataFinder.allClassIds.filter { classId ->
                !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
            }.map { it.shortClassName }
        }
    }

    override fun getMemberScope(): MemberScope = _memberScope
}
