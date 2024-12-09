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

package com.linqingying.cangjie.descriptors

import com.linqingying.cangjie.config.LanguageVersionSettings
import com.linqingying.cangjie.name.FqName
import com.linqingying.cangjie.name.Name
import com.linqingying.cangjie.resolve.lazy.declarations.DeclarationProvider
import com.linqingying.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider
import com.linqingying.cangjie.resolve.scopes.MemberScope


interface ClassOrPackageFragmentDescriptor : DeclarationDescriptorNonRoot

/**
 * 表示包的数据层
 */
interface PackageFragmentDescriptor : PackageData, ClassOrPackageFragmentDescriptor {

    fun getMemberScope(): MemberScope


    override val containingDeclaration: ModuleDescriptor

    val declarationProvider: DeclarationProvider get() = DeclarationProvider.EMPTY
}

interface PackageData : DeclarationDescriptor {
    val fqName: FqName


    fun shouldSeeInternalsOf(whatPackage: PackageData): Boolean {
//判断自己是不是 whatPackage 的子包或本包


//        val f1 = FqName.topLevel(Name.identifier("a"))
//        val f2 = f1.child(Name.identifier("b"))

        if (this.fqName == whatPackage.fqName.parent()) return true
        return this.fqName.startsWith(whatPackage.fqName)

    }

    fun shouldSeeInternalsOf(whatPackage: PackageFragmentDescriptor): Boolean {
//判断自己是不是 whatPackage 的子包或本包


//        val f1 = FqName.topLevel(Name.identifier("a"))
//        val f2 = f1.child(Name.identifier("b"))

        return this.fqName.startsWith(whatPackage.fqName)

    }

    fun shouldProtectedsOf(whatPackage: PackageData): Boolean {
// 判断模块名是否相同
        return this.fqName.moduleName == whatPackage.fqName.moduleName

    }
}

/**
 * 表示包的视图层
 */
interface PackageViewDescriptor : PackageData {

    fun getContributedDescriptorsByReexportDirective(
        languageVersionSettings: LanguageVersionSettings,
        packageFragment: PackageFragmentDescriptor?,
        declaredName: Name,

        aliasName: Name
    ): Collection<DeclarationDescriptor> {

        return emptyList()
    }

    val reexportTop: Boolean get() =  true
    override val containingDeclaration: PackageViewDescriptor?

    val memberScope: MemberScope

    val module: ModuleDescriptor

    val fragments: List<PackageFragmentDescriptor>

    fun isEmpty(): Boolean = fragments.isEmpty()
}
