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

package cn.cangnova.cangjie.descriptors.impl

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.incremental.components.NoLookupLocation
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.scopes.DescriptorKindExclude
import cn.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import cn.cangnova.cangjie.resolve.scopes.MemberScopeImpl
import cn.cangnova.cangjie.utils.Printer
import com.intellij.util.containers.addIfNotNull
import java.util.ArrayList


open class SubpackagesScope(private val moduleDescriptor: ModuleDescriptor, private val fqName: FqName) :
    MemberScopeImpl() {

    protected fun getPackage(name: Name): PackageViewDescriptor? {
        if (name.isSpecial) {
            return null
        }
        val packageViewDescriptor = moduleDescriptor.getPackage(fqName.child(name))
        if (packageViewDescriptor.isEmpty()) {
            return null
        }
        return packageViewDescriptor
    }



    override val classifierNames: Set<Name>
        get() =  emptySet()

    override fun definitelyDoesNotContainName(name: Name): Boolean {
        return classifierNames.contains(name)

    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        if (!kindFilter.acceptsKinds(DescriptorKindFilter.PACKAGES_MASK)) return listOf()
        if (fqName.isRoot && kindFilter.excludes.contains(DescriptorKindExclude.TopLevelPackages)) return listOf()

        val subFqNames = moduleDescriptor.getSubPackagesOf(fqName, nameFilter)
        val result = ArrayList<DeclarationDescriptor>(subFqNames.size)
        for (subFqName in subFqNames) {
            val shortName = subFqName.shortName()
            if (nameFilter(shortName)) {
                result.addIfNotNull(getPackage(shortName))
            }
        }
        return result
    }


    override fun getContributedPackages(name: Name, location: LookupLocation): Collection<PackageFragmentDescriptor> {
        TODO("Not yet implemented")
    }



    override fun printScopeStructure(p: Printer) {
        p.println(this::class.java.simpleName, " {")
        p.pushIndent()

        p.popIndent()
        p.println("}")
    }

    override fun toString(): String = "subpackages of $fqName from $moduleDescriptor"
}
