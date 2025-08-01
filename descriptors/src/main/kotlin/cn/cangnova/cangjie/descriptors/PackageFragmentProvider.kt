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
