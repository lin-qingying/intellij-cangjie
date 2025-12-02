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

package org.cangnova.cangjie.serialization.deserialization

import org.cangnova.cangjie.container.DefaultImplementation
import org.cangnova.cangjie.descriptors.*
import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.CangJieType

@DefaultImplementation(impl = AdditionalClassPartsProvider.None::class)

/**
 * 附加类部件提供器接口
 * 用于为类描述符提供额外的类成员信息，如超类型、函数、构造器等
 */
interface AdditionalClassPartsProvider {
    /**
     * 获取类的超类型集合
     * @param classDescriptor 类描述符
     * @return 超类型集合
     */
    fun getSupertypes(classDescriptor: InheritableDescriptor): Collection<CangJieType>

    /**
     * 根据函数名获取类中的函数集合
     * @param name 函数名
     * @param classDescriptor 类描述符
     * @return 简单函数描述符集合
     */
    fun getFunctions(name: Name, classDescriptor: HasScopeDescriptor): Collection<SimpleFunctionDescriptor>

    /**
     * 获取类的构造器集合
     * @param classDescriptor 类描述符
     * @return 类构造器描述符集合
     */
    fun getConstructors(classDescriptor: ClassDescriptor): Collection<ClassConstructorDescriptor>

    /**
     * 获取类中所有函数的名称集合
     * @param classDescriptor 类描述符
     * @return 函数名称集合
     */
    fun getFunctionsNames(classDescriptor: HasScopeDescriptor): Collection<Name>

    /**
     * 默认空实现对象
     * 所有方法都返回空集合
     */
    object None : AdditionalClassPartsProvider {
        override fun getSupertypes(classDescriptor: InheritableDescriptor): Collection<CangJieType> = emptyList()
        override fun getFunctions(
            name: Name,
            classDescriptor: HasScopeDescriptor
        ): Collection<SimpleFunctionDescriptor> =
            emptyList()

        override fun getFunctionsNames(classDescriptor: HasScopeDescriptor): Collection<Name> = emptyList()
        override fun getConstructors(classDescriptor: ClassDescriptor): Collection<ClassConstructorDescriptor> =
            emptyList()
    }
}
