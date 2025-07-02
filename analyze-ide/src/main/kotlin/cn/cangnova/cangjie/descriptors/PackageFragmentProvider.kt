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

package cn.cangnova.cangjie.descriptors

import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name
import java.util.ArrayList
import java.util.HashSet


interface PackageFragmentProviderOptimized : PackageFragmentProvider {
    fun collectPackageFragments(fqName: FqName, packageFragments: MutableCollection<PackageFragmentDescriptor>)
    fun isEmpty(fqName: FqName): Boolean
}

interface PackageFragmentProvider {
    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor>

    fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName>

    object Empty : PackageFragmentProvider {
        @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)",
            ReplaceWith("emptyList<PackageFragmentDescriptor>()")
        )
        override fun getPackageFragments(fqName: FqName) = emptyList<PackageFragmentDescriptor>()

        override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean) = emptySet<FqName>()
    }
}

fun PackageFragmentProvider.collectPackageFragmentsOptimizedIfPossible(
    fqName: FqName,
    packageFragments: MutableCollection<PackageFragmentDescriptor>
) {
    when (this) {
        is PackageFragmentProviderOptimized -> collectPackageFragments(fqName, packageFragments)
        else -> packageFragments.addAll(@Suppress("DEPRECATION") getPackageFragments(fqName))
    }
}

class CompositePackageFragmentProvider(// can be modified from outside
    private val providers: List<PackageFragmentProvider>,
    private val debugName: String
) : PackageFragmentProviderOptimized {
    override fun collectPackageFragments(
        fqName: FqName,
        packageFragments: MutableCollection<PackageFragmentDescriptor>
    ) {
        for (provider in providers) {
            provider.collectPackageFragmentsOptimizedIfPossible(fqName, packageFragments)
        }
    }

    override fun isEmpty(fqName: FqName): Boolean {

        return providers.all { it.isEmpty(fqName) }
    }

    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        val result = ArrayList<PackageFragmentDescriptor>()
        for (provider in providers) {
            provider.collectPackageFragmentsOptimizedIfPossible(fqName, result)
        }
        return result.toList()
    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        val result = HashSet<FqName>()
        for (provider in providers) {
            result.addAll(provider.getSubPackagesOf(fqName, nameFilter))
        }
        return result
    }
    override fun toString(): String = debugName
}

fun PackageFragmentProvider.packageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
    val packageFragments = mutableListOf<PackageFragmentDescriptor>()
    collectPackageFragmentsOptimizedIfPossible(fqName, packageFragments)
    return packageFragments
}
fun PackageFragmentProvider.isEmpty(fqName: FqName): Boolean {
    return when (this) {
        is PackageFragmentProviderOptimized -> isEmpty(fqName)
        else -> packageFragments(fqName).isEmpty()
    }
}
