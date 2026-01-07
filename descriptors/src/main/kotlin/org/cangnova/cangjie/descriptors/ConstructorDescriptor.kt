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

import org.cangnova.cangjie.types.CangJieType
import org.cangnova.cangjie.types.TypeSubstitutor

/**
 * 构造函数描述符接口
 */
interface ConstructorDescriptor : FunctionDescriptor {

    /**
     * 是否为主构造函数
     */
    val isPrimary: Boolean
    /**
     * 是否为最终构造函数
     */
    val isEnd: Boolean

    /**
     * 获取返回类型
     */
    override val returnType: CangJieType
    /**
     * 获取构造的类描述符
     */
    val constructedClass: ClassAndEnumDescriptor

    /**
     * 复制构造函数描述符
     * @param newOwner 新所有者描述符
     * @param modality 模态
     * @param visibility 可见性
     * @param kind 成员种类
     * @param copyOverrides 是否复制覆盖
     * @return 复制后的可调用成员描述符
     */
    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): CallableMemberDescriptor


    /**
     * 类型替换
     * @param substitutor 类型替换器
     * @return 替换后的构造函数描述符(可能为null)
     */
    override fun substitute(substitutor: TypeSubstitutor): ConstructorDescriptor?


    /**
     * 获取原始构造函数描述符
     */
    override val original: ConstructorDescriptor


    /**
     * 获取包含声明的分类器描述符(带类型参数)
     */
    override val containingDeclaration: ClassifierDescriptorWithTypeParameters
}
