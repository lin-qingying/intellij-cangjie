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

package cn.cangnova.cangjie.descriptors.impl

import cn.cangnova.cangjie.descriptors.PropertyDescriptor
import cn.cangnova.cangjie.descriptors.SourceElement
import cn.cangnova.cangjie.descriptors.annotations.Annotations

class SyntheticFieldDescriptor(
    val propertyDescriptor: PropertyDescriptor,
    accessorDescriptor: PropertyAccessorDescriptor,
    sourceElement: SourceElement
) : LocalVariableDescriptor(
    accessorDescriptor, Annotations.EMPTY, NAME,
    propertyDescriptor.type, propertyDescriptor.isVar,
    sourceElement
)  {
    constructor(
        accessorDescriptor: PropertyAccessorDescriptor,
        sourceElement: SourceElement
    ) : this(accessorDescriptor.correspondingProperty, accessorDescriptor, sourceElement)

    companion object {
        @JvmField
        val NAME = Name.identifier("field")
    }
}
