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
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeProjection
import org.cangnova.cangjie.types.TypeSubstitution

/**
 * 类型描述符接口，用于统一表示所有与类型相关的描述符。
 *
 * 这是一个更高层的抽象，统一了分类器（ClassifierDescriptor）和扩展（ExtendDescriptor）
 * 等类型相关的概念，为类型系统提供统一的处理接口。
 *
 * 主要职责：
 * - 提供类型相关的成员作用域访问
 * - 支持类型参数和类型替换
 * - 统一类型相关描述符的访问模式
 * - 为类型检查和IDE功能提供抽象层
 *
 * 适用于：
 * - 类、接口、枚举等分类器
 * - 扩展声明
 * - 其他具有类型语义的构造体
 */
interface TypeDescriptor : DeclarationDescriptorNonRoot, MemberDescriptor {

    /**
     * 获取与此类型相关的超类型集合。
     *
     * 对于分类器，返回父类和实现的接口；
     * 对于扩展，返回扩展的接口列表。
     *
     * @return 超类型的集合
     */
    val superTypes: Collection<CangJieType>

    /**
     * 根据指定的类型实参列表返回对应的成员作用域。
     *
     * @param typeArguments 类型实参列表
     * @return 返回计算后的成员作用域
     */
    fun getMemberScope(typeArguments: List<TypeProjection>): MemberScope {
        return MemberScope.Empty
    }

    /**
     * 根据指定的类型替换规则返回对应的成员作用域。
     *
     * @param typeSubstitution 类型替换映射
     * @return 返回计算后的成员作用域
     */
    fun getMemberScope(typeSubstitution: TypeSubstitution): MemberScope {
        return MemberScope.Empty
    }

    /**
     * 未进行类型替换时的成员作用域（原始作用域）。
     */
    val unsubstitutedMemberScope: MemberScope
        get() = MemberScope.Empty

    /**
     * 静态成员作用域。
     *
     * 对于不支持静态成员的类型，返回空作用域。
     */
    val staticScope: MemberScope
        get() = MemberScope.Empty

    /**
     * 指向该描述符的原始版本，通常用于在描述符替换或变换中追溯最初的声明。
     */
    override val original: TypeDescriptor


    override val modality: Modality
        get() = Modality.FINAL
}