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

import org.cangnova.cangjie.descriptors.impl.PropertyAccessorDescriptor


/**
 * 带有访问器的属性描述符接口，继承自 `VariableDescriptor`，用于描述具有 getter/setter 的属性的元信息。
 */
interface PropertyDescriptorWithAccessors : VariableDescriptor {
    /**
     * 获取该属性的 getter 描述符（如果存在）。
     *
     * @return getter 描述符，可能为 `null`
     */
    val getter: PropertyAccessorDescriptor?

    /**
     * 获取该属性的 setter 描述符（如果存在）。
     *
     * @return setter 描述符，可能为 `null`
     */
    val setter: PropertyAccessorDescriptor?

    /**
     * 注意：由于属性是否为委托属性不属于其 API 或 ABI，请谨慎依赖此标志。
     * 在编译器前端使用此标志可能会显著影响编译结果，因此不建议在可能影响编译结果的地方使用。
     *
     * 此标志主要用于反射场景，因此会被序列化到元数据中。
     *
     * 注意：当前已注释掉此属性。
     */
//    val isDelegated: Boolean
}

//val PropertyDescriptorWithAccessors.accessors: List<VariableAccessorDescriptor>
//    get() = listOfNotNull(getter, setter)
