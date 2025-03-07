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

import cn.cangnova.cangjie.config.LanguageVersionSettings
import cn.cangnova.cangjie.name.FqName
import cn.cangnova.cangjie.name.Name
import cn.cangnova.cangjie.resolve.lazy.declarations.DeclarationProvider
import cn.cangnova.cangjie.resolve.lazy.declarations.PackageMemberDeclarationProvider
import cn.cangnova.cangjie.resolve.scopes.MemberScope


interface ClassOrPackageFragmentDescriptor : DeclarationDescriptorNonRoot


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
 * 表示 CangJie 包的全局视图，汇总了同一个包的所有片段。
 * 包视图描述符用于表示整个包的逻辑视图，即使该包跨多个模块或源文件存在。
 *
 * `PackageViewDescriptor` 包含多个 `PackageFragmentDescriptor`，这些片段组成了包的完整内容。
 * 此类在进行包级别分析时非常有用，例如分析包中的所有类和函数，检查包之间的依赖关系等。
 *
 * @property fragments 包视图中所有的包片段。
 *
 * 示例：
 * ```kotlin
 * // 假设已获取到一个 PackageViewDescriptor 实例
 * val packageView: PackageViewDescriptor = ...
 *
 * // 遍历包视图中的所有包片段并输出
 * for (fragment in packageView.getFragments()) {
 *     println("包片段: ${fragment.getContainingDeclaration()}")
 * }
 * ```
 */
interface PackageViewDescriptor : PackageData ,DeclarationDescriptorWithVisibility{

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
/**
 * 表示 CangJie 模块或源文件中的包片段。
 * 包片段是包的一部分，可以存在于不同的模块或文件中。
 *
 * 每个 `PackageFragmentDescriptor` 包含该片段中的声明，例如类、函数、属性等，
 * 这些声明属于包的特定部分。
 *
 * 此类用于分析和导航 CangJie 代码中的结构，尤其是在特定模块或文件内，
 * 提供了有关包在不同片段中如何分布的洞察。
 *
 * @property containingDeclaration 包含此包片段的模块描述符（`ModuleDescriptor`）。
 *
 * 示例：
 * ```kotlin
 * // 假设已获取到一个 PackageFragmentDescriptor 实例
 * val fragment: PackageFragmentDescriptor = ...
 *
 * // 获取该包片段所属的 ModuleDescriptor
 * val module: ModuleDescriptor = fragment.getContainingDeclaration()
 * println("该包片段所属的模块名称: ${module.name}")
 * ```
 * @see [getPackageViewDescriptor]
 */
interface PackageFragmentDescriptor : PackageData, ClassOrPackageFragmentDescriptor {

    fun getMemberScope(): MemberScope


    override val containingDeclaration: ModuleDescriptor

    val declarationProvider: DeclarationProvider get() = DeclarationProvider.EMPTY
}
/**
 * 通过 `PackageFragmentDescriptor` 获取对应的 `PackageViewDescriptor`。
 *
 * @receiver 包片段实例
 * @return 对应的 `PackageViewDescriptor`
 */

fun PackageFragmentDescriptor.getPackageViewDescriptor(): PackageViewDescriptor {
    val module = containingDeclaration
    val fqName = this.fqName // 假设 PackageFragmentDescriptor 有一个 `fqName` 属性用于获取包的完全限定名
    return module.getPackage(fqName)
}
