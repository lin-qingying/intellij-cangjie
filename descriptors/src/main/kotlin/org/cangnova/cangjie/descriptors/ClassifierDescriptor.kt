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

import org.cangnova.cangjie.types.SimpleType
import org.cangnova.cangjie.types.TypeConstructor

/**
 * 分类器描述符接口 (Classifier Descriptor Interface)
 *
 * 这是仓颉编程语言编译器中用于描述类型分类器的核心接口。
 * 分类器是可以用作类型的语言构造，主要包括：
 * - 类 (Class)
 * - 接口 (Interface)
 * - 类型参数 (Type Parameter)
 * - 类型别名 (Type Alias)
 * - 结构体(Struct)
 * - 枚举(Enum)
 *
 * 该接口继承自DeclarationDescriptorNonRoot，表示它是一个非根声明描述符，
 * 意味着它有明确的父级作用域和声明上下文。
 *
 * 主要作用：
 * 1. **类型系统基础**: 为类型检查器提供统一的类型分类器抽象
 * 2. **类型构造支持**: 通过typeConstructor支持泛型和参数化类型
 * 3. **默认类型提供**: 为每个分类器提供其默认的简单类型表示
 * 4. **描述符链管理**: 维护原始描述符的引用关系
 *
 * 使用场景：
 * - 类型解析和检查过程中识别类型分类器
 * - 泛型类型的实例化和参数绑定
 * - 类型等价性判断和子类型关系检查
 * - IDE中的类型信息展示和代码补全
 */
interface ClassifierDescriptor : DeclarationDescriptorNonRoot {

    /**
     * 类型构造器 (Type Constructor)
     *
     * 类型构造器是用于构造具体类型的工具，它封装了创建该分类器类型实例所需的所有信息。
     *
     * 主要功能：
     * - **泛型支持**: 对于泛型类如List<T>，类型构造器包含类型参数信息
     * - **类型实例化**: 将类型参数绑定到具体类型，如List<String>
     * - **类型等价判断**: 用于比较两个类型是否由同一个分类器构造
     * - **子类型关系**: 提供父类型信息，支持继承关系检查
     *
     * 示例：
     * ```kotlin
     * class List<T>        // typeConstructor包含参数T的信息
     * class String         // typeConstructor不包含类型参数
     * interface Comparable<T> // typeConstructor包含参数T和协变信息
     * ```
     *
     * 对于类型参数，类型构造器还包含边界信息（如T : Comparable<T>）
     */
    val typeConstructor: TypeConstructor

    /**
     * 默认类型 (Default Type)
     *
     * 每个分类器的默认简单类型表示，不包含类型参数的具体绑定。
     *
     * 主要特点：
     * - **无参数绑定**: 对于泛型类，使用类型参数的原始形式
     * - **类型推断基础**: 作为类型推断算法的起始点
     * - **错误恢复**: 当类型解析失败时的fallback类型
     * - **简化表示**: 提供分类器的最简类型表示
     *
     * 示例：
     * ```kotlin
     * class List<T>           // defaultType = List<T> (未绑定的T)
     * class String            // defaultType = String
     * interface Runnable      // defaultType = Runnable
     * typealias StringList = List<String>  // defaultType = List<String>
     * ```
     *
     * 注意：defaultType总是SimpleType，不会是复杂的联合类型或交集类型
     */
    val defaultType: SimpleType

    /**
     * 原始描述符 (Original Descriptor)
     *
     * 指向该描述符的原始版本，主要用于处理描述符的替换和变换场景。
     *
     * 使用场景：
     * 1. **类型替换**: 当进行类型参数替换时，保持对原始描述符的引用
     * 2. **描述符链**: 在描述符变换链中追溯到最初的描述符
     * 3. **缓存key**: 使用原始描述符作为缓存的稳定key
     * 4. **等价性检查**: 比较两个描述符是否来源于同一个原始声明
     *
     * 例如：
     * ```kotlin
     * // 原始声明
     * class List<T>
     *
     * // 类型替换后的描述符 List<String>
     * // 其original仍然指向原始的List<T>描述符
     * ```
     *
     * 在大多数情况下，original == this，除非该描述符是通过某种变换产生的。
     */
    override val original: ClassifierDescriptor
}
