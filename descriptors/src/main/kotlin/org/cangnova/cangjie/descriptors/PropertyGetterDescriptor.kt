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

import org.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptor


/**
 * 属性 Getter 描述符接口，继承自 `PropertyAccessorDescriptor`，用于描述属性的 `getter` 方法的元信息。
 */
interface PropertyGetterDescriptor : PropertyAccessorDescriptor {
    /**
     * 获取原始的 Getter 描述符（通常是当前描述符或其覆盖的版本）。
     *
     * @return 原始的 Getter 描述符
     */
    override val original: PropertyGetterDescriptor
    /**
     * 获取当前 Getter 描述符覆盖的所有访问器描述符集合。
     *
     * @return 覆盖的访问器描述符集合
     */
    override val overriddenDescriptors: Collection<PropertyAccessorDescriptor>

}

/**
 * 属性 Setter 描述符接口，继承自 `PropertyAccessorDescriptor`，用于描述属性的 `setter` 方法的元信息。
 */
interface PropertySetterDescriptor : PropertyAccessorDescriptor {
    /**
     * 获取原始的 Setter 描述符（通常是当前描述符或其覆盖的版本）。
     *
     * @return 原始的 Setter 描述符
     */
    override val original: PropertySetterDescriptor

    /**
     * 获取当前 Setter 描述符覆盖的所有 Setter 描述符集合。
     *
     * @return 覆盖的 Setter 描述符集合
     */
    override val overriddenDescriptors: Collection<PropertySetterDescriptor>
}
