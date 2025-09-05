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


interface DeclarationDescriptorVisitor<R, D> {
    fun visitPackageViewDescriptor(descriptor: PackageViewDescriptor, builder: D?): R
    fun visitTypeParameterDescriptor(descriptor: TypeParameterDescriptor, builder: D?): R

    fun visitValueParameterDescriptor(
        descriptor: ValueParameterDescriptor,
        builder: D?
    ): R

    fun visitPropertyDescriptor(descriptor: PropertyDescriptor, builder: D?): R
    fun visitPropertyGetterDescriptor(
        descriptor:  PropertyGetterDescriptor ,
        builder: D?
    ): R
    fun visitPropertySetterDescriptor(
        descriptor:  PropertySetterDescriptor ,
        builder: D?
    ): R

    fun visitModuleDeclaration(descriptor: ModuleDescriptor, builder: D?): R
    fun visitTypeAliasDescriptor(descriptor: TypeAliasDescriptor, builder: D?): R
    fun visitConstructorDescriptor(
        constructorDescriptor: ConstructorDescriptor,
        builder: D?
    ): R

    fun visitClassDescriptor(descriptor: ClassDescriptor, builder: D?): R
    fun visitVariableDescriptorBase(descriptor: VariableDescriptor, builder: D?): R
    fun visitVariableDescriptor(descriptor: VariableDescriptor, builder: D?): R

    //    fun visitVariableDescriptor(descriptor: VariableDescriptor, builder: D?): R
    fun visitPackageFragmentDescriptor(
        descriptor: PackageFragmentDescriptor,
        builder: D?
    ): R

    fun visitFunctionDescriptor(descriptor: FunctionDescriptor, builder: D?): R
    fun visitReceiverParameterDescriptor(
        descriptor: ReceiverParameterDescriptor,
        builder: D?
    ): R
    
    /**
     * 访问枚举描述符
     * 
     * @param descriptor 枚举描述符
     * @param builder 构建器
     * @return 访问结果
     */
    fun visitEnumDescriptor(descriptor: EnumDescriptor, builder: D?): R
    
    /**
     * 访问枚举构造函数描述符
     * 
     * @param descriptor 枚举构造函数描述符
     * @param builder 构建器
     * @return 访问结果
     */
    fun visitEnumConstructorDescriptor(descriptor: EnumConstructorDescriptor, builder: D?): R

}
