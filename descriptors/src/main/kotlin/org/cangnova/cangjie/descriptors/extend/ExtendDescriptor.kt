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

package org.cangnova.cangjie.descriptors.extend

import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.*
interface ClassAndExtendDescriptor:DeclarationDescriptor

/**
 * 扩展描述符接口
 *
 * 表示扩展声明，用于为现有类型添加新的接口实现和成员。
 * 扩展是仓颉语言的特殊功能，允许在不修改原始类型定义的情况下扩展其能力。
 *
 * 示例：
 * ```cangjie
 * extend Type <: Interface1, Interface2 {
 *     func method1() {}
 * }
 * ```
 *
 * 设计原则：
 * - ExtendDescriptor不产生新的类型（不是ClassifierDescriptor）
 * - 但具有继承能力（可以让类型实现新的接口）
 * - 具有作用域（可以包含成员声明）
 * - 支持类型参数（可以是泛型扩展）
 *
 * 关键区别：
 * - ClassDescriptor: 既是类型又可继承
 * - ExtendDescriptor: 不是类型但可继承
 * - 这就是为什么需要分离ClassifierDescriptor和Inheritable的原因
 */
interface ExtendDescriptor :DeclarationDescriptorWithTypeParameters, InheritableDescriptor, HasScopeDescriptor ,ClassAndExtendDescriptor{


    /**
     * 扩展的id
     */
    val extendId: String

    /**
     * 扩展的 this 接收者参数描述符，用于表示扩展内成员函数的接收者类型。
     * 类型为被扩展的类型 (extendType)
     */
    val thisAsReceiverParameter: ReceiverParameterDescriptor

    override val name: Name
        get() = Name.identifier(extendId)

    /**
     * 扩展的类型列表
     */
    override val declaredTypeParameters: List<TypeParameterDescriptor>

    override val source: SourceElement
        get() = SourceElement.NO_SOURCE

    /**
     * 被扩展的类型的类型构造器
     */
    val extendTypeConstructor: TypeConstructor

    /**
     * 被扩展的类型
     */
    val extendType: CangJieType


    /**
     * 获取与此扩展相关的超类型集合，即扩展的接口列表
     */
    override val superTypes: Collection<CangJieType>


    /**
     * 根据指定的类型实参列表返回对应的成员作用域。
     *
     * @param typeArguments 类型实参列表
     * @return 返回计算后的成员作用域
     */
    override fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope

    /**
     * 根据指定的类型替换规则返回对应的成员作用域。
     *
     * @param typeSubstitution 类型替换映射
     * @return 返回计算后的成员作用域
     */
    override fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope

    /**
     * 未进行类型替换时的成员作用域（原始作用域）。
     */
    override val unsubstitutedMemberScope: MemberScope
    val memberScope: MemberScope
        get() {
            if (declaredTypeParameters.isEmpty()) return unsubstitutedMemberScope
            return getMemberScope(declaredTypeParameters.map { TypeProjectionImpl(it.defaultType) })
        }

    /**
     * 实例级成员作用域，默认返回空作用域。
     */
    override val instanceScope: MemberScope
        get() = MemberScope.Empty

    /**
     * 静态成员作用域。
     */
    override val staticScope: MemberScope

    /**
     * 包含该类的声明描述符。
     */
    override val containingDeclaration: DeclarationDescriptor

    /**
     * 此描述符对应的类的种类。
     */
    override val kind: ClassKind get() = ClassKind.EXTEND

    override val visibility: DescriptorVisibility

    /**
     * 原始的分类符描述符（用于引用未替换的原始描述符）。
     */
    override val original: ExtendDescriptor



    override val modality: Modality get() = Modality.FINAL
}

