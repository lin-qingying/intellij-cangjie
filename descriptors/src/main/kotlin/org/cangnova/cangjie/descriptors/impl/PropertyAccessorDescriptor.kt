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

package org.cangnova.cangjie.descriptors.impl

import org.cangnova.cangjie.descriptors.*

/**
 * 属性访问器描述符接口（例如属性的 getter 或 setter）。
 *
 * 此描述符表示与属性关联的访问器函数，继承自 FunctionDescriptor。
 * 它包含对对应属性的引用、覆盖关系信息，并支持通过 copy() 创建新的描述符实例。
 */
interface PropertyAccessorDescriptor : FunctionDescriptor {

    /**
     * 原始描述符：指向未被替换或复制的基准访问器描述符。
     * 在实现覆盖/复制链追踪时使用。
     */
    override val original: PropertyAccessorDescriptor

    /**
     * 被此访问器覆盖的访问器描述符集合。
     * 用于表示继承体系中的覆盖关系。
     */
    override val overriddenDescriptors: Collection<PropertyAccessorDescriptor>

    /**
     * 与此访问器对应的属性描述符（PropertyDescriptor）。
     * getter/setter 应引用其对应的属性以访问属性的元信息。
     */
    val correspondingProperty: PropertyDescriptor

    /**
     * 复制当前访问器描述符以创建一个新的实例（通常用于将描述符移动到新的所有者或改变可见性等场景）。
     *
     * @param newOwner 复制后描述符的新所有者（DeclarationDescriptor）
     * @param modality 复制后描述符的修饰符（如 final、open 等）
     * @param visibility 复制后描述符的可见性（public、private 等）
     * @param kind 描述符的种类（例如 DECLARATION、FAKE_OVERRIDE 等）
     * @param copyOverrides 是否同时复制覆盖关系（overriddenDescriptors）
     * @return 新的 PropertyAccessorDescriptor 实例
     */
    override fun copy(
        newOwner: DeclarationDescriptor,
        modality: Modality,
        visibility: DescriptorVisibility,
        kind: CallableMemberDescriptor.Kind,
        copyOverrides: Boolean
    ): PropertyAccessorDescriptor
}
