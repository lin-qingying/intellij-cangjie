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

package org.cangnova.cangjie.descriptors.extend

import org.cangnova.cangjie.descriptors.DeclarationDescriptor
import org.cangnova.cangjie.descriptors.ReceiverParameterDescriptor
import org.cangnova.cangjie.descriptors.annotations.Annotations
import org.cangnova.cangjie.descriptors.impl.AbstractReceiverParameterDescriptor
import org.cangnova.cangjie.resolve.scopes.receivers.ImplicitExtendReceiver
import org.cangnova.cangjie.resolve.scopes.receivers.ReceiverValue

/**
 * 扩展（extend）的接收器参数描述符
 *
 * 用于表示扩展块内成员函数的 this 接收器，类型为被扩展的类型（extendType）
 *
 * @param descriptor 扩展描述符
 */
class LazyExtendReceiverParameterDescriptor(private val descriptor: ExtendDescriptor) :
    AbstractReceiverParameterDescriptor(
        Annotations.EMPTY
    ) {
    private val receiverValue = ImplicitExtendReceiver(descriptor, null)

    override val value: ReceiverValue
        get() = receiverValue

    override val containingDeclaration: DeclarationDescriptor
        get() = descriptor

    override fun copy(newOwner: DeclarationDescriptor): ReceiverParameterDescriptor {
        throw UnsupportedOperationException("LazyExtendReceiverParameterDescriptor cannot be copied")
    }

    override fun toString(): String {
        return "extend ${descriptor.extendType}::this"
    }
}
