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

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.ComposableTypeSubstitutor

/**
 * 类型别名构造器描述符接口，继承自`ConstructorDescriptor`和`DescriptorDerivedFromTypeAlias`，用于描述类型别名构造器的元信息。
 */
interface TypeAliasConstructorDescriptor : ConstructorDescriptor, DescriptorDerivedFromTypeAlias {
    /**
     * 获取底层构造器描述符，即该类型别名构造器所依赖的实际类构造器。
     *
     * @return 底层构造器描述符
     */
    val underlyingConstructorDescriptor: ClassConstructorDescriptor

    /**
     * 获取构造器的返回类型。
     *
     * @return 返回类型
     */
    override val returnType: CangJieType
    /**
     * 获取原始类型别名构造器描述符，通常是当前描述符或其覆盖的版本。
     *
     * @return 原始类型别名构造器描述符
     */
    override val original: TypeAliasConstructorDescriptor


    /**
     * 获取包含该构造器的类型别名描述符。
     *
     * @return 类型别名描述符
     */
    override val containingDeclaration: TypeAliasDescriptor


    /**
     * 使用类型替换器替换当前构造器的类型，返回替换后的构造器描述符（可能为`null`）。
     *
     * @param substitutor 类型替换器
     * @return 替换后的构造器描述符，可能为`null`
     */
    override fun substitute(substitutor: ComposableTypeSubstitutor): TypeAliasConstructorDescriptor?

    /**
     * 获取带有分发接收器的类型别名构造器描述符（如果存在）。
     *
     * @return 带有分发接收器的构造器描述符，可能为`null`
     */
    val withDispatchReceiver: TypeAliasConstructorDescriptor?

    /**
     * 创建当前构造器描述符的副本，并指定新的所有者、模态、可见性、种类和是否复制覆盖关系。
     *
     * @param newOwner 新的声明描述符
     * @param modality 新的模态
     * @param visibility 新的可见性
     * @param kind 新的种类
     * @param copyOverrides 是否复制覆盖关系
     * @return 新的构造器描述符副本
     */
    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): TypeAliasConstructorDescriptor
}
