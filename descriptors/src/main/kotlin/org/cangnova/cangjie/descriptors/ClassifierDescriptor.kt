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
 * 分类器描述符接口。
 *
 * 分类器是语言中可以作为类型使用的构造体，例如类、接口、类型参数、类型别名、结构体、枚举等。
 * 该接口为编译器类型系统提供统一的抽象，用于类型构造、泛型参数管理、类型实例化与子类型关系等功能。
 *
 * 主要职责：
 * - 提供类型构造器（typeConstructor），描述类型参数和构造过程；
 * - 提供默认类型（defaultType），作为该分类器的简单类型表示；
 * - 保持对原始描述符（original）的引用，便于在类型替换和变换中追溯源声明；
 * - 为类型检查、类型等价性判断和 IDE 展示提供元信息支持。
 */
interface ClassifierDescriptor : TypeDescriptor, DeclarationDescriptorNonRoot {

    /**
     * 类型构造器，用于封装创建该分类器类型实例所需的信息（例如泛型参数和边界）。
     */
    val typeConstructor: TypeConstructor

    /**
     * 分类器的默认简单类型表示（不包含具体类型参数绑定）。
     */
    val defaultType: SimpleType

    /**
     * 指向该描述符的原始版本，通常用于在描述符替换或变换中追溯最初的声明。
     */
    override val original: ClassifierDescriptor
}
