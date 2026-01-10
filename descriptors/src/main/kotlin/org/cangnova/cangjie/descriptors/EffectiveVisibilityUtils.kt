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

import org.cangnova.cangjie.builtins.StandardNames
import org.cangnova.cangjie.descriptors.Visibilities.Inherited.customEffectiveVisibility
import org.cangnova.cangjie.resolve.DescriptorUtils
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.checker.SimpleClassicTypeSystemContext

/**
 * 计算描述符的有效可见性
 * @param visibility 指定的可见性（默认为当前描述符的可见性）
 * @param checkPublishedApi 是否检查PublishedApi注解
 * @return 返回计算后的EffectiveVisibility
 */
fun DeclarationDescriptorWithVisibility.effectiveVisibility(
    visibility: DescriptorVisibility = this.visibility,

): EffectiveVisibility =
    lowerBound(
        visibility.effectiveVisibility(this),
        (this.containingDeclaration as? ClassDescriptor)?.effectiveVisibility()
            ?: EffectiveVisibility.Public
    )


/**
 * 表示一个分类器描述符及其与类型的关系
 * @param descriptor 分类器描述符
 * @param relation 描述符与类型的关系
 */
data class DescriptorWithRelation(val descriptor: ClassifierDescriptor, private val relation: RelationToType) {
    fun effectiveVisibility() =
        (descriptor as? ClassDescriptor)?.visibility?.effectiveVisibility(descriptor) ?: EffectiveVisibility.Public

    override fun toString() = "$relation ${descriptor.name}"
}

/**
 * 计算类描述符的有效可见性
 * @param classes 已处理的类描述符集合
 * @param checkPublishedApi 是否检查PublishedApi注解
 * @return 返回计算后的EffectiveVisibility
 */
private fun ClassDescriptor.effectiveVisibility(classes: Set<ClassDescriptor> ): EffectiveVisibility =
    if (this in classes) EffectiveVisibility.Public
    else with(this.containingDeclaration as? ClassDescriptor) {
        lowerBound(
            visibility.effectiveVisibility(this@effectiveVisibility),
            this?.effectiveVisibility(classes + this@effectiveVisibility) ?: EffectiveVisibility.Public
        )
    }
/**
 * 计算描述符可见性的有效可见性
 * @param descriptor 声明描述符
 * @param checkPublishedApi 是否检查PublishedApi注解
 * @return 返回计算后的EffectiveVisibility
 */
fun DescriptorVisibility.effectiveVisibility(
    descriptor: DeclarationDescriptor,

): EffectiveVisibility {
    return customEffectiveVisibility() ?: normalize().forVisibility(descriptor)
}

/**
 * 计算两个有效可见性的下限
 * @param first 第一个有效可见性
 * @param second 第二个有效可见性
 * @return 返回下限后的EffectiveVisibility
 */
private fun lowerBound(first: EffectiveVisibility, second: EffectiveVisibility): EffectiveVisibility {
    return first.lowerBound(second, SimpleClassicTypeSystemContext)
}

/**
 * 计算类描述符的有效可见性
 * @param checkPublishedApi 是否检查PublishedApi注解
 * @return 返回计算后的EffectiveVisibility
 */
fun ClassDescriptor.effectiveVisibility( ) =
    effectiveVisibility(emptySet())

/**
 * 根据描述符的可见性计算有效可见性
 * @param descriptor 声明描述符
 * @param checkPublishedApi 是否检查PublishedApi注解
 * @return 返回计算后的EffectiveVisibility
 */
private fun DescriptorVisibility.forVisibility(
    descriptor: DeclarationDescriptor,

): EffectiveVisibility =
    when (this) {
        DescriptorVisibilities.PRIVATE_TO_THIS, DescriptorVisibilities.INVISIBLE_FAKE -> EffectiveVisibility.PrivateInClass
        DescriptorVisibilities.PRIVATE -> if (descriptor is ClassDescriptor &&
            descriptor.containingDeclaration is PackageFragmentDescriptor
        ) EffectiveVisibility.PrivateInFile else EffectiveVisibility.PrivateInClass

        DescriptorVisibilities.PROTECTED -> EffectiveVisibility.Protected(
            (descriptor.containingDeclaration as? ClassDescriptor)?.defaultType?.constructor
        )

        DescriptorVisibilities.INTERNAL ->  EffectiveVisibility.Internal

        DescriptorVisibilities.PUBLIC -> EffectiveVisibility.Public
        DescriptorVisibilities.LOCAL -> EffectiveVisibility.Local
        // NB: visibility must be already normalized here, so e.g. no JavaVisibilities are possible at this point
        else -> throw AssertionError("Visibility $name is not allowed in forVisibility")
    }
/**
 * 计算CangJieType的最小允许描述符
 * @param base 基础有效可见性
 * @return 返回最小允许描述符
 */
fun CangJieType.leastPermissiveDescriptor(base: EffectiveVisibility) = dependentDescriptors().leastPermissive(base)
// Should collect all dependent classifier descriptors, to get verbose diagnostic
private fun CangJieType.dependentDescriptors() = dependentDescriptors(emptySet(), RelationToType.CONSTRUCTOR)
/**
 * 收集CangJieType的依赖描述符
 * @param types 已处理的类型集合
 * @param ownRelation 当前类型的关系
 * @return 返回依赖描述符集合
 */
private fun CangJieType.dependentDescriptors(types: Set<CangJieType>, ownRelation: RelationToType): Set<DescriptorWithRelation> {
    if (this in types) return emptySet()
    val ownDependent = constructor.declarationDescriptor?.dependentDescriptors(ownRelation) ?: emptySet()
    val argumentDependent = arguments.map { it.type.dependentDescriptors(types + this, RelationToType.ARGUMENT) }.flatten()
    return ownDependent + argumentDependent
}
/**
 * 收集分类器描述符的依赖描述符
 * @param ownRelation 当前分类器描述符的关系
 * @return 返回依赖描述符集合
 */
private fun ClassifierDescriptor.dependentDescriptors(ownRelation: RelationToType): Set<DescriptorWithRelation> =
    setOf(DescriptorWithRelation(this, ownRelation)) +
            ((this.containingDeclaration as? ClassifierDescriptor)?.dependentDescriptors(ownRelation.containerRelation()) ?: emptySet())
private fun Set<DescriptorWithRelation>.leastPermissive(base: EffectiveVisibility): DescriptorWithRelation? {
    for (descriptorWithRelation in this) {
        val currentVisibility = descriptorWithRelation.effectiveVisibility()
        when (currentVisibility.relation(base, SimpleClassicTypeSystemContext)) {
            EffectiveVisibility.Permissiveness.LESS, EffectiveVisibility.Permissiveness.UNKNOWN -> {
                return descriptorWithRelation
            }
            else -> {
            }
        }
    }
    return null
}





