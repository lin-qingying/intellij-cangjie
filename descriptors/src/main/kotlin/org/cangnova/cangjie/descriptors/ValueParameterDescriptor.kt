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

import org.cangnova.cangjie.name.Name
import org.cangnova.cangjie.types.CangJieType

/**
 * 值参数描述符接口，继承自`VariableDescriptor`和`ParameterDescriptor`，用于描述函数或构造器的参数。
 * 该接口提供了参数的元信息，包括是否为命名参数、参数索引、可变参数元素类型等。
 */
interface ValueParameterDescriptor : VariableDescriptor, ParameterDescriptor {
    /**
     * 原始参数描述符，返回当前参数描述符的原始版本（通常是其自身或其覆盖的版本）。
     *
     * @return 原始参数描述符
     */
    override val original: ValueParameterDescriptor


    /**
     * 包含该参数的声明，通常是其所属的函数或构造器描述符。
     *
     * @return 包含该参数的声明
     */
    override val containingDeclaration: CallableDescriptor

    /**
     * 可变参数元素类型，如果该参数是可变参数（vararg），则返回其元素类型；否则返回`null`。
     *
     * @return 可变参数元素类型，如果存在则为`CangJieType`，否则为`null`
     */
    val varargElementType: CangJieType? get() = null

    /**
     * 是否为命名参数，如果该参数在声明时指定了名称，则返回`true`；否则返回`false`。
     *
     * @return `true`表示是命名参数，`false`表示不是
     */
    val isNamed: Boolean

    /**
     * 返回该参数在其所属函数的参数列表中的0基索引。
     *
     * @return 参数索引
     */
    val index: Int

    /**
     * 返回当前参数描述符覆盖的所有参数描述符集合。
     * 参数p1覆盖p2当且仅当满足以下条件：
     * 1. 它们的所属函数声明（f1和f2）存在覆盖关系（f1覆盖f2）；
     * 2. p1和p2在其所属函数的参数列表中具有相同的索引。
     *
     * @return 覆盖的参数描述符集合
     */
    override val overriddenDescriptors: Collection<ValueParameterDescriptor>

    /**
     * 创建当前参数描述符的副本，并指定新的所有者、名称和索引。
     *
     * @param newOwner 新的函数或构造器描述符
     * @param newName 新的参数名称
     * @param newIndex 新的参数索引
     * @return 新的参数描述符副本
     */
    fun copy(newOwner: CallableDescriptor, newName: Name, newIndex: Int): ValueParameterDescriptor

}
