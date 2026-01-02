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

import org.cangnova.cangjie.types.CangJieType

/**
 * 具有继承能力的声明描述符接口
 *
 * 该接口表示那些可以参与继承关系的声明，包括继承其他类型或被其他类型继承。
 * 这是一个功能性接口，独立于类型系统和作用域系统。
 *
 * 主要实现者：
 * - ClassDescriptor: 类可以继承其他类和实现接口
 * - EnumDescriptor: 枚举可以实现接口
 * - ExtendDescriptor: 扩展可以为类型添加新的继承关系
 *
 * 设计原则：
 * - 继承功能与类型系统分离（不是所有可继承的都产生类型，如ExtendDescriptor）
 * - 继承功能与作用域分离（继承关系和成员管理是正交的概念）
 * - 提供统一的继承相关属性和方法
 */
interface InheritableDescriptor : DeclarationDescriptorNonRoot,DeclarationDescriptorWithVisibility {
    
    /**
     * 声明的种类
     * 
     * 表示此声明的具体类型，如CLASS、INTERFACE、ENUM、EXTEND等。
     * 这对于确定继承规则和约束非常重要。
     */
    val kind: ClassKind
    
    /**
     * 声明的模态性
     * 
     * 决定此声明是否可以被继承以及如何被继承。
     * - FINAL: 不可被继承
     * - OPEN: 可被继承
     * - ABSTRACT: 必须被继承（不能实例化）
     * - SEALED: 受限继承（只能在特定范围内被继承）
     */
    val modality: Modality
    
    /**
     * 声明的可见性
     * 
     * 决定此声明在何处可见和可访问。
     * 这影响继承的可能性和继承后成员的可见性。
     */
    override val visibility: DescriptorVisibility
    
    /**
     * 超类型集合
     * 
     * 此声明直接继承或实现的所有类型。
     * 对于类，这包括父类和实现的接口。
     * 对于接口，这包括扩展的其他接口。
     * 对于扩展，这包括扩展添加的接口实现。
     */
    val superTypes: Collection<CangJieType> get() = listOf()
    
    /**
     * 获取所有超类型（包括间接继承的）
     * 
     * 递归地获取整个继承层次中的所有超类型。
     * 这对于类型检查和成员解析很重要。
     *
     * @return 所有直接和间接超类型的集合
     */
    fun getAllSuperTypes(): Collection<CangJieType> {
        return emptyList()
    }
    

    /**
     * 原始声明描述符
     * 
     * 在类型替换场景中，这指向未替换的原始声明。
     * 对于非替换的声明，这返回自身。
     */
    override val original: InheritableDescriptor
    

    /**
     * 是否可以被继承
     *
     * 基于模态性快速判断此声明是否可以被其他声明继承。
     * FINAL和某些特殊情况下的声明不可被继承。
     *
     * 与编译器 IsOpen() 方法对应，判断是否可被继承/实现。
     */
    val isInheritable: Boolean
        get() = modality != Modality.FINAL

    /**
     * 是否为开放声明
     *
     * 与编译器的 IsOpen() 方法对应：
     * - OPEN: 显式标记为可被继承
     * - ABSTRACT: 抽象类/接口天然可被继承
     * - Interface: 天然可被实现（默认 OPEN）
     *
     * 编译器逻辑：
     * - ClassDecl.IsOpen() = TestAnyAttr(OPEN, ABSTRACT)
     * - InterfaceDecl.IsOpen() = true
     */
    val isOpen: Boolean
        get() = modality == Modality.OPEN || modality == Modality.ABSTRACT || kind == ClassKind.INTERFACE

    /**
     * 是否为抽象声明
     *
     * 抽象声明不能被直接实例化，必须通过子类实现。
     */
    val isAbstract: Boolean
        get() = modality == Modality.ABSTRACT

    /**
     * 是否为密封声明
     *
     * 密封声明只能在限定范围内被继承。
     */
    val isSealed: Boolean
        get() = modality == Modality.SEALED
}