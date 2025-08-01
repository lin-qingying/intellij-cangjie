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

package cn.cangnova.cangjie.resolve.lazy.declarations

import cn.cangnova.cangjie.descriptors.*
import cn.cangnova.cangjie.descriptors.macro.MacroDescriptor
import cn.cangnova.cangjie.incremental.components.LookupLocation
import cn.cangnova.cangjie.incremental.components.NoLookupLocation
import cn.cangnova.cangjie.incremental.record
import cn.cangnova.cangjie.psi.CjDeclaration
import cn.cangnova.cangjie.resolve.lazy.ResolveSession
import cn.cangnova.cangjie.resolve.scopes.DescriptorKindFilter
import cn.cangnova.cangjie.resolve.scopes.LexicalScope


class LazyPackageMemberScope(
    private val resolveSession: ResolveSession,
    declarationProvider: PackageMemberDeclarationProvider,
    thisPackage: PackageFragmentDescriptor
) : AbstractLazyMemberScope<PackageFragmentDescriptor, PackageMemberDeclarationProvider>(
    resolveSession,
    declarationProvider,
    thisPackage,
    resolveSession.trace
) {
    override fun getScopeForMemberDeclarationResolution(declaration: CjDeclaration) =
        resolveSession.fileScopeProvider!!.getFileResolutionScope(declaration.getContainingCjFile())

    override fun getNonDeclaredFunctions(name: Name, result: MutableSet<SimpleFunctionDescriptor>) {

    }

    override fun getNonDeclaredClasses(name: Name, result: MutableSet<ClassDescriptor>) {
//        c.syntheticResolveExtension.generateSyntheticClasses(thisDescriptor, name, c, declarationProvider, result)

    }

    override fun recordLookup(name: Name, location: LookupLocation) {
        c.lookupTracker.record(location, thisDescriptor, name)
    }

    override fun getContributedDescriptors(
        kindFilter: DescriptorKindFilter,
        nameFilter: (Name) -> Boolean
    ): Collection<DeclarationDescriptor> {
        return computeDescriptorsFromDeclaredElements(
            kindFilter,
            nameFilter,
            NoLookupLocation.MATCH_GET_ALL_DESCRIPTORS
        )

    }

    override fun getNonDeclaredMacros(name: Name, result: MutableSet<MacroDescriptor>) {

    }

    override fun getScopeForInitializerResolution(declaration: CjDeclaration): LexicalScope =
        getScopeForMemberDeclarationResolution(declaration)


    override fun getNonDeclaredProperties(name: Name, result: MutableSet<PropertyDescriptor>) {
        // No extra properties
    }

    override fun getNonDeclaredVariables(name: Name, result: MutableSet<VariableDescriptor>) {

    }

    override fun getContributedPackageView(name: Name, location: LookupLocation): PackageViewDescriptor? {


        val packageView = thisDescriptor.containingDeclaration.getPackage(thisDescriptor.fqName.child(name))
        return if (packageView.isEmpty()) null else packageView

    }

    // Do not add details here, they may compromise the laziness during debugging
    override fun toString() = "lazy scope for package " + thisDescriptor.name
}
