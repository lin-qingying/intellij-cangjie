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

import org.cangnova.cangjie.types.TypeSubstitutor

/**
 * 表示类构造函数的描述符接口，扩展自 ConstructorDescriptor。
 * 用于提供类构造函数的元信息（包含所属类、原始构造函数、类型替换与复制功能）。
 */
interface ClassConstructorDescriptor : ConstructorDescriptor {

    /**
     * 获取包含此构造函数声明的类描述符。
     */
    override val containingDeclaration: ClassDescriptor

    /**
     * 指向原始（未替换或未变换）版本的构造函数描述符。
     */
    override val original: ClassConstructorDescriptor

    /**
     * 使用给定的类型替换器对构造函数所涉及的类型执行替换，返回替换后的构造函数描述符（可能为 null）。
     *
     * @param substitutor 类型替换器
     * @return 替换后的 ClassConstructorDescriptor（若替换不可行可返回 null）
     */
    override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor?

    /**
     * 复制当前构造函数描述符并可修改其属性（拥有者、模态性、可见性、成员种类以及是否复制覆盖关系）。
     *
     * @param newOwner 新的声明所有者
     * @param modality 新的模态性
     * @param visibility 新的可见性
     * @param kind 新的成员种类
     * @param copyOverrides 是否复制覆盖关系
     * @return 复制后的 CallableMemberDescriptor
     */
    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): CallableMemberDescriptor
}
