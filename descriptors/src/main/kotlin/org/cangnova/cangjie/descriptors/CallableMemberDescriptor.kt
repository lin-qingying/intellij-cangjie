/*
 * Copyright 2026 LinQingYing. and contributors.
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

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor
import org.cangnova.cangjie.types.TypeSubstitution
import org.cangnova.cangjie.types.TypeSubstitutor

/**
 * 表示类成员中的可调用实体（如函数、属性访问器等）的描述符。
 * 该接口结合了可调用描述符和成员描述符的语义，表示声明所属以及覆盖关系等成员级信息。
 */
interface CallableMemberDescriptor : CallableDescriptor, MemberDescriptor {
    /**
     * 可调用成员的种类（声明、伪重写、委托或合成）。
     */
    val kind: Kind


    /**
     * 指向此描述符的原始未替换副本，用于追溯最初的声明。
     */
    override val original: CallableMemberDescriptor

    /**
     * 返回此成员覆盖的其他可调用成员描述符集合。
     */
    override val overriddenDescriptors: Collection<CallableMemberDescriptor>

    /**
     * 设置此成员覆盖的描述符集合。
     * @param overriddenDescriptors 被覆盖的描述符集合
     */
    fun setOverriddenDescriptors(overriddenDescriptors: Collection<CallableMemberDescriptor>)

    /**
     * 创建并返回具有指定属性（所有者、模态性、可见性等）的描述符副本。
     *
     * @param newOwner 新的拥有者描述符
     * @param modality 新的调度性
     * @param visibility 新的可见性
     * @param kind 新的成员种类
     * @param copyOverrides 是否复制覆盖关系
     */
    fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: Kind,
        copyOverrides: Boolean
    ): CallableMemberDescriptor

    /**
     * 创建用于构建描述符副本的构建器。
     * @return 返回 CopyBuilder 实例以按需设置副本属性并构建新描述符
     */
    fun newCopyBuilder(): CopyBuilder<out CallableMemberDescriptor>

    /**
     * 可调用成员的种类枚举。
     */
    enum class Kind {
        /** 普通声明（直接在源或类中声明的成员） */
        DECLARATION,
        /** 伪重写：在语义上存在覆盖关系但非实际声明的成员 */
        FAKE_OVERRIDE,
        /** 委托生成的成员 */
        DELEGATION,
        /** 编译器或工具合成的成员 */
        SYNTHESIZED;

        /**
         * 判断此 Kind 是否表示真实声明（非伪重写）。
         */
        val isReal: Boolean
            get() = this != Kind.FAKE_OVERRIDE
    }

    // TODO: 将与 userdata 相关的成员提升到 DeclarationDescriptor，并使用更高效的实现（例如 THashMap）

    /**
     * 根据键获取此描述符关联的用户自定义数据（若有）。
     *
     * @param key 用户数据键
     * @return 与键关联的用户数据实例，若不存在则返回 null
     */
    fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V?

    /**
     * 复制构建器接口，用于按需设置新描述符的各项属性并构建。
     */
    interface CopyBuilder<D : CallableMemberDescriptor> {
        /** 设置新描述符的拥有者 */
        fun setOwner(owner: DeclarationDescriptor): CopyBuilder<D>

        /** 设置新描述符的模态性 */
        fun setModality(modality: Modality): CopyBuilder<D>

        /** 设置新描述符的可见性 */
        fun setVisibility(visibility: DescriptorVisibility): CopyBuilder<D>

        /** 设置新描述符的种类 */
        fun setKind(kind: Kind): CopyBuilder<D>

        /** 设置新描述符的类型参数列表 */
        fun setTypeParameters(parameters: List<TypeParameterDescriptor>): CopyBuilder<D>

        /** 设置新描述符的派发接收者参数 */
        fun setDispatchReceiverParameter(dispatchReceiverParameter: ReceiverParameterDescriptor?): CopyBuilder<D>

        /** 设置新描述符的类型替换规则 */
        fun setSubstitution(substitution: ComposableTypeSubstitutor): CopyBuilder<D>

        /** 设置是否复制覆盖关系 */
        fun setCopyOverrides(copyOverrides: Boolean): CopyBuilder<D>

        /** 设置新描述符的名称 */
        fun setName(name: Name): CopyBuilder<D>

        /** 设置新描述符的原始描述符引用 */
        fun setOriginal(original: CallableMemberDescriptor?): CopyBuilder<D>

        /** 指示在复制时保留源元素信息 */
        fun setPreserveSourceElement(): CopyBuilder<D>

        /** 设置新描述符的返回类型 */
        fun setReturnType(type: CangJieType): CopyBuilder<D>

        /** 构建并返回新描述符实例，若构建失败则返回 null */
        fun build(): D?
    }
}
