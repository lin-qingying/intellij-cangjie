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

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeSubstitution

/**
 * 表示一个可调用成员的描述符，例如方法或属性，可以被调用。
 * 它扩展了 CallableDescriptor 和 MemberDescriptor，结合了可调用性和类成员的特性。
 */
interface CallableMemberDescriptor : CallableDescriptor, MemberDescriptor {
    /**
     * 判断这是一个真实的方法还是方法投影。
     *
     * @return 返回此可调用成员的类型，指示其是真实方法还是投影。
     */

    val kind: Kind

    /**
     * 获取原始的可调用成员描述符，用于追溯最初声明的成员。
     *
     * @return 原始的可调用成员描述符。
     */
    override val original: CallableMemberDescriptor

    /**
     * 获取被覆盖成员的描述符集合。
     *
     * @return 被覆盖成员的描述符集合。
     */
    /**
     * 设置被覆盖成员的描述符集合。
     *
     * @param overriddenDescriptors 被覆盖成员的描述符集合。
     */

    override val overriddenDescriptors: Collection<CallableMemberDescriptor>
    fun setOverriddenDescriptors(overriddenDescriptors: Collection<CallableMemberDescriptor>)


    /**
     * 创建一个新的可调用成员描述符副本，并设置新的属性。
     *
     * @param newOwner      新的所有者。
     * @param modality      新的模态性。
     * @param visibility    新的可见性。
     * @param kind          新的类型。
     * @param copyOverrides 是否复制被覆盖的描述符。
     * @return 具有指定属性的新 CallableMemberDescriptor 实例。
     */
    fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: Kind,
        copyOverrides: Boolean
    ): CallableMemberDescriptor

    /**
     * 创建一个新的可调用成员描述符副本生成器。
     *
     * @return 用于构建此描述符副本的新 CopyBuilder 实例。
     */
    fun newCopyBuilder(): CopyBuilder<out CallableMemberDescriptor>

    /**
     * 定义可调用成员的类型。
     */
    enum class Kind {
        DECLARATION,  // 声明
        FAKE_OVERRIDE,  // 伪重写，指在某些情况下并不真正重写父类的方法
        DELEGATION,  // 委托
        SYNTHESIZED; // 合成，可能是指编译器或工具自动生成的代码或结构。

        val isReal: Boolean
            /**
             * 检查此类型是否表示一个真实的可调用成员。
             *
             * @return 如果此类型不是 FAKE_OVERRIDE，则返回 true，表示它是一个真实的可调用成员；否则返回 false。
             */
            get() = this != Kind.FAKE_OVERRIDE
    }
    // TODO: 将与 userdata 相关的成员提升到 DeclarationDescriptor，并使用更高效的实现（例如 THashMap）
    /**
     * 获取与此描述符关联的用户自定义数据。
     *
     * @param key 用于检索用户数据的键。
     * @param <V> 用户数据的类型。
     * @return 与给定键关联的用户数据，如果不存在则返回 null。
    </V> */
    fun <V> getUserData(key: CallableDescriptor.UserDataKey<V>): V?

    /**
     * 定义用于复制可调用成员描述符的生成器接口。
     */
    interface CopyBuilder<D : CallableMemberDescriptor> {
        /**
         * 设置新描述符的所有者。
         *
         * @param owner 新描述符的所有者。
         * @return 当前生成器实例。
         */
        fun setOwner(owner: DeclarationDescriptor): CopyBuilder<D>

        /**
         * 设置新描述符的模态性。
         *
         * @param modality 新描述符的模态性。
         * @return 当前生成器实例。
         */
        fun setModality(modality: Modality): CopyBuilder<D>

        /**
         * 设置新描述符的可见性。
         *
         * @param visibility 新描述符的可见性。
         * @return 当前生成器实例。
         */
        fun setVisibility(visibility: DescriptorVisibility): CopyBuilder<D>

        /**
         * 设置新描述符的类型。
         *
         * @param kind 新描述符的类型。
         * @return 当前生成器实例。
         */
        fun setKind(kind: Kind): CopyBuilder<D>

        /**
         * 设置新描述符的类型参数。
         *
         * @param parameters 新描述符的类型参数列表。
         * @return 当前生成器实例。
         */
        fun setTypeParameters(parameters: List<TypeParameterDescriptor>): CopyBuilder<D>

        /**
         * 设置新描述符的调度接收参数。
         *
         * @param dispatchReceiverParameter 新描述符的调度接收参数。
         * @return 当前生成器实例。
         */
        fun setDispatchReceiverParameter(dispatchReceiverParameter: ReceiverParameterDescriptor?): CopyBuilder<D>

        /**
         * 设置新描述符的替换规则。
         *
         * @param substitution 新描述符的替换规则。
         * @return 当前生成器实例。
         */
        fun setSubstitution(substitution: TypeSubstitution): CopyBuilder<D>

        /**
         * 设置是否复制被覆盖的描述符。
         *
         * @param copyOverrides 是否复制被覆盖的描述符。
         * @return 当前生成器实例。
         */
        fun setCopyOverrides(copyOverrides: Boolean): CopyBuilder<D>

        /**
         * 设置新描述符的名称。
         *
         * @param name 新描述符的名称。
         * @return 当前生成器实例。
         */
        fun setName(name: Name): CopyBuilder<D>

        /**
         * 设置新描述符的原始描述符。
         *
         * @param original 新描述符的原始描述符。
         * @return 当前生成器实例。
         */
        fun setOriginal(original: CallableMemberDescriptor?): CopyBuilder<D>

        /**
         * 设置是否保留源元素。
         *
         * @return 当前生成器实例。
         */
        fun setPreserveSourceElement(): CopyBuilder<D>

        /**
         * 设置新描述符的返回类型。
         *
         * @param type 新描述符的返回类型。
         * @return 当前生成器实例。
         */
        fun setReturnType(type: CangJieType): CopyBuilder<D>

        /**
         * 构建并返回新描述符实例。
         *
         * @return 新的 CallableMemberDescriptor 实例，如果构建失败则返回 null。
         */
        fun build(): D?
    }
}
