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

package cn.cangnova.cangjie.descriptors

import cn.cangnova.cangjie.types.TypeSubstitutor

/**
 * ClassConstructorDescriptor接口继承自ConstructorDescriptor和ConstructorSymbolMarker，
 * 主要用于描述类的构造器相关的信息。
 */
interface ClassConstructorDescriptor : ConstructorDescriptor {

    // 获取包含此构造器声明的类描述符。
    override val containingDeclaration: ClassDescriptor

    // 获取原始的类构造器描述符，用于处理派生描述符的情况。
    override val original: ClassConstructorDescriptor

    /**
     * 使用给定的类型替换器来替换此构造器描述符中的类型。
     * @param substitutor 类型替换器，用于执行类型替换。
     * @return 替换后的类构造器描述符，如果替换不可行则可能返回null。
     */
    override fun substitute(substitutor: TypeSubstitutor): ClassConstructorDescriptor

    /**
     * 复制当前构造器描述符，但修改其所有者、模态性、可见性、种类和是否复制覆盖标志。
     * @param newOwner 新的声明所有者。
     * @param modality 新的模态性。
     * @param visibility 新的可见性。
     * @param kind 新的成员描述符种类。
     * @param copyOverrides 是否复制覆盖标志。
     * @return 复制并修改后的类构造器描述符。
     */
    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): CallableMemberDescriptor
}
