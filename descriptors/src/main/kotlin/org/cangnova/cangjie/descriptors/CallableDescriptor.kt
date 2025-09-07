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
package org.cangnova.cangjie.descriptors

import org.cangnova.cangjie.types.CangJieType


/**
 * 可调用描述符接口，描述可调用的声明元素（如函数、属性等）。
 * 
 * 该接口定义了所有可调用声明的通用特性，包括参数、返回类型、接收器等。
 * 继承自 DeclarationDescriptorWithVisibility（具有可见性的声明描述符）、
 * DeclarationDescriptorNonRoot（非根声明描述符）和 Substitutable（可替换的）。
 */
interface CallableDescriptor : DeclarationDescriptorWithVisibility, DeclarationDescriptorNonRoot,
    Substitutable<CallableDescriptor> {

    /**
     * 值参数列表，表示该可调用元素的所有参数。
     */
    val valueParameters: List<ValueParameterDescriptor>

    /**
     * 原始描述符，在类型替换过程中保持对原始声明的引用。
     */
    override val original: CallableDescriptor

    /**
     * 上下文接收器参数列表，用于支持上下文相关的功能调用。
     */
    val contextReceiverParameters: List<ReceiverParameterDescriptor>

    /**
     * 返回类型，可能为 null（当对象尚未完全初始化或发生错误时）。
     */
    val returnType: CangJieType?

    /**
     * 扩展接收器参数，用于扩展函数的接收器类型。
     */
    val extensionReceiverParameter: ReceiverParameterDescriptor?

    /**
     * 被覆盖的描述符集合，用于处理继承和多态中的方法覆盖。
     */
    val overriddenDescriptors: Collection<CallableDescriptor>

    /**
     * 分发接收器参数，用于成员函数的this引用。
     */
    val dispatchReceiverParameter: ReceiverParameterDescriptor?

    /**
     * 检查是否使用了合成的参数名称。
     * 当参数名称不可用时，会使用合成名称如 "p0", "p1" 等。
     * 
     * @return 如果使用了合成参数名称则返回 true，否则返回 false
     */
    fun hasSynthesizedParameterNames(): Boolean

    /**
     * 类型参数列表，定义该可调用元素的泛型参数。
     */
    val typeParameters: List<TypeParameterDescriptor>

    /**
     * 非扩展的类型参数列表，默认返回空的可变列表。
     * 可以被子类覆盖以提供特定的实现。
     */
    val typeParametersNotExtend: MutableList<TypeParameterDescriptor>
        get() = mutableListOf()

    /**
     * 检查参数名称是否稳定（即不会在编译过程中改变）。
     * 
     * @return 如果参数名称稳定则返回 true，否则返回 false
     */
    fun hasStableParameterNames(): Boolean

    /**
     * 用户数据键接口，用于在描述符中存储额外的用户定义数据。
     * 
     * @param V 用户数据的值类型
     */
    interface UserDataKey<V>
}
