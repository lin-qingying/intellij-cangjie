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

package org.cangnova.cangjie.descriptors.impl


import org.cangnova.cangjie.descriptors.ClassDescriptor
import org.cangnova.cangjie.descriptors.ClassifierDescriptor
import org.cangnova.cangjie.descriptors.ClassifierDescriptorWithTypeParameters
import org.cangnova.cangjie.descriptors.EnumDescriptor
import org.cangnova.cangjie.descriptors.HasScopeDescriptor
import org.cangnova.cangjie.descriptors.impl.ModuleAwareDescriptorBase.Companion.getRefinedMemberScopeIfPossible
import org.cangnova.cangjie.descriptors.impl.ModuleAwareDescriptorBase.Companion.getRefinedUnsubstitutedMemberScopeIfPossible
import org.cangnova.cangjie.resolve.scopes.MemberScope
import org.cangnova.cangjie.types.TypeProjection
import org.cangnova.cangjie.types.TypeSubstitution
import org.cangnova.cangjie.types.checker.CangJieTypeRefiner


interface ModuleAwareDescriptor {

    /**
     * 获取未经类型替换的成员作用域
     *
     * 该方法返回原始的、未进行任何类型替换的成员作用域，但仍然会
     * 通过类型细化器进行必要的调整。这通常用于获取类的基础成员信息。
     *
     * @param cangjieTypeRefiner 类型细化器，用于根据编译上下文调整类型行为
     * @return MemberScope 经过细化但未经类型替换的成员作用域
     */
    fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope

}
/**
 * 模块感知描述符的通用基类
 *
 * 提供了模块感知描述符的通用实现，消除了ModuleAwareClassDescriptor和ModuleAwareEnumDescriptor之间的代码重复。
 * 该基类定义了类型细化相关的核心方法和扩展函数。
 *
 * @param T 具体的分类器描述符类型（ClassDescriptor或EnumDescriptor）
 */
abstract class ModuleAwareDescriptorBase<T : ClassifierDescriptor> :
    HasScopeDescriptor, ClassifierDescriptorWithTypeParameters, ModuleAwareDescriptor {

    /**
     * 获取带有类型替换的成员作用域
     *
     * 该方法允许在类型替换的基础上，进一步通过类型细化器来调整成员作用域。
     * 这对于处理泛型类实例化时的类型解析非常重要。
     *
     * @param typeSubstitution 类型替换映射，用于将泛型参数替换为具体类型
     * @param cangjieTypeRefiner 类型细化器，用于根据编译上下文调整类型行为
     * @return MemberScope 经过类型替换和细化的成员作用域
     */
    abstract fun getMemberScope(typeSubstitution: TypeSubstitution, cangjieTypeRefiner: CangJieTypeRefiner): MemberScope

    /**
     * 获取带有类型参数的成员作用域
     *
     * 该方法通过类型投影列表来指定具体的类型参数，然后结合类型细化器
     * 生成对应的成员作用域。
     *
     * @param typeArguments 类型参数列表，每个元素表示一个类型投影
     * @param cangjieTypeRefiner 类型细化器，用于根据编译上下文调整类型行为
     * @return MemberScope 经过类型参数化和细化的成员作用域
     */
    abstract fun getMemberScope(
        typeArguments: List<TypeProjection>,
        cangjieTypeRefiner: CangJieTypeRefiner
    ): MemberScope

    /**
     * 获取未经类型替换的成员作用域
     *
     * 该方法返回原始的、未进行任何类型替换的成员作用域，但仍然会
     * 通过类型细化器进行必要的调整。这通常用于获取类的基础成员信息。
     *
     * @param cangjieTypeRefiner 类型细化器，用于根据编译上下文调整类型行为
     * @return MemberScope 经过细化但未经类型替换的成员作用域
     */
    abstract override fun getUnsubstitutedMemberScope(cangjieTypeRefiner: CangJieTypeRefiner): MemberScope

    companion object {
        /**
         * 尝试获取细化的未替换成员作用域
         *
         * 这是一个内部扩展函数，用于为任意HasScopeDescriptor提供类型细化能力。
         * 如果目标描述符是ModuleAwareDescriptorBase的实例，则使用其细化方法；
         * 否则回退到标准的未替换成员作用域。
         *
         * @param cangjieTypeRefiner 类型细化器
         * @return MemberScope 细化后的成员作用域或标准成员作用域
         */
        internal fun HasScopeDescriptor.getRefinedUnsubstitutedMemberScopeIfPossible(
            cangjieTypeRefiner: CangJieTypeRefiner
        ): MemberScope =
            (this as? ModuleAwareDescriptorBase<*>)?.getUnsubstitutedMemberScope(cangjieTypeRefiner)
                ?: this.unsubstitutedMemberScope

        /**
         * 尝试获取细化的替换成员作用域
         *
         * 这是一个内部扩展函数，用于为任意HasScopeDescriptor提供类型替换和细化能力。
         * 如果目标描述符是ModuleAwareDescriptorBase的实例，则使用其细化方法；
         * 否则回退到标准的类型替换成员作用域。
         *
         * @param typeSubstitution 类型替换映射
         * @param cangjieTypeRefiner 类型细化器
         * @return MemberScope 细化后的成员作用域或标准替换成员作用域
         */
        internal fun HasScopeDescriptor.getRefinedMemberScopeIfPossible(
            typeSubstitution: TypeSubstitution,
            cangjieTypeRefiner: CangJieTypeRefiner
        ): MemberScope =
            (this as? ModuleAwareDescriptorBase<*>)?.getMemberScope(typeSubstitution, cangjieTypeRefiner)
                ?: this.getMemberScope(typeSubstitution)
    }
}

