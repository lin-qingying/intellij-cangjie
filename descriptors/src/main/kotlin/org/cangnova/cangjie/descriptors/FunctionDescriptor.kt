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

import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.name.Name

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeSubstitution
import org.cangnova.cangjie.types.TypeSubstitutor

/**
 * 函数描述符接口，继承自`CallableMemberDescriptor`和`EnumMember`，用于描述函数的元信息，如覆盖关系、参数、返回类型等。
 */
interface FunctionDescriptor : CallableMemberDescriptor, EnumMember {



    /**
     * 获取包含该函数的声明描述符（通常是类或对象）。
     *
     * @return 包含该函数的声明描述符
     */
    override val containingDeclaration: DeclarationDescriptor


    /**
     * 获取原始函数描述符，通常是当前描述符或其覆盖的版本。
     *
     * @return 原始函数描述符
     */
    override val original: FunctionDescriptor

    /**
     * 使用类型替换器替换当前函数的类型，返回替换后的函数描述符（可能为`null`）。
     *
     * @param substitutor 类型替换器
     * @return 替换后的函数描述符，可能为`null`
     */
    override fun substitute(substitutor: TypeSubstitutor): FunctionDescriptor?

    /**
     * 获取当前函数描述符覆盖的所有函数描述符集合。
     * 注意：如果当前描述符是替换后的版本（substituted），调用此方法可能会触发不必要的惰性计算。
     * 如果可以使用`original.overriddenDescriptors`替代，建议优先使用。
     *
     * @return 覆盖的函数描述符集合
     */
    override val overriddenDescriptors: Collection<FunctionDescriptor>
    /**
     * 获取表示初始签名的函数描述符（例如，在重命名后的函数中，返回重命名前的描述符）。
     *
     * @return 初始签名描述符，可能为`null`
     */
    val initialSignatureDescriptor: FunctionDescriptor?

    /**
     * 检查是否因签名冲突而隐藏（例如`java.nio.CharBuffer`中的某些方法）。
     *
     * @return `true`表示因签名冲突而隐藏
     */
    val isHiddenToOvercomeSignatureClash: Boolean

    /**
     * 创建当前函数描述符的副本，并指定新的所有者、模态、可见性、种类和是否复制覆盖关系。
     *
     * @param newOwner 新的所有者描述符
     * @param modality 新的模态
     * @param visibility 新的可见性
     * @param kind 新的种类
     * @param copyOverrides 是否复制覆盖关系
     * @return 新的函数描述符副本
     */
    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): CallableMemberDescriptor


    /**
     * 检查是否为操作符函数（标记为`operator`）。
     *
     * @return `true`表示是操作符函数
     */
    val isOperator: Boolean
    /**
     * 检查是否为`const`函数（默认为`false`）。
     *
     * @return `true`表示是`const`函数
     */
    val isConst: Boolean
        get() = false


    /**
     * 检查是否在除`super`调用外的所有地方隐藏（用于解决某些方法的调用冲突）。
     *
     * @return `true`表示隐藏
     */
    val isHiddenForResolutionEverywhereBesideSupercalls: Boolean
        get

    //    bool isSuspend();
    /**
     * 创建一个新的函数描述符副本构建器，用于生成函数描述符的副本。
     *
     * @return 副本构建器
     */
    override fun newCopyBuilder(): CopyBuilder<out FunctionDescriptor>

    interface CopyBuilder<D : FunctionDescriptor> : CallableMemberDescriptor.CopyBuilder<D> {
        override fun setOwner(owner: DeclarationDescriptor): CopyBuilder<D>

        override fun setModality(modality: Modality): CopyBuilder<D>

        override fun setVisibility(visibility: DescriptorVisibility): CopyBuilder<D>

        override fun setKind(kind: CallableMemberDescriptor.Kind): CopyBuilder<D>

        override fun setCopyOverrides(copyOverrides: Boolean): CopyBuilder<D>

        override fun setName(name: Name): CopyBuilder<D>

        fun setValueParameters(parameters: List<ValueParameterDescriptor>): CopyBuilder<D>

        override fun setTypeParameters(parameters: List<TypeParameterDescriptor>): CallableMemberDescriptor.CopyBuilder<D>

        override fun setReturnType(type: CangJieType): CopyBuilder<D>


        override fun setDispatchReceiverParameter(dispatchReceiverParameter: ReceiverParameterDescriptor?): CopyBuilder<D>

        override fun setOriginal(original: CallableMemberDescriptor?): CopyBuilder<D>

        fun setSignatureChange(): CopyBuilder<D>

        override fun setPreserveSourceElement(): CopyBuilder<D>

        fun setDropOriginalInContainingParts(): CopyBuilder<D>

        fun setHiddenToOvercomeSignatureClash(): CopyBuilder<D>

        fun setHiddenForResolutionEverywhereBesideSupercalls(): CopyBuilder<D>

        fun setAdditionalAnnotations(additionalAnnotations: Annotations): CopyBuilder<D>

        override fun setSubstitution(substitution: TypeSubstitution): CopyBuilder<D>

        fun <V> putUserData(userDataKey: CallableDescriptor.UserDataKey<V>, value: V): CopyBuilder<D>

        override fun build(): D?
    }
}
