/*
 * Copyright 2026 LinQingYing. and contributors.
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
interface PackageViewDescriptor : PackageData ,DeclarationDescriptorWithVisibility,PackageAndModuleDescriptor{

    val isMacro: Boolean

    override val containingDeclaration: PackageViewDescriptor?

    val memberScope: MemberScope

    val module: ModuleDescriptor

    val fragments: List<PackageFragmentDescriptor>

    fun isEmpty(): Boolean = fragments.isEmpty()
}
