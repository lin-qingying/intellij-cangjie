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
 * 可调用声明的通用描述符接口，表示函数、方法、属性访问器等可被调用的声明元素。
 *
 * 该接口定义了可调用元素的共同属性，例如参数列表、返回类型、接收者、覆盖关系以及泛型参数等。
 * 它继承自 DeclarationDescriptorWithVisibility、DeclarationDescriptorNonRoot 和 Substitutable，
 * 因此支持可见性、源信息以及类型替换操作。
 */
interface CallableDescriptor : DeclarationDescriptorWithVisibility, DeclarationDescriptorNonRoot,
    Substitutable<CallableDescriptor> {

    /** 值参数列表 */
    val valueParameters: List<ValueParameterDescriptor>

    /** 原始描述符（用于在替换场景中回溯到未替换的声明） */
    override val original: CallableDescriptor

    /** 上下文接收者参数列表 */
    val contextReceiverParameters: List<ReceiverParameterDescriptor>

    /** 返回类型，可能为 null（如尚未初始化或出错时） */
    val returnType: CangJieType?

    /** 扩展接收器参数（扩展函数的接收者） */
    val extensionReceiverParameter: ReceiverParameterDescriptor?

    /** 被覆盖的描述符集合（用于覆盖关系和继承解析） */
    val overriddenDescriptors: Collection<CallableDescriptor>

    /** 分发接收器参数（成员函数的 this 引用） */
    val dispatchReceiverParameter: ReceiverParameterDescriptor?

    /** 检查是否使用了合成的参数名称（如 p0、p1 等） */
    fun hasSynthesizedParameterNames(): Boolean

    /** 类型参数列表（泛型参数） */
    val typeParameters: List<TypeParameterDescriptor>

    /** 非扩展形式的类型参数列表，默认返回可变空列表 */
    val typeParametersNotExtend: MutableList<TypeParameterDescriptor>
        get() = mutableListOf()

    /** 检查参数名称是否稳定（不会在编译过程中变化） */
    fun hasStableParameterNames(): Boolean

    /**
     * 用户数据键接口，用于在描述符中存放自定义附加数据。
     * @param V 用户数据的类型
     */
    interface UserDataKey<V>
}
