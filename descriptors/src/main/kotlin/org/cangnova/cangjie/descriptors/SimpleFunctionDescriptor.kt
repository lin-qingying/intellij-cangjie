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


/**
 * 简单函数描述符接口，继承自`FunctionDescriptor`，用于描述普通函数（非扩展、非操作符等）的元信息。
 */
interface SimpleFunctionDescriptor : FunctionDescriptor {
    /**
     * 创建当前简单函数描述符的副本，并指定新的所有者、模态、可见性、种类和是否复制覆盖关系。
     *
     * @param newOwner 新的所有者描述符
     * @param modality 新的模态
     * @param visibility 新的可见性
     * @param kind 新的种类
     * @param copyOverrides 是否复制覆盖关系
     * @return 新的简单函数描述符副本
     */
    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): SimpleFunctionDescriptor

    /**
     * 获取原始简单函数描述符，通常是当前描述符或其覆盖的版本。
     *
     * @return 原始简单函数描述符
     */
    override val original: SimpleFunctionDescriptor

    /**
     * 创建一个新的简单函数描述符副本构建器，用于生成简单函数描述符的副本。
     *
     * @return 副本构建器
     */
    override fun newCopyBuilder(): FunctionDescriptor.CopyBuilder<out SimpleFunctionDescriptor>
}
