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

import org.cangnova.cangjie.resolve.scopes.MemberScope


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
