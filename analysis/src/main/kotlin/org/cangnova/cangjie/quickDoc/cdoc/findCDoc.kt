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

package org.cangnova.cangjie.quickDoc.cdoc

import org.cangnova.cangjie.descriptors.CallableDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.DeclarationDescriptorWithSource
import org.cangnova.cangjie.psi.CjDeclaration
import org.cangnova.cangjie.psi.CjElement
import org.cangnova.cangjie.resolve.caches.resolveToDescriptorIfAny
import com.intellij.psi.PsiElement
import org.cangnova.cangjie.descriptors.DescriptorToSourceUtils
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocSection
import org.cangnova.cangjie.lexer.cdoc.psi.impl.CDocTag

/**
 * CDoc 文档内容。
 *
 * 封装了文档的主要内容和相关章节，用于在快速文档（Quick Documentation）中显示。
 *
 * @property contentTag 文档的主要内容标签，包含描述文本
 * @property sections 相关的文档章节列表，如 `@param`、`@return`、`@throws` 等
 *
 * @see findCDoc
 * @see CDocTag
 * @see CDocSection
 */
data class CDocContent(
    val contentTag: CDocTag,
    val sections: List<CDocSection>
)

/**
 * 声明描述符到 PSI 元素的转换函数类型。
 *
 * 用于在查找 CDoc 时将描述符转换为对应的 PSI 元素。
 */
private typealias DescriptorToPsi = (DeclarationDescriptorWithSource) -> PsiElement?

/**
 * 从声明描述符查找 CDoc 文档注释。
 *
 * 该方法会将描述符转换为 PSI 元素，然后查找对应的文档注释。
 * 支持查找继承的文档注释。
 *
 * ## 使用场景
 *
 * - 在代码补全、快速文档等场景中，从描述符获取文档
 * - 查找重写方法的文档时，自动查找父类/接口的文档
 *
 * ## 示例
 * ```kotlin
 * val descriptor: FunctionDescriptor = ...
 * val cdoc = descriptor.findCDoc()
 * if (cdoc != null) {
 *     // 显示文档内容
 *     println(cdoc.contentTag.text)
 * }
 * ```
 *
 * @receiver DeclarationDescriptor 声明描述符
 * @param descriptorToPsi 描述符到 PSI 元素的转换函数，默认使用 [DescriptorToSourceUtils.descriptorToDeclaration]
 * @return CDocContent 文档内容；如果没有找到文档则返回 `null`
 *
 * @see CDocContent
 * @see findCDocByPsi
 * @see lookupInheritedCDoc
 */
fun DeclarationDescriptor.findCDoc(
    descriptorToPsi: (DeclarationDescriptorWithSource) -> PsiElement? = { DescriptorToSourceUtils.descriptorToDeclaration(it) }
): CDocContent? {
    if (this is DeclarationDescriptorWithSource) {
        val psiDeclaration = descriptorToPsi(this)?.navigationElement
        return (psiDeclaration as? CjElement)?.findCDoc(descriptorToPsi)
    }
    return null
}

/**
 * 从仓颉元素查找 CDoc 文档注释，支持查找继承的文档。
 *
 * 该方法会按照以下优先级查找文档：
 * 1. 查找元素自己的 CDoc 注释（通过 [findCDocByPsi]）
 * 2. 如果没有自己的注释，则查找继承自父类/接口的文档（通过 [lookupInheritedCDoc]）
 *
 * ## 继承查找规则
 *
 * 对于重写的方法或属性，如果没有自己的文档注释，会自动查找被重写的父类/接口成员的文档。
 * 这遵循了文档继承的原则。
 *
 * ### 示例
 * ```kotlin
 * interface Animal {
 *     /**
 *      * 发出声音
 *      */
 *     fun makeSound()
 * }
 *
 * class Dog : Animal {
 *     // 没有自己的文档，会继承 Animal.makeSound() 的文档
 *     override fun makeSound() {
 *         println("Woof!")
 *     }
 * }
 * ```
 *
 * @receiver CjElement 要查找文档的仓颉元素
 * @param descriptorToPsi 描述符到 PSI 元素的转换函数
 * @return CDocContent 文档内容（可能来自自己或继承）；如果没有找到则返回 `null`
 *
 * @see findCDocByPsi
 * @see lookupInheritedCDoc
 */
fun CjElement.findCDoc(descriptorToPsi: DescriptorToPsi): CDocContent? {
    return findCDocByPsi()
        ?: this.lookupInheritedCDoc(descriptorToPsi)
}

/**
 * 查找继承自父类/接口的 CDoc 文档注释。
 *
 * 该方法用于处理重写（override）的方法和属性。当元素自己没有文档注释时，
 * 会遍历所有被重写的父类/接口成员，查找它们的文档注释。
 *
 * ## 查找顺序
 *
 * 按照重写链从近到远查找，第一个找到的文档会被返回。例如：
 *
 * ```
 * Interface A (有文档)
 *     ↑
 * Interface B (无文档)
 *     ↑
 * Class C (无文档) ← 当前元素
 * ```
 *
 * 会找到 Interface B 的文档（如果有），否则找到 Interface A 的文档。
 *
 * ## 适用范围
 *
 * - 重写的方法
 * - 重写的属性
 * - 实现的接口方法
 *
 * @receiver CjElement 要查找文档的仓颉元素
 * @param descriptorToPsi 描述符到 PSI 元素的转换函数
 * @return CDocContent 继承的文档内容；如果没有找到则返回 `null`
 *
 * @see CallableDescriptor.overriddenDescriptors
 */
private fun CjElement.lookupInheritedCDoc(descriptorToPsi: DescriptorToPsi): CDocContent? {
    if (this is CjDeclaration) {
        val descriptor = resolveToDescriptorIfAny()

        if (descriptor is CallableDescriptor) {
            for (baseDescriptor in descriptor.overriddenDescriptors) {
                val baseCDoc = baseDescriptor.original.findCDoc(descriptorToPsi)
                if (baseCDoc != null) {
                    return baseCDoc
                }
            }
        }
    }

    return null
}
