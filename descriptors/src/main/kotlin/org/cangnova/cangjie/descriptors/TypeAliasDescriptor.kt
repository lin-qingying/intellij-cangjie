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

import org.cangnova.cangjie.types.SimpleType


/**
 * 类型别名描述符接口，继承自`ClassifierDescriptorWithTypeParameters`，用于描述类型别名的元信息，如底层类型、展开类型等。
 */
interface TypeAliasDescriptor : ClassifierDescriptorWithTypeParameters {
    /**
     * 获取类型别名的底层类型（即别名定义的右侧类型），该类型可能包含其他类型别名。
     *
     * @return 底层类型
     */
    val underlyingType: SimpleType

    /**
     * 获取完全展开的类型（未替换类型参数），该类型不包含任何类型别名。
     *
     * @return 完全展开的类型
     */
    val expandedType: SimpleType

    /**
     * 获取与该类型别名关联的类描述符（如果存在）。
     *
     * @return 关联的类描述符，可能为`null`
     */
    val classDescriptor: ClassDescriptor?


    /**
     * 获取原始类型别名描述符，通常是当前描述符或其覆盖的版本。
     *
     * @return 原始类型别名描述符
     */
    override val original: TypeAliasDescriptor


    /**
     * 获取该类型别名关联的所有构造器描述符集合。
     *
     * @return 构造器描述符集合
     */
    val constructors: Collection<TypeAliasConstructorDescriptor>


}
