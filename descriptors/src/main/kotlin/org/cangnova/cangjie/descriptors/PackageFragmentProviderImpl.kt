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

package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.name.FqName
import org.cangnova.cangjie.name.Name

class PackageFragmentProviderImpl(
    private val packageFragments: Collection<PackageFragmentDescriptor> = emptyList()
) : PackageFragmentProviderOptimized {
    override fun collectPackageFragments(
        fqName: FqName,
        packageFragments: MutableCollection<PackageFragmentDescriptor>
    ) {
        this.packageFragments.filterTo(packageFragments ) { it.fqName == fqName }

    }

    override fun isEmpty(fqName: FqName): Boolean {
        return this.packageFragments.none { it.fqName == fqName }

    }

    @Deprecated("for usages use #packageFragments(FqName) at final point, for impl use #collectPackageFragments(FqName, MutableCollection<PackageFragmentDescriptor>)")
    override fun getPackageFragments(fqName: FqName): List<PackageFragmentDescriptor> {
        return packageFragments.filter { it.fqName == fqName }

    }

    override fun getSubPackagesOf(fqName: FqName, nameFilter: (Name) -> Boolean): Collection<FqName> {
        return packageFragments.asSequence()
            .map { it.fqName }
            .filter { !it.isRoot && it.parent() == fqName }
            .toList()
    }
}