/**
 * 模块感知的类描述符 (Module-Aware Class Descriptor)
 *
 * 这是一个抽象基类，为类描述符提供了模块感知的成员作用域管理能力。
 * 与普通的ClassDescriptor相比，该类能够根据不同的类型细化器(CangJieTypeRefiner)
 * 提供不同版本的成员作用域，这对于支持模块化编译和类型细化非常重要。
 *
 * 主要特性：
 * 1. **类型细化支持**: 能够根据类型细化器调整成员作用域的行为
 * 2. **类型替换感知**: 支持泛型类型参数的替换操作
 * 3. **模块化编译**: 在多模块项目中提供正确的类型解析
 * 4. **向后兼容**: 通过扩展函数为现有ClassDescriptor提供细化能力
 *
 * 使用场景：
 * - 处理跨模块的类型引用时
 * - 进行泛型类型实例化时
 * - 需要根据编译上下文调整类型行为时
 */
abstract class ModuleAwareClassDescriptor : ModuleAwareDescriptorBase<ClassDescriptor>(), ClassDescriptor
abstract class ModuleAwareEnumDescriptor : ModuleAwareDescriptorBase<EnumDescriptor>(), EnumDescriptor
/**
 * ClassDescriptor的扩展函数：获取细化的未替换成员作用域
 *
 * 这个公共扩展函数为所有ClassDescriptor实例提供了获取细化未替换成员作用域的能力。
 * 它内部调用companion object中的同名函数。
 *
 * @param cangjieTypeRefiner 类型细化器
 * @return MemberScope 细化后的成员作用域
 */
fun HasScopeDescriptor.getRefinedUnsubstitutedMemberScopeIfPossible(
    cangjieTypeRefiner: CangJieTypeRefiner
): MemberScope = getRefinedUnsubstitutedMemberScopeIfPossible(cangjieTypeRefiner)

/**
 * ClassDescriptor的扩展函数：获取细化的替换成员作用域
 *
 * 这个公共扩展函数为所有ClassDescriptor实例提供了获取细化替换成员作用域的能力。
 * 它内部调用companion object中的同名函数。
 *
 * @param typeSubstitution 类型替换映射
 * @param cangjieTypeRefiner 类型细化器
 * @return MemberScope 细化后的成员作用域
 */
fun HasScopeDescriptor.getRefinedMemberScopeIfPossible(
    typeSubstitution: TypeSubstitution,
    cangjieTypeRefiner: CangJieTypeRefiner
): MemberScope = getRefinedMemberScopeIfPossible(typeSubstitution, cangjieTypeRefiner)
