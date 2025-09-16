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
 * 带类型参数的分类器描述符接口
 * 表示具有类型参数（泛型参数）的分类器，如泛型类、泛型接口等
 * 
 * 该接口组合了多个功能：
 * - ClassifierDescriptor: 基本的分类器功能
 * - DeclarationDescriptorWithVisibility: 具有可见性声明的描述符
 * - MemberDescriptor: 成员描述符功能
 * - Substitutable: 支持类型替换的功能
 */
interface ClassifierDescriptorWithTypeParameters

    : ClassifierDescriptor, DeclarationDescriptorWithVisibility, MemberDescriptor,
    Substitutable<ClassifierDescriptorWithTypeParameters> {

    /**
     * 声明的类型参数列表
     * 表示该分类器定义的类型参数（泛型参数）
     * 例如：class MyClass<T, U> 中的 T 和 U
     */
    val declaredTypeParameters: List<TypeParameterDescriptor>
}
