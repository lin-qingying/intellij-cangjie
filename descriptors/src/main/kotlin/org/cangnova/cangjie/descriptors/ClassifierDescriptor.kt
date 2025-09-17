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

import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeConstructor

/**
 * 分类器描述符接口
 *
 * 分类器是语言中可以作为类型使用的构造体，包括：
 * - 类（Class）
 * - 接口（Interface）
 * - 结构体（Struct）
 * - 枚举（Enum）
 * - 类型参数（TypeParameter）
 * - 类型别名（TypeAlias）
 *
 * 该接口为编译器类型系统提供统一的抽象，专注于类型相关的功能。
 *
 * 设计原则：
 * - ClassifierDescriptor只关注类型系统，不涉及继承和作用域
 * - 继承功能由Inheritable接口提供
 * - 作用域功能由HasScope接口提供
 * - 不是所有ClassifierDescriptor都可以被继承（如TypeParameterDescriptor）
 * - 不是所有ClassifierDescriptor都有作用域（如TypeParameterDescriptor）
 *
 * 主要职责：
 * - 提供类型构造器（typeConstructor），描述如何构造此类型
 * - 提供默认类型（defaultType），作为该分类器的基础类型表示
 * - 管理类型参数和类型替换
 * - 支持类型等价性判断和子类型关系检查
 */
interface ClassifierDescriptor : DeclarationDescriptorNonRoot {

    /**
     * 类型构造器
     *
     * 封装了创建该分类器类型实例所需的信息，包括：
     * - 类型参数及其边界
     * - 超类型信息（如果适用）
     * - 类型构造规则
     */
    val typeConstructor: TypeConstructor

    /**
     * 分类器的默认简单类型
     *
     * 这是不带具体类型参数绑定的基础类型表示。
     * 对于泛型类型，这通常使用类型参数的默认边界。
     * 例如：List<T> 的 defaultType 是 List<*>
     */
    val defaultType: SimpleType

    /**
     * 原始分类器描述符
     *
     * 在类型替换场景中，指向未替换的原始描述符。
     * 对于非替换的描述符，返回自身。
     */
    override val original: ClassifierDescriptor


}

interface ClassifierDescriptorWithTypeParameters : ClassifierDescriptor, DeclarationDescriptorWithTypeParameters,
    Substitutable<ClassifierDescriptorWithTypeParameters>


interface ClassifierDescriptorWithTypeConstructor:ClassifierDescriptorWithTypeParameters,InheritableDescriptor, MemberDescriptor{
    override val original: ClassifierDescriptorWithTypeConstructor
}