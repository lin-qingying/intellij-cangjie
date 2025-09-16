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

import org.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptor
import org.cangnova.cangjie.resolve.constants.ConstantValue
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeSubstitutor


/**
 * 属性描述符接口
 * 表示仓颉语言中的属性（property），支持getter和setter访问器
 * 
 * 组合了多个接口的功能：
 * - EnumMember: 枚举成员功能
 * - PropertyDescriptorWithAccessors: 带访问器的属性功能
 * - CallableMemberDescriptor: 可调用成员描述符功能
 */
interface PropertyDescriptor :EnumMember, PropertyDescriptorWithAccessors, CallableMemberDescriptor {

    /**
     * getter访问器描述符
     * 用于获取属性值的访问器，可能为null（如果没有定义getter）
     */
    override val getter: PropertyGetterDescriptor?


    /**
     * setter访问器描述符
     * 用于设置属性值的访问器，可能为null（如果没有定义setter）
     */
    override val setter: PropertySetterDescriptor?


    /**
     * 所有访问器的列表
     * 包含此属性的所有getter和setter访问器
     */
    val accessors: List<PropertyAccessorDescriptor>



    /**
     * 原始属性描述符
     * 返回此描述符的原始版本（非子类型替换版本）
     */
    override val original: PropertyDescriptor


    /**
     * 被重写的描述符集合
     * 包含此属性重写的所有父类或接口中的属性描述符
     */
    override val overriddenDescriptors: Collection<PropertyDescriptor>

    /**
     * 获取编译时常量初始化器
     * 返回属性的编译时常量值，如果属性不是常量则为null
     */
    override fun getCompileTimeInitializer(): ConstantValue<*>?


    /**
     * 类型替换
     * 使用给定的类型替换器创建此属性的新版本
     */
    override fun substitute(substitutor: TypeSubstitutor): PropertyDescriptor

    /**
     * 创建复制构建器
     * 返回用于创建此属性副本的构建器
     */
    override fun newCopyBuilder(): CallableMemberDescriptor.CopyBuilder<out PropertyDescriptor>

    /**
     * 输入类型
     * 对于扩展属性，返回接收者类型，否则为null
     */
    val inType: CangJieType?
}
