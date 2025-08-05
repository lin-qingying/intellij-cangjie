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
import cn.cangnova.cangjie.descriptors.annotations.Annotations
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.psi.CjImportInfo
import cn.cangnova.cangjie.psi.CjPackageDirective
import cn.cangnova.cangjie.resolve.scopes.ChainedMemberScope
import cn.cangnova.cangjie.resolve.scopes.LazyScopeAdapter
import cn.cangnova.cangjie.resolve.scopes.MemberScope
import cn.cangnova.cangjie.storage.StorageManager
import cn.cangnova.cangjie.storage.getValue


class LazyPackageViewDescriptorImpl(
    override val module: ModuleDescriptorImpl, override val fqName: FqName, val storageManager: StorageManager
) : DeclarationDescriptorImpl(Annotations.EMPTY, fqName.shortNameOrSpecial()), PackageViewDescriptor {
    override fun <R, D> accept(visitor: DeclarationDescriptorVisitor<R, D>, data: D?): R =
        visitor.visitPackageViewDescriptor(this, data!!)


    protected val empty: Boolean by storageManager.createLazyValue {
        module.packageFragmentProvider.isEmpty(fqName)
    }

    override fun isEmpty(): Boolean = empty


    private val reexportPackageView: MutableMap<CjImportInfo, PackageViewDescriptor> = mutableMapOf()
    var packageDirectives: MutableList<CjPackageDirective> = mutableListOf(


    )


    private var _visibility: DescriptorVisibility? = null





    override val visibility: DescriptorVisibility
        get() {
            if (_visibility != null) {
                return _visibility!!
            }

            return DescriptorVisibilities.PUBLIC
        }


    override val containingDeclaration: PackageViewDescriptor?
        get() = if (fqName.isRoot) null else module.getPackage(fqName.parent())
    override val memberScope: MemberScope = LazyScopeAdapter(storageManager) {
        if (isEmpty()) {
            MemberScope.Empty
        } else {
            // Packages from SubpackagesScope are got via getContributedDescriptors(DescriptorKindFilter.PACKAGES, MemberScope.ALL_NAME_FILTER)
            val scopes =
                fragments.map { it.getMemberScope() } /*+  SubpackagesScope(module, fqName)*/ + if (this.module.name.asString() != "<built-ins module>") {
                    SubpackagesScope(module, fqName)
                } else {
                    MemberScope.Empty

                }
            ChainedMemberScope.create("package view scope for $fqName in ${module.name}", scopes)
        }
    }
    override val fragments: List<PackageFragmentDescriptor> by storageManager.createLazyValue {
        module.packageFragmentProvider.packageFragments(fqName)
    }

}




