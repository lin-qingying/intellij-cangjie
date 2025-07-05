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

import cn.cangnova.cangjie.descriptors.ModuleDescriptor
import cn.cangnova.cangjie.descriptors.PackageFragmentDescriptor
import cn.cangnova.cangjie.descriptors.PackageFragmentProviderOptimized
import cn.cangnova.cangjie.metadata.decompiler.CangJieMetadataFinder
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.storage.StorageManager
import cn.cangnova.cangjie.utils.addIfNotNull

abstract class AbstractDeserializedPackageFragmentProvider(
    protected val storageManager: StorageManager,
    protected val finder: CangJieMetadataFinder,
    protected val moduleDescriptor: ModuleDescriptor
) : PackageFragmentProviderOptimized {
    protected lateinit var components: DeserializationComponents

    private val fragments = storageManager.createMemoizedFunctionWithNullableValues<FqName, PackageFragmentDescriptor> { fqName ->
        findPackage(fqName)?.apply {
            initialize(components)
        }
    }

    protected abstract fun findPackage(fqName: FqName): DeserializedPackageFragment?

    override fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>) {
        packageFragments.addIfNotNull(fragments(fqName))
    }

    override fun isEmpty(fqName: FqName): Boolean {
        val descriptor = if (fragments.isComputed(fqName)) {
            fragments.invoke(fqName)
        } else {
            findPackage(fqName)
        }
        return descriptor == null
    }

    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> = listOfNotNull(fragments.invoke(fqName))

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> = emptySet()
}
