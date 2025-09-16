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

import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue
import org.cangnova.cangjie.types.TypeSubstitutor


/**
 * 接收器参数描述符接口，继承自`ParameterDescriptor`，用于描述扩展函数或属性的接收器参数。
 */
interface ReceiverParameterDescriptor : ParameterDescriptor {

    /**
     * 获取接收器参数的值，表示该接收器的实际值或引用。
     *
     * @return 接收器参数的值
     */
    val value: ReceiverValue

    /**
     * 使用类型替换器替换当前接收器参数的类型，返回替换后的接收器参数描述符（可能为`null`）。
     *
     * @param substitutor 类型替换器
     * @return 替换后的接收器参数描述符，可能为`null`
     */
    override fun substitute(substitutor: TypeSubstitutor): ReceiverParameterDescriptor?

    /**
     * 创建当前接收器参数描述符的副本，并指定新的所有者。
     *
     * @param newOwner 新的所有者描述符
     * @return 新的接收器参数描述符副本
     */
    fun copy(newOwner: DeclarationDescriptor): ReceiverParameterDescriptor
}
