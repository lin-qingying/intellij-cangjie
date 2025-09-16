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

import org.cangnova.cangjie.descriptors.extend.ExtendDescriptor

/**
 * 声明描述符访问者接口，采用访问者模式用于对不同类型的声明描述符执行操作。
 *
 * 各 visitXXX 方法用于访问对应类型的描述符，并可通过返回值传递处理结果。
 * @param R 访问返回值类型
 * @param D 传递给访问者的可选数据类型
 */
interface DeclarationDescriptorVisitor<R, D> {
    /** 访问包视图描述符 */
    fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, builder: D?): R
    /** 访问类型参数描述符 */
    fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, builder: D?): R

    /** 访问值参数描述符 */
    fun visitValueParameterDescriptor(
        descriptor: ValueParameterDescriptor,
        builder: D?
    ): R

    /** 访问属性描述符 */
    fun visitPropertyDescriptor(descriptor: PropertyDescriptor, builder: D?): R
    /** 访问属性 getter 描述符 */
    fun visitPropertyGetterDescriptor(
        descriptor:  PropertyGetterDescriptor ,
        builder: D?
    ): R
    /** 访问属性 setter 描述符 */
    fun visitPropertySetterDescriptor(
        descriptor:  PropertySetterDescriptor ,
        builder: D?
    ): R

    /** 访问模块声明描述符 */
    fun visitModuleDeclaration(descriptor: ModuleDescriptor, builder: D?): R
    /** 访问类型别名描述符 */
    fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, builder: D?): R
    /** 访问构造函数描述符 */
    fun visitConstructorDescriptor(
        constructorDescriptor: ConstructorDescriptor,
        builder: D?
    ): R

    /** 访问类型描述符（统一的类型相关描述符入口） */
    fun visitTypeDescriptor(descriptor: TypeDescriptor, builder: D?): R {
        return when (descriptor) {
            is ClassifierDescriptor -> visitClassifierDescriptor(descriptor, builder)
            is ExtendDescriptor -> visitExtendDescriptor(descriptor, builder)
            else -> throw IllegalArgumentException("Unknown TypeDescriptor implementation: ${descriptor::class}")
        }
    }

    /** 访问分类器描述符 */
    fun visitClassifierDescriptor(descriptor: ClassifierDescriptor, builder: D?): R {
        return when (descriptor) {
            is ClassDescriptor -> visitClassDescriptor(descriptor, builder)
            is EnumDescriptor -> visitEnumDescriptor(descriptor, builder)
            else -> throw IllegalArgumentException("Unknown ClassifierDescriptor implementation: ${descriptor::class}")
        }
    }

    /** 扩展描述符 */
    fun visitExtendDescriptor(descriptor: ExtendDescriptor, builder: D?): R

    /** 访问类描述符 */
    fun visitClassDescriptor(descriptor: ClassDescriptor, builder: D?): R
    /** 访问变量描述符基类（抽象变量） */
    fun visitVariableDescriptorBase(descriptor: VariableDescriptor, builder: D?): R
    /** 访问具体变量描述符 */
    fun visitVariableDescriptor(descriptor: VariableDescriptor, builder: D?): R

   /** 访问包片段描述符 */
    fun visitPackageFragmentDescriptor(
        descriptor: PackageFragmentDescriptor,
        builder: D?
    ): R

    /** 访问函数描述符 */
    fun visitFunctionDescriptor(descriptor: FunctionDescriptor, builder: D?): R
    /** 访问接收者参数描述符 */
    fun visitReceiverParameterDescriptor(
        descriptor: ReceiverParameterDescriptor,
        builder: D?
    ): R
    
    /** 访问枚举描述符 */
    fun visitEnumDescriptor(descriptor: EnumDescriptor, builder: D?): R
    /** 访问枚举构造函数描述符 */
    fun visitEnumConstructorDescriptor(descriptor: EnumConstructorDescriptor, builder: D?): R

}
