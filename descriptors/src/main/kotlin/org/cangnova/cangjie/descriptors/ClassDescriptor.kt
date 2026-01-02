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


/**
 * 类描述符接口
 *
 * 表示类（Class）、接口（Interface）、结构体（Struct）等类型声明。
 * 这些声明同时具有：
 * - 类型能力（可以作为类型使用）
 * - 继承能力（可以继承其他类型或被继承）
 * - 作用域（包含成员声明）
 *
 * 设计原则：
 * - 通过组合多个功能接口，清晰表达类的多重职责
 * - ClassifierDescriptor提供类型系统支持
 * - Inheritable提供继承机制
 * - HasScope提供成员管理
 * - ClassifierDescriptorWithTypeParameters提供泛型支持
 *
 * 使用场景：
 * - 类型检查和类型推导
 * - 成员解析和访问控制
 * - 继承层次分析
 * - 泛型实例化
 *
 * @see ClassAndEnumDescriptor
 * @see EnumDescriptor
 */
interface ClassDescriptor : ClassAndEnumDescriptor {

    /**
     * 类的 this 接收者参数描述符，用于表示类的接收者类型。
     */
    override val thisAsReceiverParameter: ReceiverParameterDescriptor



    /**
     * 类的所有构造函数集合。
     */
    val constructors: Collection<ClassConstructorDescriptor>


    /**
     * 在类结尾处执行的构造函数集合（项目特定语义）。
     */
    val endConstructors: Collection<ClassConstructorDescriptor>


    /**
     * 包含该类的声明描述符。
     */
    override val containingDeclaration: DeclarationDescriptor

    /**
     * 获取类的默认类型；对于泛型类返回 A<T> 形式的类型。
     */
    override val defaultType: SimpleType


    /**
     * 未替换的主构造函数（如果有）。
     */
    val unsubstitutedPrimaryConstructor: ClassConstructorDescriptor?

    /**
     * 返回当前类实际声明的类型参数列表。
     *
     * @return 返回当前类实际声明的类型参数列表
     */


    override val declaredTypeParameters: List<TypeParameterDescriptor>

    /**
     * 如果这是一个 sealed 类，则返回其直接子类；否则返回空集合。
     */

    val sealedSubclasses: Collection<ClassDescriptor>

    /**
     * 原始的类描述符
     *
     * 在类型替换场景中，指向未替换的原始类描述符。
     * 注意：这里返回类型是ClassDescriptor而不是ClassifierDescriptor，
     * 以保持类型的精确性。
     */
    override val original: ClassDescriptor

    /**
     * 对于 SAM 接口的备选函数类型（如无法通过更优方式获取时使用）。
     */
    val defaultFunctionTypeForSamInterface: SimpleType?

    /**
     * 快速判定该类是否肯定不是 SAM 接口。可能在某些情况下返回 false，但只有在确定不是 SAM 时才返回 true。
     */
    val isDefinitelyNotSamInterface: Boolean


    /**
     * 判断类是否包含 const 构造函数。
     *
     * @return 如果存在 const 构造函数则返回 true，否则返回 false
     */
    fun hasConstConstructor(): Boolean {
        val constructors =
            this.constructors
        for (constructor in constructors) {
            if (constructor.isConst) {
                return true
            }
        }


        return false
    }
}
