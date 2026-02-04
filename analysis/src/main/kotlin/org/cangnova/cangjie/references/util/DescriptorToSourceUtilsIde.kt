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

package org.cangnova.cangjie.references.util

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.references.CjReferenceResolutionHelper
import org.cangnova.cangjie.utils.sequenceOfLazyValues
import com.intellij.openapi.project.Project
import com.intellij.psi.PsiElement
import com.intellij.psi.search.GlobalSearchScope
import org.cangnova.cangjie.descriptors.DescriptorToSourceUtils

/**
 * IDE 环境下 Descriptor 到源码的转换工具
 *
 * 该工具类提供了在 IDE 环境中将 Descriptor（描述符）转换为对应的 PSI 元素的功能。
 * 它是 {@link DescriptorToSourceUtils} 的 IDE 扩展版本，能够：
 * - 查找源代码中的声明
 * - 查找内置库（builtins）中的声明
 * - 查找反编译代码中的声明
 * - 处理伪覆盖（fake override）等复杂情况
 *
 * ## 主要用途
 *
 * 1. **导航功能**: "跳转到声明"、"查找用法"等 IDE 功能需要从 Descriptor 找到对应的 PSI 元素
 * 2. **引用解析**: 解析代码中的引用时，需要将解析结果（Descriptor）转换为可导航的 PSI 元素
 * 3. **跨文件引用**: 处理库文件、反编译文件等场景下的引用
 *
 * ## 设计说明
 *
 * - 使用懒加载序列（Sequence）来提高性能，避免不必要的计算
 * - 自动去重导航目标，避免同一声明被多次展示
 * - 支持内置库和反编译代码的搜索范围控制
 */
object DescriptorToSourceUtilsIde {
    /**
     * 获取 Descriptor 对应的任意一个 PSI 声明元素
     *
     * 该方法用于快速获取一个与给定 Descriptor 相关的 PSI 元素。
     * 如果存在多个相关元素（例如伪覆盖有多个声明），则返回其中任意一个。
     *
     * ## 使用场景
     *
     * - 快速导航：只需要跳转到一个声明位置
     * - 获取示例：只需要一个 PSI 元素进行分析
     *
     * ## 查找范围
     *
     * 该方法可以找到以下位置的声明：
     * - 用户源代码
     * - 内置库（builtins）
     * - 反编译的库代码
     *
     * @param project 当前 IDE 项目
     * @param descriptor 要查找的 Descriptor（可能是类、函数、属性等的描述符）
     * @return 对应的 PSI 元素，如果未找到则返回 null
     */
    fun getAnyDeclaration(project: Project, descriptor: DeclarationDescriptor): PsiElement? {
        return getDeclarationsStream(project, descriptor).firstOrNull()
    }

    /**
     * 获取 Descriptor 对应的所有 PSI 声明元素
     *
     * 该方法返回与给定 Descriptor 相关的所有 PSI 元素。这在处理以下情况时特别有用：
     * - 伪覆盖（fake override）：一个方法可能覆盖多个父类/接口的方法
     * - 多声明引用：同一符号在库源码和反编译代码中都有声明
     *
     * ## 去重逻辑
     *
     * 方法会自动过滤掉导航到同一目标的重复元素。
     * 例如，如果元素 A 的 navigationElement 是元素 B，则只保留 B。
     * 这避免了在库源文件中引用时出现重复结果。
     *
     * ## 使用场景
     *
     * - "查找所有声明"功能
     * - 显示所有覆盖/实现的方法
     * - 分析继承关系
     *
     * @param project 当前 IDE 项目
     * @param targetDescriptor 目标 Descriptor
     * @param builtInsSearchScope 内置库的搜索范围（可选），用于限制在特定范围内查找
     * @return 所有相关的 PSI 元素集合，已去重
     */
    fun getAllDeclarations(
        project: Project,
        targetDescriptor: DeclarationDescriptor,
        builtInsSearchScope: GlobalSearchScope? = null
    ): Collection<PsiElement> {
        val result = getDeclarationsStream(project, targetDescriptor, builtInsSearchScope).toHashSet()

        // 过滤掉那些导航到结果集中其他元素的元素
        // 这是为了避免在库源文件中的引用产生重复结果
        // 例如：如果 element 的 navigationElement 是结果集中的另一个元素，则只保留后者
        return result.filter { element -> result.none { element != it && it.navigationElement == element } }
    }

    /**
     * 获取 Descriptor 对应的 PSI 元素流（懒加载序列）
     *
     * 这是内部核心方法，使用懒加载序列来高效地查找 PSI 元素。
     *
     * ## 查找策略
     *
     * 1. 首先获取"有效的被引用 Descriptor"列表（处理伪覆盖等情况）
     * 2. 对每个有效 Descriptor，尝试从以下来源查找：
     *    - 源代码声明
     *    - 反编译声明（包括内置库和库文件）
     * 3. 使用懒加载序列避免不必要的计算
     *
     * ## 为什么同时查找源码和反编译声明？
     *
     * 在库源码中的引用应该解析到对应的反编译声明。
     * 因此我们将源码声明和反编译声明都放入流中，
     * 然后在 getAllDeclarations 中进行过滤去重。
     *
     * @param project 当前 IDE 项目
     * @param targetDescriptor 目标 Descriptor
     * @param builtInsSearchScope 内置库的搜索范围（可选）
     * @return PSI 元素的懒加载序列
     */
    private fun getDeclarationsStream(
        project: Project, targetDescriptor: DeclarationDescriptor, builtInsSearchScope: GlobalSearchScope? = null
    ): Sequence<PsiElement> {
        // 获取有效的被引用 Descriptor 列表
        // 例如，对于伪覆盖，这会返回实际被覆盖的声明
        val effectiveReferencedDescriptors =
            DescriptorToSourceUtils.getEffectiveReferencedDescriptors(targetDescriptor).asSequence()

        return effectiveReferencedDescriptors.flatMap { effectiveReferenced ->
            // 对每个有效的 Descriptor，尝试多种方式查找对应的 PSI 元素
            // 使用 sequenceOfLazyValues 实现懒加载：只有在需要时才执行查找
            sequenceOfLazyValues(
                // 1. 从源代码中查找声明
                { DescriptorToSourceUtils.getSourceFromDescriptor(effectiveReferenced) },

                // 2. 从反编译代码中查找声明（包括内置库和编译后的库文件）
                { CjReferenceResolutionHelper.getInstance().findDecompiledDeclaration(project, effectiveReferenced, builtInsSearchScope) }
            )
        }
            .filterNotNull() // 过滤掉查找失败（null）的结果
            .filter { it.isValid && it.containingFile != null } // 过滤掉失效的 PSI 元素
    }
}
