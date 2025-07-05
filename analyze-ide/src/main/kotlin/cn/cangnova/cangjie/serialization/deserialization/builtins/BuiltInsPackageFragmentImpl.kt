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

package cn.cangnova.cangjie.serialization.deserialization.builtins

import cn.cangnova.cangjie.builtins.BuiltInsPackageFragment
import cn.cangnova.cangjie.descriptors.DeclarationDescriptor
import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.metadata.ProtoBuf
import cn.cangnova.cangjie.metadata.builtins.BuiltInsBinaryVersion
import cn.cangnova.cangjie.metadata.builtins.readBuiltinsPackageFragment
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.resolve.DescriptorUtils
import cn.cangnova.cangjie.serialization.deserialization.DeserializedPackageFragmentImpl
import cn.cangnova.cangjie.storage.StorageManager
import java.io.InputStream

class BuiltInsPackageFragmentImpl private constructor(
    fqName: FqName,
    storageManager: StorageManager,
    module: ModuleDescriptor,
    proto: ProtoBuf.PackageFragment,
    metadataVersion: BuiltInsBinaryVersion,
    override val isFallback: Boolean
) : BuiltInsPackageFragment, DeserializedPackageFragmentImpl(
    fqName, storageManager, module, proto, metadataVersion, containerSource = null
) {
    companion object {
        fun create(
            fqName: FqName,
            storageManager: StorageManager,
            module: ModuleDescriptor,
            inputStream: InputStream,
            isFallback: Boolean
        ): BuiltInsPackageFragmentImpl {
            val (proto, version) = inputStream.readBuiltinsPackageFragment()

            if (proto == null) {
                // TODO: report a proper diagnostic
                throw UnsupportedOperationException(
                    "CangJie built-in definition format version is not supported: " +
                            "expected ${BuiltInsBinaryVersion.INSTANCE}, actual $version. " +
                            "Please update CangJie"
                )
            }

            return BuiltInsPackageFragmentImpl(fqName, storageManager, module, proto, version, isFallback)
        }
    }

    override fun toString(): String = "builtins package fragment for $fqName from $module"
}

val DeclarationDescriptor.module: ModuleDescriptor
    get() = DescriptorUtils.getContainingModule(this)
