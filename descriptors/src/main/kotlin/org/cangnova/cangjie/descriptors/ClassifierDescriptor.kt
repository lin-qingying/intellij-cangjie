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

/**
 * 带类型参数的分类器描述符接口
 *
 * 扩展了 [ClassifierDescriptor]，增加了对类型参数的支持。
 * 此接口用于描述支持泛型的分类器，如泛型类、泛型接口等。
 *
 * ## 继承关系
 * - [ClassifierDescriptor] - 基础分类器功能
 * - [DeclarationDescriptorWithTypeParameters] - 提供类型参数声明管理
 * - [Substitutable] - 支持类型替换操作
 *
 * ## 适用场景
 * 当分类器需要声明和管理类型参数时使用，例如：
 * ```cangjie
 * class List<T> { ... }           // T 是类型参数
 * interface Map<K, V> { ... }     // K 和 V 是类型参数
 * ```
 *
 * ## 类型替换支持
 * 通过 [Substitutable] 接口，支持创建具体类型实例的描述符。
 * 例如：从 `List<T>` 创建 `List<String>` 的描述符。
 *
 * @see ClassifierDescriptor 分类器基础接口
 * @see DeclarationDescriptorWithTypeParameters 类型参数管理
 * @see Substitutable 类型替换支持
 */
interface ClassifierDescriptorWithTypeParameters : ClassifierDescriptor, MemberDescriptor,
    DeclarationDescriptorWithTypeParameters,
    Substitutable<ClassifierDescriptorWithTypeParameters>

/**
 * 带类型构造器的分类器描述符接口
 *
 * 这是最完整的分类器描述符接口，整合了类型系统、继承关系和成员访问的所有功能。
 * 用于描述可以被实例化、继承和作为成员的分类器类型。
 *
 * ## 继承关系和职责
 *
 * ### 1. [ClassifierDescriptorWithTypeParameters]
 * - 提供类型构造器和类型参数管理
 * - 支持泛型类型的定义和替换
 *
 * ### 2. [InheritableDescriptor]
 * - 支持继承关系（超类、接口）
 * - 提供类型层次结构查询
 * - 管理虚函数和重写关系
 *
 * ### 3. [MemberDescriptor]
 * - 作为包或类的成员
 * - 提供可见性控制
 * - 支持成员访问检查
 *
 * ## 适用的分类器类型
 *
 * 此接口通常用于以下类型：
 * - **类（Class）** - 可继承、可实例化、有成员
 * - **接口（Interface）** - 可继承、可实现、有成员
 * - **结构体（Struct）** - 可实例化、有成员
 * - **枚举（Enum）** - 可实例化、有成员
 *
 * 注意：**类型参数（TypeParameter）** 和 **类型别名（TypeAlias）** 通常不实现此接口，
 * 因为它们不能被继承或不需要完整的成员描述符功能。
 *
 * ## 示例场景
 *
 * ```cangjie
 * // 公开的泛型类，可以被继承
 * public class ArrayList<T> : List<T> {
 *     // 类成员...
 * }
 * ```
 *
 * 对于上述类：
 * - 类型参数：`T`
 * - 超类型：`List<T>`
 * - 可见性：`public`
 * - 可继承性：是
 *
 * ## 核心功能
 *
 * 1. **类型构造**：通过 typeConstructor 创建具体类型实例
 * 2. **继承查询**：检查是否为某类型的子类型
 * 3. **成员访问**：提供可见性和封装控制
 * 4. **类型替换**：支持泛型实例化
 *
 * @property original 原始的未替换描述符，用于追溯类型替换链
 *
 * @see ClassifierDescriptorWithTypeParameters 类型参数支持
 * @see InheritableDescriptor 继承关系管理
 * @see MemberDescriptor 成员访问控制
 */
interface ClassifierDescriptorWithTypeConstructor : ClassifierDescriptorWithTypeParameters, InheritableDescriptor,
    MemberDescriptor {
    override val original: ClassifierDescriptorWithTypeConstructor
}