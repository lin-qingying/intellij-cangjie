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

package com.linqingying.cangjie.serialization.deserialization

import com.linqingying.cangjie.descriptors.ModuleDescriptor
import com.linqingying.cangjie.descriptors.SourceElement
import com.linqingying.cangjie.metadata.ProtoBuf
import com.linqingying.cangjie.metadata.deserialization.BinaryVersion
import com.linqingying.cangjie.metadata.deserialization.NameResolverImpl
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.resolve.scopes.MemberScope
import com.linqingying.cangjie.serialization.deserialization.descriptors.DeserializedContainerSource
import com.linqingying.cangjie.serialization.deserialization.descriptors.DeserializedPackageMemberScope
import com.linqingying.cangjie.storage.StorageManager


abstract class DeserializedPackageFragmentImpl(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    proto: ProtoBuf.PackageFragment,
    private val metadataVersion: BinaryVersion,
    private val containerSource: DeserializedContainerSource?
) : DeserializedPackageFragment(fqName, storageManager, module) {
    protected val nameResolver = NameResolverImpl(proto.strings, proto.qualifiedNames)

    override val classDataFinder =
        ProtoBasedClassDataFinder(proto, nameResolver, metadataVersion) { containerSource ?: SourceElement.NO_SOURCE }

    // Temporary storage: until `initialize` is called
    private var _proto: ProtoBuf.PackageFragment? = proto
    private lateinit var _memberScope: MemberScope

    override fun initialize(components: DeserializationComponents) {
        val proto = _proto ?: error("Repeated call to DeserializedPackageFragmentImpl::initialize")
        _proto = null
        _memberScope = DeserializedPackageMemberScope(
            this, proto.`package`, nameResolver, metadataVersion, containerSource, components,
            "scope of $this"
        ) {
            classDataFinder.allClassIds.filter { classId ->
                !classId.isNestedClass && classId !in ClassDeserializer.BLACK_LIST
            }.map { it.shortClassName }
        }

    }

    override fun getMemberScope(): MemberScope = _memberScope
}
