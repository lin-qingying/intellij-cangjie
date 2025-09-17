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

/**
 * 具有类型参数的声明描述符接口
 * 
 * 这是一个纯粹的功能接口，表示任何可以具有类型参数（泛型参数）的声明。
 * 
 * 主要实现者：
 * - ClassDescriptor: 泛型类/接口/结构体
 * - EnumDescriptor: 泛型枚举  
 * - TypeAliasDescriptor: 泛型类型别名
 * - ExtendDescriptor: 泛型扩展
 * - FunctionDescriptor: 泛型函数
 * - PropertyDescriptor: 泛型属性
 * 
 * 设计原则：
 * - 作为独立的功能接口，不依赖于其他接口
 * - 只关注类型参数的声明和管理
 * - 可以被任何需要类型参数的描述符实现
 */
interface DeclarationDescriptorWithTypeParameters : DeclarationDescriptor {

    /**
     * 声明的类型参数列表
     * 
     * 表示该声明定义的类型参数（泛型参数）。
     * 例如：
     * - class MyClass<T, U> 中的 T 和 U
     * - extend Type<T> 中的 T
     * - func foo<T>() 中的 T
     * 
     * 注意：这是声明时定义的类型参数，不是使用时的类型实参。
     */
    val declaredTypeParameters: List<TypeParameterDescriptor>
}
